package chrisliebaer.chrisliebot.util;


import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import okhttp3.Request;
import org.kitteh.irc.client.library.util.Format;

import java.awt.*;
import java.util.function.Consumer;


public final class ErrorOutputBuilder {
	
	private static final Color ERROR_COLOR = Color.RED;
	
	private static final ErrorOutputBuilder PERMISSION_ERROR = new ErrorOutputBuilder()
			.fn(out -> out
					.title("Berechtigungsfehler")
					.description("Hierfür hast du nicht ausreichende Berechtigungen."));
	
	private OutputFunction fn;
	
	private ErrorOutputBuilder fn(OutputFunction fn) {
		this.fn = fn;
		return this;
	}
	
	public static ErrorOutputBuilder permission() {
		return PERMISSION_ERROR;
	}
	
	public static ErrorOutputBuilder remoteRequest(Request req, Throwable t) {
		var reason = t.getMessage();
		
		return new ErrorOutputBuilder()
				.fn(out -> {
					out.title("Verbindungsfehler");
					var description = out.description();
					
					if (reason == null || reason.isBlank())
						description.appendEscape("Konnte Server nicht erreichen.");
					else
						description.appendEscape("Konnte Server nicht erreichen: ").appendEscape(reason, ChrislieFormat.HIGHLIGHT);
				});
	}
	
	public static ErrorOutputBuilder remoteErrorCode(Request req, @SuppressWarnings("UnnecessaryFullyQualifiedName") retrofit2.Response<?> resp) {
		
		return new ErrorOutputBuilder()
				.fn(out -> {
					out.title("Server meldet fehler");
					out.description()
							.appendEscape(String.valueOf(resp.code()), ChrislieFormat.HIGHLIGHT)
							.append(" ")
							.appendEscape(resp.message(), ChrislieFormat.HIGHLIGHT);
				});
	}
	
	public static ErrorOutputBuilder remoteErrorCode(Request req, @SuppressWarnings("UnnecessaryFullyQualifiedName") okhttp3.Response resp) {
		return new ErrorOutputBuilder()
				.fn(out -> {
					out.title("Server meldet fehler");
					out.description()
							.appendEscape(String.valueOf(resp.code()), ChrislieFormat.HIGHLIGHT)
							.append(" ")
							.appendEscape(resp.message(), ChrislieFormat.HIGHLIGHT);
				});
	}
	
	public static ErrorOutputBuilder generic(String description) {
		return new ErrorOutputBuilder().fn(out -> defaultConvert(out.title("Fehler").description(description).convert()));
	}
	
	public static ErrorOutputBuilder generic(Consumer<PlainOutput> outFn) {
		return new ErrorOutputBuilder().fn(out -> {
			outFn.accept(out.description());
			defaultConvert(out.title("Fehler").convert());
		});
	}
	
	public static ErrorOutputBuilder throwable(Throwable throwable) {
		return new ErrorOutputBuilder().fn(out -> {
			out.title(throwable.getClass().getSimpleName());
			String msg = throwable.getMessage();
			if (msg != null && !msg.isBlank())
				out.description(msg);
		});
	}
	
	private static void defaultConvert(PlainOutput.PlainOutputSubstituion convert) {
		convert.appendEscapeSub("[${title}] ", Format.RED, Format.BOLD).appendEscapeSub("${description}");
	}
	
	public ChrislieOutput write(ChrislieOutput out) {
		out.color(ERROR_COLOR);
		fn.out(out);
		return out;
	}
	
	public void write(ChrislieMessage m) {
		write(m.channel().output()).send();
	}
	
	@FunctionalInterface
	private interface OutputFunction {
		
		public void out(ChrislieOutput out);
	}
	
}
