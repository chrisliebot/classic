package chrisliebaer.chrisliebot.util;

/**
 * Just a regular {@link Runnable} but it can throw exceptions.
 */
@FunctionalInterface
public interface ExceptionalRunnable {
	
	public void run() throws Exception;
}
