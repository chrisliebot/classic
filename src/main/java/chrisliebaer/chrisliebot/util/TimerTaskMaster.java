package chrisliebaer.chrisliebot.util;

import edu.emory.mathcs.backport.java.util.Collections;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static chrisliebaer.chrisliebot.C.unsafeCast;

/**
 * This class is extending the regular {@link TimerTask} by a commonly found paradigma where an entity wants to create
 * timer tasks but also needs to stop created timer during a shutdown callback. With the regular TimerTask you can't
 * prevent timers from executing their {@link Runnable} after they have been scheduled on their {@link Timer} instance.
 * This class makes all it's timers synchronize on a shared lock while also making sure that each timer is checking a
 * shutdown flag before continuing executing. This approach allows the class owning the {@link TimerTaskMaster} to not
 * only cancel all inactive timers, but also ensuring that no timer is currently scheduled for executing.
 */
public class TimerTaskMaster { // TODO: this class might have fundamental flaws within it's synchronisation, maybe trash it
	
	private final Object lock;
	private volatile boolean shutdown;
	
	private final Set<TimerTaskSlave> timers = unsafeCast(Collections.newSetFromMap(new IdentityHashMap<>()));
	
	/**
	 * Creates a new timer task master. Due to how the tasks in this class work, it is vital to provide an acurate lock
	 * object.
	 *
	 * @param lock The lock object that will be used to synchronize state changes to this class.
	 */
	public TimerTaskMaster(@NonNull final Object lock) {
		this.lock = lock;
	}
	
	/**
	 * Creates a new timer that will be controlled with this master instance.
	 *
	 * @param r The runnable to execute, just like a regular {@link TimerTask} would.
	 * @return A timer that will behave as described in this classes documentation.
	 */
	@SuppressWarnings("ReturnOfInnerClass")
	public TimerTask createTimer(Runnable r) {
		synchronized (lock) {
			var timer = new TimerTaskSlave(r);
			timers.add(timer);
			return timer;
		}
	}
	
	/**
	 * Cancells all pending timers and prevents currently running timers from executing.
	 */
	public void cancel() {
		synchronized (lock) {
			if (shutdown)
				throw new IllegalStateException("you are not allowed to cancel this instance twice");
			
			shutdown = true;
			
			for (var timer : timers)
				timer.cancel();
			timers.clear();
		}
	}
	
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private final class TimerTaskSlave extends TimerTask {
		
		private final @NonNull Runnable r;
		
		@Override
		public void run() {
			synchronized (lock) {
				if (shutdown)
					return;
				
				// timer would be removed in #cancel so no need to remove ourself if shutdown is true
				timers.remove(this);
				
				r.run();
			}
		}
	}
}
