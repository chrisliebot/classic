package chrisliebaer.chrisliebot.listener;

import chrisliebaer.chrisliebot.abstraction.ChrislieMessage;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.function.Predicate;

public interface ChatListener extends Predicate<ChrislieMessage> {
	
	public static class AllMessageListener implements ChatListener {
		
		@Override
		public boolean test(ChrislieMessage m) {
			return true;
		}
		
		public static AllMessageListener fromJson(Gson gson, JsonElement json) {
			return new AllMessageListener();
		}
	}
	
	public static class ChannelMessageListener implements ChatListener {
		
		@Override
		public boolean test(ChrislieMessage m) {
			return !m.channel().isDirectMessage();
		}
		
		public static ChannelMessageListener fromJson(Gson gson, JsonElement json) {
			return new ChannelMessageListener();
		}
	}
	
	public static class PrivateMessageListener implements ChatListener {
		
		@Override
		public boolean test(ChrislieMessage m) {
			return m.channel().isDirectMessage();
		}
		
		public static PrivateMessageListener fromJson(Gson gson, JsonElement json) {
			return new PrivateMessageListener();
		}
	}
}
