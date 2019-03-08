package chrisliebaer.chrisliebot.util;

import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Helper method that allows functionals to be used for {@link AbstractScheduledService} implementations instead of implementing a child class.
 */
@AllArgsConstructor
public class BetterScheduledService extends AbstractScheduledService {
	
	private @NonNull ExceptionalRunnable runnable;
	private @NonNull AbstractScheduledService.Scheduler scheduler;
	
	@Override
	protected void runOneIteration() throws Exception {
		runnable.run();
	}
	
	@Override
	protected Scheduler scheduler() {
		return scheduler;
	}
}
