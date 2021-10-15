package chrisliebaer.chrisliebot.command.cron;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieIdentifier;
import chrisliebaer.chrisliebot.command.ChrislieDispatcher;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.google.gson.JsonElement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.validation.constraints.NotEmpty;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CronListener implements ChrislieListener {
	
	
	private Config config;
	
	private Chrisliebot bot;
	private ContextResolver resolver;
	
	private ScheduledExecutorService executorService;
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		config = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		this.bot = bot;
		executorService = bot.sharedResources().timer();
	}
	
	@Override
	public void start(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		for (var entry : config.entries) {
			queueRun(entry);
		}
		
		log.info("loaded {} cron entries", config.entries.size());
	}
	
	private void queueRun(CronEntry entry) {
		var now = ZonedDateTime.now(config.zoneId);
		var executionTime = ExecutionTime.forCron(entry.cron);
		var maybeDuration = executionTime.timeToNextExecution(now);
		
		if (maybeDuration.isEmpty()) {
			log.debug("no future executions for cron entry: {}", entry);
			return;
		}
		var duration = maybeDuration.get();
		
		log.trace("schedule execution in {} for {}", duration, entry);
		
		executorService.schedule(() -> executeCron(entry), duration.toMillis(), TimeUnit.MILLISECONDS);
	}
	
	private void executeCron(CronEntry entry) {
		var maybeChannel = entry.channel.channel(bot);
		if (maybeChannel.isPresent()) {
			var channel = maybeChannel.get();
			var message = new CronCommandMessage(channel.service().botUser(), channel, entry.msg, entry.asParse());
			bot.dispatcher().dispatch(message);
		} else {
			log.warn("unable to find channel for cron: {}", entry);
		}
	
		// rerun even if channel was not available (might have been down or just starting)
		queueRun(entry);
	}
	
	@Data
	private static class Config {
		private ZoneId zoneId;
		
		private List<CronEntry> entries;
	}
	
	@Data
	private static class CronEntry {
		@NonNull
		private Cron cron;
		
		private ChrislieIdentifier.ChannelIdentifier channel;
		
		@NonNull @NotEmpty
		private String alias;
		
		private String args;
		
		private String msg;
		
		public ChrislieDispatcher.CommandParse asParse() {
			return new ChrislieDispatcher.CommandParse(alias, args == null ? "" : args);
		}
	}
}
