package chrisliebaer.chrisliebot.command.exec;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.SharedResources;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Slf4j
public class ShellCommand implements CommandExecutor {
	
	private static final int TIMEOUT = 10000;
	
	private static final String ENV_NICKNAME = "CB_NICKNAME";
	private static final String ENV_REALNAME = "CB_REALNAME";
	private static final String ENV_HOSTNAME = "CB_HOSTNAME";
	private static final String ENV_ACCOUNT = "CB_ACCOUNT"; // empty if none
	private static final String ENV_IS_ADMIN = "CB_IS_ADMIN"; // 1 or 0
	private static final String ENV_MODES = "CB_MODES";
	
	private static final String ENV_CHANNEL = "CB_CHANNEL"; // empty if privmsg
	private static final String ENV_MESSAGE = "CB_MESSAGE";
	private static final String ENV_ARGUMENT = "CB_ARGUMENT"; // not set on listener invocation
	
	private ProcessBuilder builder;
	
	public ShellCommand(List<String> cmd, String dir, Map<String, String> envMap, boolean stderrToSdtOut) {
		Preconditions.checkArgument(!cmd.isEmpty(), "command list is empty");
		
		builder = new ProcessBuilder();
		builder.command(cmd);
		builder.redirectErrorStream(stderrToSdtOut);
		
		if (dir != null)
			builder.directory(new File(dir));
		
		if (envMap != null)
			builder.environment().putAll(envMap);
	}
	
	@Override
	public synchronized void execute(Message m, String arg) {
		// env map is shared so we have to rebuild it every time we want to invoke a remote command
		var env = builder.environment();
		
		var u = m.user();
		env.put(ENV_NICKNAME, u.getNick());
		env.put(ENV_REALNAME, u.getRealName().orElse(""));
		env.put(ENV_HOSTNAME, u.getHost());
		env.put(ENV_ACCOUNT, u.getAccount().orElse(""));
		env.put(ENV_IS_ADMIN, m.isAdmin() ? "1" : "0");
		env.put(ENV_MODES, getUserModeString(m.channel().orElse(null), u));
		
		env.put(ENV_CHANNEL, m.channel().map(Channel::getName).orElse(""));
		env.put(ENV_MESSAGE, m.message());
		
		if (arg == null)
			env.remove(ENV_ARGUMENT);
		else
			env.put(ENV_ARGUMENT, arg);
		
		try {
			handleProcess(builder.start(), m);
		} catch (@SuppressWarnings("OverlyBroadCatchBlock") Throwable e) {
			m.reply(C.error("Bei der Verbeitung dieses Befehls ging etwas schief."));
			log.warn(C.LOG_IRC, "failed to execute remote command: {} ({})", builder.command(), e.getMessage());
		}
	}
	
	private synchronized void handleProcess(Process p, Message m) {
		// copy command invocation for logging since accessing builder in callbacks is not thread safe
		var command = ImmutableList.copyOf(builder.command());
		
		// start timer that will kill process after timeout
		var timer = new TimerTask() {
			@Override
			public void run() {
				p.destroyForcibly();
			}
		};
		
		// register for process exit
		SharedResources.INSTANCE().timer().schedule(timer, TIMEOUT);
		p.onExit().thenAccept(pp -> {
			timer.cancel();
			
			if (pp.exitValue() != 0) {
				m.reply(C.error("Bei der Ausführung des Befehls trat ein Fehler auf."));
				log.warn(C.LOG_IRC, "command {} exited with non-zero error code {}", command, pp.exitValue());
				return;
			}
			
			// bless IO utils
			try {
				var lines = IOUtils.readLines(pp.getInputStream(), StandardCharsets.UTF_8);
				lines.forEach(m::reply);
			} catch (IOException e) {
				log.error(C.LOG_IRC, "unexpected io error while reading from process output of {}", command, e);
			}
		}).exceptionally(t -> {
			timer.cancel();
			log.warn(C.LOG_IRC, "an error occured while exeucting {}: {}", command, t.getMessage());
			m.reply(C.error("Der Befehl konnte nicht ausgeführt werden."
					+ (t.getMessage() != null ? "(" + t.getMessage() + ")" : "")));
			
			return null;
		});
	}
	
	private static String getUserModeString(Channel channel, User user) {
		if (channel == null)
			return "";
		
		return channel.getUserModes(user)
				.map(channelUserModes -> channelUserModes.stream()
						.map(ChannelUserMode::getNickPrefix)
						.map(String::valueOf)
						.collect(Collectors.joining(", ")))
				.orElse("");
	}
	
	public static ShellCommand fromJson(Gson gson, JsonElement json) {
		var cfg = gson.fromJson(json, Config.class);
		return new ShellCommand(cfg.cmd(), cfg.dir(), cfg.envMap, cfg.stderrToSdtOut());
	}
	
	@Data
	private static class Config {
		
		private boolean stderrToSdtOut; // will merge stderr with stdout
		private List<String> cmd;
		private String dir;
		private Map<String, String> envMap;
	}
}
