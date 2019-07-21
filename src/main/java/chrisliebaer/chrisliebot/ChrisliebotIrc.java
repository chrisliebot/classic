package chrisliebaer.chrisliebot;

import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.command.CommandDispatcher;
import chrisliebaer.chrisliebot.config.CommandConfig;
import chrisliebaer.chrisliebot.config.ConfigContext;
import chrisliebaer.chrisliebot.protocol.IrcBootstrap;
import chrisliebaer.chrisliebot.protocol.irc.IrcConfig;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ChrisliebotIrc implements BotManagment {
	
	// proper shutdown of logging framework
	static {
		System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
	}
	
	private static final String PROPERTY_CONFIG_DIR = "config.dir";
	
	@Getter private boolean dirty; // indicates that at least one start() or stop() cycle failed, application may be inconsistent
	
	private final AtomicBoolean managmentLock = new AtomicBoolean(false);
	
	private Gson gson;
	
	private ChrislieService service;
	
	private File botCfgFile;
	private File cmdCfgFile;
	private IrcConfig.BotConfig botCfg;
	private CommandConfig cmdCfg;
	
	private ConfigContext configContext;
	private CommandDispatcher dispatcher;
	
	public static void main(String[] args) throws Exception {
		File configDir = new File(System.getProperty(PROPERTY_CONFIG_DIR, "."));
		
		ChrisliebotIrc bot = new ChrisliebotIrc(
				new File(configDir, "bot.json"),
				new File(configDir, "commands.json"));
		bot.start();
	}
	
	public ChrisliebotIrc(@NonNull File botCfgFile, @NonNull File cmdCfgFile) {
		this.botCfgFile = botCfgFile;
		this.cmdCfgFile = cmdCfgFile;
		
		log.info("using config files: {}, {}", botCfgFile, cmdCfgFile);
	}
	
	private static void exceptionLogger(Throwable t) {
		log.error("error in irc library", t);
	}
	
	private void makeDirty() {
		if (!dirty)
			log.error("application entered tainted state, can no longer enforce data safety, dirty bit has been set");
		dirty = true;
	}
	
	public void start() throws IOException {
		SharedResources.INSTANCE().startAsync().awaitRunning();
		gson = SharedResources.INSTANCE().gson();
		
		// connection will never be reloaded
		loadBotConfig();
		startConnection();
		
		// load configuration, might fail if config error
		try {
			loadCommandConfig();
			reload(true);
		} catch (Exception e) {
			// here is hoping we are at least able to set up the basic stuff
			try {
				log.error("failed to create initial bot setup, entering emergency state", e);
				configContext = ConfigContext.emergencyContext(this);
				configContext.start();
				dispatcher = new CommandDispatcher(botCfg.prefix(), configContext.commandTable(), configContext.chatListener());
				service.sink(dispatcher::dispatch);
			} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable e1) {
				// you sir, are royally fucked
				log.error("failed to load emergency environment, you are on your own, good luck", e1);
				System.exit(-1);
			}
		}
		
		log.info("bot is up and running for your enjoyment");
	}
	
	private void loadBotConfig() throws IOException {
		try (FileReader botFr = new FileReader(botCfgFile)) {
			botCfg = gson.fromJson(botFr, IrcConfig.BotConfig.class);
		}
	}
	
	private void loadCommandConfig() throws IOException {
		try (FileReader cmdFr = new FileReader(cmdCfgFile)) {
			cmdCfg = gson.fromJson(cmdFr, CommandConfig.class);
			
			// load used command definitions
			for (String use : cmdCfg.use()) {
				File useFile = new File(use + ".json");
				try (FileReader useFr = new FileReader(useFile)) {
					cmdCfg.commandConfig().merge(gson.fromJson(useFr, CommandConfig.CommandRegistry.class));
				}
			}
		}
	}
	
	private void startConnection() {
		IrcBootstrap bootstrap = new IrcBootstrap(botCfg);
		service = bootstrap.service();
	}
	
	private void reload(boolean firstRun) throws Exception {
		log.info(C.LOG_PUBLIC, "reloading configuration, this may take a few moments, the application may be unresponsive during that time");
		
		// on first launch, config is already up to date
		if (!firstRun) {
			loadBotConfig();
			loadCommandConfig();
		}
		
		// use local variable since we can't already replace the old instances
		CommandDispatcher dispatcher;
		ConfigContext configContext;
		
		// errors in this stage are not fatal, simply continue with old config
		configContext = ConfigContext.fromConfig(this, cmdCfg);
		
		try {
			configContext.start();
			configContext.passService(service);
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable t) {
			// error in start may result in inconsistent state
			makeDirty();
			throw t;
		}
		
		// dispatcher can only be created after successfull .start()
		dispatcher = new CommandDispatcher(botCfg.prefix(), configContext.commandTable(), configContext.chatListener());
		
		// if we reached this point, we are safe to swap old dispatcher with new one
		if (!firstRun)
			unload();
		
		// we need to wait before we assign this new dispatcher since it will be cleared during unload
		this.configContext = configContext;
		this.dispatcher = dispatcher;
		
		service.sink(dispatcher::dispatch);
		
		// we did it
	}
	
	private void unload() throws Exception {
		// if this method is entered, a new dispatcher is ready to be connected, we can safely remove the old one
		
		// unhook command executor
		service.sink(null);
		
		try {
			// may throw a ton of exception if shutdown fails
			configContext.stop();
			
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable t) {
			// error in stop may result in inconsistent state and lingering threads or listeners
			makeDirty();
			throw t;
		}
		
		// invalidate dispatcher and context to catch "use after free" bugs
		dispatcher = null;
		configContext = null;
	}
	
	private void stop(int code) {
		try {
			log.info("shutdown has been triggered, shutting down and returning with error code {}", code);
			unload();
			service.exit();
			System.exit(code);
		} catch (Exception e) {
			log.error("failed to shut down, forcing exit", e);
			System.exit(-1);
		}
	}
	
	@Override
	public CompletableFuture<Void> doReload() {
		var future = new CompletableFuture<Void>();
		if (managmentLock.compareAndSet(false, true)) {
			new Thread(() -> {
				try {
					reload(false);
					future.complete(null);
				} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable t) {
					future.completeExceptionally(t);
					log.error("reload during runtime failed", t);
				} finally {
					// release lock
					managmentLock.set(false);
				}
			}).start();
		} else
			future.completeExceptionally(new IllegalStateException("Es wird bereits eine Managmentoperation durchgeführt."));
		return future;
	}
	
	@Override
	public void doShutdown(int code) {
		if (managmentLock.compareAndSet(false, true)) {
			new Thread(() -> stop(code)).start(); // never returns, so no reason to ever reset lock
		}
	}
	
	@Override
	public void doReconect() {
		service.reconnect();
	}
}
