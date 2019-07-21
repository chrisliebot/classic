package chrisliebaer.chrisliebot;

import java.util.concurrent.CompletableFuture;

public interface BotManagment {
	
	public boolean dirty();
	
	public CompletableFuture<Void> doReload();
	
	public void doShutdown(int code);
	
	public void doReconect();
}
