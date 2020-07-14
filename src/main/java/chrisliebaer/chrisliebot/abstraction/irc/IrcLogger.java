package chrisliebaer.chrisliebot.abstraction.irc;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.Client;
import org.slf4j.MDC;

@Slf4j
@AllArgsConstructor
public final class IrcLogger {
	public final String name;
	
	public static void attach(@NonNull String name, @NonNull Client.Builder builder) {
		var logger = new IrcLogger(name);
		
		builder.listeners()
				.exception(logger::error)
				.input(logger::in)
				.output(logger::out);
	}
	
	private void error(Throwable t) {
		runMdc(() -> log.error("error in irc library", t));
	}
	
	private void in(String s) {
		runMdc(() -> log.trace("<<< {}", s));
	}
	
	private void out(String s) {
		runMdc(() -> log.trace(">>> {}", s));
	}
	
	private void runMdc(Runnable r) {
		MDC.put("service", name);
		r.run();
		MDC.remove("service");
	}
}
