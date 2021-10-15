package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.abstraction.ServiceBootstrap;
import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.CoreConfig;
import chrisliebaer.chrisliebot.config.JsonBotConfig;
import chrisliebaer.chrisliebot.config.scope.ScopeMapping;
import chrisliebaer.chrisliebot.util.GsonValidator;
import chrisliebaer.chrisliebot.util.SystemProperty;
import chrisliebaer.chrisliebot.util.typeadapter.CronTypeAdapter;
import chrisliebaer.chrisliebot.util.typeadapter.PatternTypeAdapter;
import chrisliebaer.chrisliebot.util.typeadapter.ZoneIdTypeAdapter;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Validation;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
public class Chrisliebot extends AbstractIdleService {
	
	private static final int EXIT_CODE_ERROR = -1;
	private static final int EXIT_CODE_PROPER_SHUTDOWN = 0;
	private static final int EXIT_CODE_RESTART = 10;
	private static final int EXIT_CODE_UPGRADE = 20;
	
	// proper shutdown of logging framework
	static {
		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}
	
	private final File cwd = SystemProperty.of("cwd", new File("."), File::new);
	private final File coreFile = SystemProperty.of("chrisliebot.core", new File("core.json"), f -> new File(cwd, f));
	private final File botFile = SystemProperty.of("chrisliebot.bot", new File("bot.json"), f -> new File(cwd, f));
	
	/**
	 * In order to maintain control over how we load and save json files, this instance is supposed to be shared by the
	 * entire application.
	 */
	@SuppressWarnings("resource") private GsonValidator gson = new GsonValidator(new GsonBuilder()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.registerTypeAdapter(Pattern.class, new PatternTypeAdapter().nullSafe())
			.registerTypeAdapter(Cron.class, new CronTypeAdapter(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)))
			.registerTypeAdapter(ZoneId.class, new ZoneIdTypeAdapter())
			.create(), Validation.buildDefaultValidatorFactory().getValidator());
	
	/**
	 * Set up before even starting up services. Provides commonly shared ressources like http clients, database
	 * connections worker pools, etc.
	 */
	@Getter private SharedResources sharedResources;
	
	/**
	 * List of currently active services.
	 */
	private Map<String, ChrislieService> services = new HashMap<>();
	
	/**
	 * Dispatcher instance for services.
	 */
	@Getter private ChrislieDispatcher dispatcher;
	
	/**
	 * Resolver that's used in dispatcher.
	 */
	private ContextResolver resolver;
	
	/**
	 * Exit code that will be returned if proper shutdown occurs.
	 */
	private volatile int exitCode = 0;
	
	@Getter private final Managment managment = new Managment();
	
	public static void main(String[] args) {
		log.debug("default charset is: {}", Charset.defaultCharset());
		
		// hook uncaugth exceptions and redirect to logger
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error("uncaugth exception in thread {}", t, e));
		
		var bot = new Chrisliebot();
		bot.addListener(new Listener() {
			@Override
			public void starting() {
				log.info("Chrisliebot is starting up");
			}
			
			@Override
			public void running() {
				log.info("Chrisliebot has entered running state");
			}
			
			@Override
			public void stopping(State from) {
				log.info("Chrisliebot is transitioning to termination (was {})", from);
			}
			
			@Override
			public void terminated(State from) {
				log.info("Chrisliebot has terminated (was {})", from);
				System.exit(bot.exitCode);
			}
			
			@Override
			public void failed(State from, Throwable failure) {
				log.error("Chrisliebot has encountered a critical error during `{}` and will now terminate the process", from, failure);
				System.exit(EXIT_CODE_ERROR);
			}
		}, command -> new Thread(command, "Watchdog").start()); // we don't know the state of any shared executors, so we have to create a new thread
		bot.startAsync();
	}
	
	@SuppressWarnings("MethodOnlyUsedFromInnerClass")
	private void shutdownWithCode(int code) {
		synchronized (managment) {
			if (exitCode != 0) {
				// exit code may only be set once
				return;
			}
			exitCode = code;
			stopAsync();
		}
	}
	
	@Override
	protected void startUp() throws Exception {
		log.info("using core config `{}` and bot config `{}`", coreFile, botFile);
		
		CoreConfig coreCfg;
		try (var fr = new FileReader(coreFile)) {
			coreCfg = gson.fromJson(fr, CoreConfig.class);
		} catch (IOException e) {
			throw new Exception("failed to load connection config", e);
		}
		
		// setup shared ressources as they might be required by some services
		sharedResources = new SharedResources(coreCfg.databasePool(), gson);
		sharedResources.startAsync().awaitRunning();
		
		// bot config requires running serivces, so services go first
		coreCfg.ensureDisjoint();
		Map<String, ServiceBootstrap> bootstraps = new HashMap<>();
		bootstraps.putAll(coreCfg.irc());
		bootstraps.putAll(coreCfg.discord());
		
		for (var entry : bootstraps.entrySet()) {
			var name = entry.getKey();
			var boostrap = entry.getValue();
			try {
				services.put(name, boostrap.service(this, name));
			} catch (Exception e) {
				throw new Exception(String.format("failed to start service `%s`", name), e);
			}
		}
		
		// separate loop so services can work in parallel
		for (var entry : services.entrySet()) {
			var name = entry.getKey();
			var service = entry.getValue();
			try {
				service.awaitReady();
			} catch (Exception e) {
				throw new Exception(String.format("service `%s` failed to get ready", name), e);
			}
		}
		
		// on the first load, we abort on errors, since we have no fallback
		loadBotConfig();
	}
	
	private void loadBotConfig() throws ChrisliebotException {
		
		// deserializing config
		JsonBotConfig botConfig;
		try (var fr = new FileReader(botFile)) {
			botConfig = gson.fromJson(fr, JsonBotConfig.class);
		} catch (IOException e) {
			throw new ChrisliebotException("failed to load bot config file", e);
		}
		
		// instancing bot config is actually a very heavy task, don't be fooled
		List<ScopeMapping> scopeMappings;
		try {
			scopeMappings = new ArrayList<>(botConfig.instance(gson));
		} catch (JsonBotConfig.ConfigInitializeException e) {
			throw new ChrisliebotException("unable to instance bot config", e);
		}
		
		// listeners are created, but not yet initialized or started since this requires a context resolver and a chrisliebot instance, so do that
		resolver = new ContextResolver(scopeMappings);
		log.info("found {} listeners, {} groups and {} mappings",
				resolver.envelopes().size(), resolver.groups().size(), resolver.mappings().size());
		
		// we also need two loops for listener startup, since the second phase requires each listener to have completed the first phase
		log.debug("calling init() on listeners");
		for (var envelope : resolver.envelopes()) {
			try {
				log.trace("calling init() on {}", envelope);
				envelope.listener().init(this, resolver);
			} catch (ChrislieListener.ListenerException e) {
				throw new ChrisliebotException(String.format("error in init() of listener with source `%s`", envelope.source()), e);
			} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
				throw new ChrisliebotException(String.format("unhandled error in init() of listener with source `%s`", envelope.source()), e);
			}
		}
		log.debug("calling start() on listeners");
		for (var envelope : resolver.envelopes()) {
			try {
				log.trace("calling start() on {}", envelope);
				envelope.listener().start(this, resolver);
			} catch (ChrislieListener.ListenerException e) {
				throw new ChrisliebotException(String.format("error in start() of listener with source `%s`", envelope.source()), e);
			} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
				throw new ChrisliebotException(String.format("unhandled error in start() of listener with source `%s`", envelope.source()), e);
			}
		}
		
		// announce the resolver that's going to be used to services for command suggestion
		services.values().forEach(s -> s.announceResolver(resolver));
		
		// create dispatcher and hook into services
		dispatcher = new ChrislieDispatcher(this, resolver);
		services.values().forEach(s -> s.sink(dispatcher::dispatch));
	}
	
	@Override
	protected void shutDown() throws ChrisliebotException {
		// unhook services from dispatcher to stop messsage stream
		services.values().forEach(s -> s.sink(null));
		
		// shutdown dispatcher to cut listeners from message sink
		try {
			dispatcher.shutdown();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ChrisliebotException("interrupted while waiting for dispatcher to shut down", e);
		}
		
		/* there can't be any active listener callbacks after dispatcher shutdown, but listeners might still have other threads running
		 * so the next step is to tell listeners to shut down while services are still operational
		 */
		
		log.debug("calling stop() on listeners");
		for (var envelope : resolver.envelopes()) {
			var listener = envelope.listener();
			try {
				log.trace("calling stop() on {}", envelope);
				listener.stop(this, resolver);
			} catch (ChrislieListener.ListenerException e) {
				throw new ChrisliebotException("failed to shut down listener: " + envelope.source(), e);
			}
		}
		
		// shut down services since no more listeners are expected to access services
		log.info("shutting down services");
		for (var service : services.values()) {
			try {
				log.debug("shutting down service: {}", service.identifier());
				service.exit();
			} catch (ChrislieService.ServiceException e) {
				throw new ChrisliebotException("failed to shut down service: " + service.identifier(), e);
			}
		}
		
		// shut down shared ressources
		try {
			sharedResources.shutDown();
		} catch (ChrisliebotException e) {
			throw new ChrisliebotException("shutdown of shared ressources failed", e);
		}
	}
	
	public Optional<ChrislieService> service(String identifier) {
		return Optional.ofNullable(services.get(identifier));
	}
	
	public static class ChrisliebotException extends Exception {
		
		public ChrisliebotException() {
		}
		
		public ChrisliebotException(String message) {
			super(message);
		}
		
		public ChrisliebotException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public ChrisliebotException(Throwable cause) {
			super(cause);
		}
	}
	
	public class Managment {
		
		public void shutdown() {
			shutdownWithCode(EXIT_CODE_PROPER_SHUTDOWN);
		}
		
		public void restart() {
			shutdownWithCode(EXIT_CODE_RESTART);
		}
		
		public void upgrade() {
			shutdownWithCode(EXIT_CODE_UPGRADE);
		}
	}
}
