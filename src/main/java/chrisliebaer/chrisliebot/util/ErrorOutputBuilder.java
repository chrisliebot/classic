package chrisliebaer.chrisliebot.util;


import chrisliebaer.chrisliebot.abstraction.ChrislieFormat;
import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import okhttp3.Request;

import java.awt.*;

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
