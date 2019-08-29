package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.config.CoreConfig;
import chrisliebaer.chrisliebot.config.JsonBotConfig;
import chrisliebaer.chrisliebot.config.scope.ScopeMapping;
import chrisliebaer.chrisliebot.protocol.ServiceBootstrap;
import chrisliebaer.chrisliebot.util.GsonValidator;
import chrisliebaer.chrisliebot.util.SystemProperty;
import com.google.common.util.concurrent.AbstractService;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Validation;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class Chrisliebot extends AbstractService {
	
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
	private final boolean exitOnDirty = SystemProperty.of("cfg.exitOnDirty", false, s -> true);
	
	/**
	 * In order to maintain control over how we load and save json files, this instance is supposed to be shared by the entire application.
	 */
	@SuppressWarnings("resource") private GsonValidator gson = new GsonValidator(new GsonBuilder()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create(), Validation.buildDefaultValidatorFactory().getValidator());
	/**
	 * This flag is set to indicate that at least one error occured that could not be fully recovered from. Meaning that there could be lingering threads or invalid
	 * handles. This always indicates an programming error.
	 */
	@Getter private boolean dirty;
	
	/**
	 * Set up before even starting up services. Provides commonly shared ressources like http clients, database connections worker pools, etc.
	 */
	@Getter private SharedResources sharedResources;
	
	/**
	 * List of currently active services.
	 */
	private Map<String, ChrislieService> services = new HashMap<>();
	
	public static void main(String[] args) {
		log.debug("default charset is: {}", Charset.defaultCharset());
		
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
			}
			
			@Override
			public void failed(State from, Throwable failure) {
				log.error("Chrisliebot has encountered a critical error during `{}` and will now terminate the process", from, failure);
				System.exit(EXIT_CODE_ERROR);
			}
		}, command -> new Thread(command, "Watchdog").start()); // we don't know the state of any shared executors, so we have to create a new thread
		bot.startAsync();
	}
	
	/**
	 * Calling this method will make chrisliebot enter the dirty state. The dirty state can only be cleared by restarting and indicates that a serious error has occured
	 * that might impact the integrity of the application because a consistant state can no longer be guaranteed. This is always the the result of a programming error.
	 */
	public void enterDirty() {
		if (!dirty)
			log.error("chrisliebot has entered dirty state, data integrity can no longer be guaranteed, contact developer and attach any logs");
		dirty = true;
	}
	
	@Override
	protected void doStart() {
		log.info("using core config `{}` and bot config `{}`", coreFile, botFile);
		
		CoreConfig coreCfg;
		try (var fr = new FileReader(coreFile)) {
			coreCfg = gson.fromJson(fr, CoreConfig.class);
		} catch (IOException e) {
			notifyFailed(new Exception("failed to load connection config", e));
			return;
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
				notifyFailed(new Exception(String.format("failed to start service `%s`", name), e));
				return;
			}
		}
		
		// separate loop so services can work in parallel
		for (var entry : services.entrySet()) {
			var name = entry.getKey();
			var service = entry.getValue();
			try {
				service.awaitReady();
			} catch (Exception e) {
				notifyFailed(new Exception(String.format("service `%s` failed to get ready", name), e));
				return;
			}
		}
		
		// on the first load, we abort on errors, since we have no fallback
		loadBotConfig(this::notifyFailed);
		
		notifyStarted();
	}
	
	private void loadBotConfig(Consumer<Throwable> exceptionHandler) { // exception handler is for potential reload function
		
		// scope mappings are populated with hardcoded scope, cnfig mappoings are added after to allow for overriding
		List<ScopeMapping> scopeMappings = new ArrayList<>();
		
		// deserializing config
		JsonBotConfig botConfig;
		try (var fr = new FileReader(botFile)) {
			botConfig = gson.fromJson(fr, JsonBotConfig.class);
		} catch (IOException e) {
			exceptionHandler.accept(new Exception("failed to load bot config file", e));
			return;
		}
		
		// instancing bot config is actually a very heavy task, don't be fooled
		try {
			scopeMappings.addAll(botConfig.instance(gson));
		} catch (JsonBotConfig.ConfigInitializeException e) {
			exceptionHandler.accept(new Exception("unable to instance bot config", e));
			return;
		}
		
		// listeners are created, but not yet initialized or started since this requires a context resolver and a chrisliebot instance, so do that
		ContextResolver resolver = new ContextResolver(scopeMappings);
		log.info("found {} listeners, {} groups and {} mappings",
				resolver.envelopes().size(), resolver.groups().size(), resolver.mappings().size());
		
		// we also need two loops for listener startup, since the second phase requires each listener to have completed the first phase
		log.debug("calling init() on listeners");
		for (var envelope : resolver.envelopes()) {
			try {
				envelope.listener().init(this, resolver);
			} catch (ChrislieListener.ListenerException e) {
				exceptionHandler.accept(new Exception(String.format("error in init() of listener with source `%s`", envelope.source()), e));
			} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
				enterDirty();
				exceptionHandler.accept(new Exception(String.format("unhandled error in init() of listener with source `%s`", envelope.source()), e));
			}
		}
		log.debug("calling start() on listeners");
		for (var envelope : resolver.envelopes()) {
			try {
				envelope.listener().start(this, resolver);
			} catch (ChrislieListener.ListenerException e) {
				exceptionHandler.accept(new Exception(String.format("error in start() of listener with source `%s`", envelope.source()), e));
				return;
			} catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
				enterDirty();
				exceptionHandler.accept(new Exception(String.format("unhandled error in start() of listener with source `%s`", envelope.source()), e));
				return;
			}
		}
		
		// create dispatcher and hook into services
		ChrislieDispatcher dispatcher = new ChrislieDispatcher(this, resolver);
		services.values().forEach(s -> s.sink(dispatcher::dispatch));
	}
	
	@Override
	protected void doStop() {
		notifyStopped();
	}
	
	public Optional<ChrislieService> service(String identifier) {
		return Optional.ofNullable(services.get(identifier));
	}
}
