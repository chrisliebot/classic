package chrisliebaer.chrisliebot.config;

import chrisliebaer.chrisliebot.BotManagment;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieService;
import chrisliebaer.chrisliebot.command.ChrisieCommand;
import chrisliebaer.chrisliebot.command.CommandContainer;
import chrisliebaer.chrisliebot.command.CommandDispatcher;
import chrisliebaer.chrisliebot.command.basic.*;
import chrisliebaer.chrisliebot.command.bottlespin.BottleSpinCommand;
import chrisliebaer.chrisliebot.command.choice.ChoiceCommand;
import chrisliebaer.chrisliebot.command.dns.DnsCommand;
import chrisliebaer.chrisliebot.command.flip.FlipCommand;
import chrisliebaer.chrisliebot.command.help.HelpCommand;
import chrisliebaer.chrisliebot.command.manage.*;
import chrisliebaer.chrisliebot.command.mock.MockCommand;
import chrisliebaer.chrisliebot.command.random.CoinCommand;
import chrisliebaer.chrisliebot.command.random.DiceCommand;
import chrisliebaer.chrisliebot.command.sed.SedCommand;
import chrisliebaer.chrisliebot.command.special.KlaxaCommand;
import chrisliebaer.chrisliebot.command.unicode.UnicodeCommand;
import chrisliebaer.chrisliebot.command.until.UntilCommand;
import chrisliebaer.chrisliebot.command.urbandictionary.UrbanDictionaryCommand;
import chrisliebaer.chrisliebot.command.vote.VoteCommand;
import chrisliebaer.chrisliebot.config.CommandConfig.CommandDefinition;
import chrisliebaer.chrisliebot.config.CommandConfig.ListenerDefinition;
import chrisliebaer.chrisliebot.listener.ChatListener;
import chrisliebaer.chrisliebot.listener.ListenerContainer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.NonNull;
import org.kitteh.irc.client.library.util.CtcpUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


public final class ConfigContext {
	
	private static final String INSTACE_METHOD_NAME = "fromJson";
	
	private Gson gson = SharedResources.INSTANCE().gson();
	
	private BotManagment bot;
	private Map<String, CommandContainer> cmdDefs = new HashMap<>();
	private Map<String, String> bindings = new HashMap<>();
	private List<ListenerContainer> listener = new ArrayList<>();
	
	private Map<String, CommandDefinition> cfgCmdDefs;
	private Map<String, List<String>> cfgCmdBindings;
	private List<ListenerDefinition> cfgListener;
	private Set<String> unbind;
	
	// private implementation of interface to be passed to commands upon creation
	private PreConfigAccessor preConfigAccessor = cmdDef -> {
		var cmd = cmdDefs.get(cmdDef);
		Preconditions.checkState(cmd != null, "failed to find command definition with name : " + cmdDef);
		return cmd;
	};
	
	private ConfigContext(@NonNull BotManagment bot,
						  @NonNull Map<String, CommandDefinition> cfgCmdDefs,
						  @NonNull Map<String, List<String>> cfgCmdBindings,
						  @NonNull List<ListenerDefinition> cfgListener,
						  @NonNull Set<String> unbind)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		this.bot = bot;
		this.cfgCmdDefs = cfgCmdDefs;
		this.cfgCmdBindings = cfgCmdBindings;
		this.cfgListener = cfgListener;
		this.unbind = unbind;
		
		createDefaultCommands();
		createDefaultListener();
		
		loadCommandDefinitions();
		loadCommandBindings();
		loadListener();
	}
	
	public void start() throws Exception {
		for (var en : cmdDefs.entrySet()) {
			try {
				en.getValue().start();
			} catch (Exception e) {
				throw new IllegalArgumentException("cmdDef " + en.getKey() + " failed to start", e);
			}
		}
	}
	
	public void stop() throws Exception {
		for (var en : cmdDefs.entrySet()) {
			try {
				en.getValue().stop();
			} catch (Exception e) {
				throw new IllegalArgumentException("cmdDef " + en.getKey() + " failed to stop", e);
			}
		}
	}
	
	private void createDefaultCommands() {
		addCommandDefinition("shutdown", new ShutdownCommand(bot),
				"Beendet den Botprozess vollständig.");
		addCommandDefinition("restart", new RestartCommand(bot),
				"Beendet den Botprozess und startet ihn anschließend neu.");
		addCommandDefinition("upgrade", new UpgradeCommand(bot),
				"Läd den neusten Quellcode und Konfiguration von git und führt ein Upgrade durch.");
		addCommandDefinition("reload", new ReloadCommand(bot),
				"Versucht Teile der Konfiguration neu zu Laden.");
		addCommandDefinition("reconnect", new ReconnectCommand(bot),
				"Trennt die Verbindung mit dem aktuellen IRC Server und baut sie erneut auf.");
		addCommandDefinition("dirty", new DirtyCheck(bot),
				"Prüft ob ein Konfigurationsfehler aufgetreten ist, der einen ungültigen Zustand erzeugt haben könnte.");
		addCommandDefinition("help", new HelpCommand(bindings, cmdDefs),
				"Zeigt Informationen über den übergebenen Befehl an.");
		addCommandDefinition("channellist", new ChannelListCommand(),
				"Zeigt dir an in welchen Channeln du mich gerade findest.");
		addCommandDefinition("uptime", new UptimeCommand(),
				"Lass mal gucken wer den Längeren hat.");
		addCommandDefinition("join", new JoinCommand(),
				"Joint dem angegebenen Channel (mit optionalem Passwort).");
		addCommandDefinition("part", new PartCommand(),
				"Verlässt den angegebenen Channel.");
		addCommandDefinition("nick", new NickCommand(),
				"Setzt den Nickname des Bots.");
		addCommandDefinition("echo", ChrislieMessage::reply,
				"Gibt den übergebenen Inhalt wieder aus.");
		addCommandDefinition("say", new SayCommand(),
				"Sendet eine Nachricht an ein beliebiges Ziel.");
		addCommandDefinition("me", (m, s) -> m.reply(CtcpUtil.toCtcp("ACTION " + s)),
				"Ich tu was du willst.");
		addCommandDefinition("ping", (m, s) -> m.reply("pong"),
				"Eine Runde Ping-Pong.");
		addCommandDefinition("pong", (m, s) -> m.reply("ping"),
				"Eine Runde Pong-Ping.");
		addCommandDefinition("dice", new DiceCommand(6),
				"Keine Ahnung welche Antwort die richtige ist? Werf einen Würfel!");
		addCommandDefinition("coin", new CoinCommand(),
				"Wirf eine Münze und lass den Zufall für dich entscheiden. Sei faul!");
		addCommandDefinition("bottlespin", new BottleSpinCommand(), "Drehe eine Flasche und finde einen zufälligen User aus dem Channel.");
		addCommandDefinition("dns", new DnsCommand(),
				"<host> [<type>]");
		addCommandDefinition("klaxa", new KlaxaCommand(),
				"Shortcut um klaxa zu begrüßen.");
		addCommandDefinition("vote", new VoteCommand(),
				"Starte eine Umfrage mit: !vote <Frage>? Option1, Option2, Option3, ... oder nimm an einer Umfrage Teil mit !vote <OptNum>");
		addCommandDefinition("until", new UntilCommand(), "Kein Geld für einen Kalender. Ich berechne für dich, wie lange ein Datum noch entfernt ist.");
		addCommandDefinition("flip", new FlipCommand(), "˙ɟdoʞ ʇɥǝʇs ʇlǝM ǝᴉp");
		addCommandDefinition("unicode", new UnicodeCommand(), "Keine Ahnung von Unicode? Geb mir entweder ein Zeichen oder einen Codepoint (startet mit U+).");
		addCommandDefinition("choice", new ChoiceCommand(), "Ich treff für dich die wirklich wichtigen Entscheidungen. Auflistung der Auswahloptionen mit Komma.");
		addCommandDefinition("urban", new UrbanDictionaryCommand(), "Schlägt einen Begriff im Urban Dictionary nach. urbandictionary.com");
		addCommandDefinition("mock", new MockCommand(), "DiEsEr BeFheHl iSt sEhR gUt.");
		
		// do it the lazy way, every command is also bound to it's own name
		cmdDefs.keySet().forEach(d -> addCommandBinding(d, d));
		
		// but also add some aliases
		addCommandBinding("action", "me");
		addCommandBinding("h", "help");
		addCommandBinding("hilfe", "help");
		addCommandBinding("echo", "return");
		addCommandBinding("tell", "say");
		addCommandBinding("v", "vote");
		addCommandBinding("random", "dice");
		addCommandBinding("würfel", "dice");
		addCommandBinding("münze", "coin");
		addCommandBinding("spinbottle", "bottlespin");
		addCommandBinding("flaschendrehen", "bottlespin");
		addCommandBinding("bis", "until");
		addCommandBinding("wahl", "choice");
		
		// we want to unbind commands by their binding, not by their definition
		bindings.entrySet().removeIf(bnd -> unbind.contains(bnd.getValue()));
		
		// some default commands are not ment to be invoked by text commands and are therefore defined later
		addCommandDefinition("sed", new SedCommand(), null);
	}
	
	private void createDefaultListener() {
		// there aren't any right now
	}
	
	private void addCommandDefinition(@NonNull String name, @NonNull ChrisieCommand executor, String help) {
		Preconditions.checkArgument(!cmdDefs.containsKey(name), "duplicated command definition: " + name);
		cmdDefs.put(name, new CommandContainer(executor, help));
	}
	
	private void addCommandBinding(@NonNull String exposed, @NonNull String def) {
		Preconditions.checkArgument(!bindings.containsKey(exposed), "duplicated binding, exposed name: " + exposed);
		Preconditions.checkArgument(cmdDefs.containsKey(def), "no command definition with name: " + def);
		bindings.put(exposed, def);
	}
	
	private void loadCommandDefinitions()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		for (var e : cfgCmdDefs.entrySet()) {
			var executor = instanceExecutor(e.getValue());
			addCommandDefinition(e.getKey(), executor, e.getValue().help());
			
			if (e.getValue().map())
				addCommandBinding(e.getKey(), e.getKey());
		}
	}
	
	private void loadCommandBindings() {
		for (var e : cfgCmdBindings.entrySet()) {
			e.getValue().forEach(s -> addCommandBinding(s, e.getKey()));
		}
	}
	
	private void loadListener() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		for (var listenerDef : cfgListener) {
			List<CommandContainer> trigger = listenerDef.trigger().stream()
					.map(input -> {
						var container = cmdDefs.get(input);
						Preconditions.checkArgument(container != null, "trigger to unknown command definition: " + input);
						
						return container;
					}).collect(Collectors.toList());
			
			listener.add(new ListenerContainer(instanceListener(listenerDef), trigger));
		}
	}
	
	/**
	 * Remove the indirection via the binding table for use within the {@link CommandDispatcher}.
	 *
	 * @return Immutable command table.
	 */
	public Map<String, CommandContainer> commandTable() {
		HashMap<String, CommandContainer> t = new HashMap<>(bindings.size());
		for (var e : bindings.entrySet()) {
			// all values are set, this is enforced in the accessor methods
			t.put(e.getKey(), cmdDefs.get(e.getValue()));
		}
		return ImmutableMap.copyOf(t);
	}
	
	public Collection<ListenerContainer> chatListener() {
		return ImmutableList.copyOf(listener);
	}
	
	private ChrisieCommand instanceExecutor(CommandDefinition def)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		String clazzStr = def.clazz();
		Class<?> clazz = Class.forName(clazzStr);
		
		// try all instance signatures
		Optional<Method> maybeMethod;
		
		maybeMethod = getMethodEx(clazz, INSTACE_METHOD_NAME, Gson.class, JsonElement.class);
		if (maybeMethod.isPresent())
			return (ChrisieCommand) maybeMethod.get().invoke(null, gson, def.config());
		
		maybeMethod = getMethodEx(clazz, INSTACE_METHOD_NAME, Gson.class, JsonElement.class, PreConfigAccessor.class);
		if (maybeMethod.isPresent())
			return (ChrisieCommand) maybeMethod.get().invoke(null, gson, def.config(), preConfigAccessor);
		
		throw new NoSuchMethodException("class " + clazz + " is missing " + INSTACE_METHOD_NAME + " method");
	}
	
	private static <T> Optional<Method> getMethodEx(Class<T> clazz, String name, Class<?>... parameterTypes) {
		try {
			return Optional.of(clazz.getMethod(name, parameterTypes));
		} catch (NoSuchMethodException ignore) {
			return Optional.empty();
		}
	}
	
	private ChatListener instanceListener(ListenerDefinition def)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		String clazzStr = def.clazz();
		Class<?> clazz = Class.forName(clazzStr);
		Method method = clazz.getMethod(INSTACE_METHOD_NAME, Gson.class, JsonElement.class);
		return (ChatListener) method.invoke(null, gson, def.config());
	}
	
	public static ConfigContext fromConfig(@NonNull BotManagment bot, @NonNull CommandConfig cmdCfg)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		var registry = cmdCfg.commandConfig();
		return new ConfigContext(bot,
				registry.cmdDef(),
				registry.cmdBinding(),
				registry.listener(),
				registry.unbind());
	}
	
	public static ConfigContext emergencyContext(@NonNull BotManagment bot)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		
		// emergency context doesn't have any external command definitions
		return new ConfigContext(bot,
				Map.of(),
				Map.of(),
				List.of(),
				Set.of());
	}
	
	public void passService(@NonNull ChrislieService service) throws Exception {
		for (CommandContainer container : cmdDefs.values()) {
			container.init(service);
		}
	}
	
	/**
	 * Passed to commands during creation to access config context for meta commands.
	 */
	public interface PreConfigAccessor {
		
		public CommandContainer getCommandByDefinition(String cmdDef);
	}
}
