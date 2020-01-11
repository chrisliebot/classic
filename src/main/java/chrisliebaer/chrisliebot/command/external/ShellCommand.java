package chrisliebaer.chrisliebot.command.external;

import chrisliebaer.chrisliebot.Chrisliebot;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.SerializedOutput;
import chrisliebaer.chrisliebot.config.ContextResolver;
import chrisliebaer.chrisliebot.util.GsonValidator;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ShellCommand extends ExternalCommandListener {
	
	private GsonValidator gson;
	private Config cfg;
	private ExternalMessageTranslator translator = new ExternalMessageTranslator();
	
	private final List<Thread> processes = new LinkedList<>(); // we are going to join on them anyway, so no reason to get fancy with a wrapper
	
	private volatile boolean shutdown;
	
	@Override
	protected ExternalCommandListener.@NonNull @org.checkerframework.checker.nullness.qual.NonNull Config externalConfig() {
		return cfg.ext;
	}
	
	@Override
	public void fromConfig(GsonValidator gson, JsonElement json) throws ListenerException {
		cfg = gson.fromJson(json, Config.class);
	}
	
	@Override
	public void init(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		gson = bot.sharedResources().gson();
		cfg.flex.forEach(translator::withFlex);
	}
	
	@Override
	public void stop(Chrisliebot bot, ContextResolver resolver) throws ListenerException {
		// bot framework ensures that this listener will not receive any more messages, so we don't need to care about that
		
		List<Thread> l;
		synchronized (processes) {
			// prevent terminating processes from modifying process list
			shutdown = true;
			
			// create copy since termination listener will also lock on processes and we need to wait for them to finish, otherwise we deadlock
			l = new ArrayList<>(processes);
		}
		
		log.debug("terminating {} pending processes", l.size());
		
		// first loop triggers termination
		for (var t : l)
			t.interrupt();
		
		// second loop waits on termination
		try {
			for (var t : l)
				t.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ListenerException("interrupted while shutting down processes", e);
		}
	}
	
	@Override
	protected void handleCommand(Invocation invc) throws ListenerException {
		dispatch(translator.toFlatMap(invc), invc.reply(), invc.exceptionHandler());
	}
	
	@Override
	protected void externalMessage(ListenerMessage msg) throws ListenerException {
		dispatch(translator.toFlatMap(msg), msg.reply(), msg.exceptionHandler());
	}
	
	private void dispatch(Map<String, String> flatMap, ChrislieOutput out, ExceptionHandler exceptionHandler) throws ListenerException {
		var procExec = new ProcessExecutor()
				.command(cfg.cmd)
				.redirectErrorStream(cfg.stderrToSdtOut)
				.environment(flatMap)
				.readOutput(true)
				.redirectOutputAlsoTo(Slf4jStream.of(log).asTrace())
				.redirectErrorAlsoTo(Slf4jStream.of(log).asTrace());
		
		if (cfg.dir != null)
			procExec.directory(new File(cfg.dir));
		
		switch (cfg.exitMethod) {
			case ANY:
				procExec.exitValueAny();
				break;
			case NORMAL:
				procExec.exitValueNormal();
				break;
			case LIST:
				procExec.exitValues(cfg.exitCodes);
				break;
		}
		
		if (cfg.timeout > 0)
			procExec.timeout(cfg.timeout, TimeUnit.MILLISECONDS);
		
		// this entire library sucks suchs massive donkey cocks that we simply go with the blocking route
		synchronized (processes) {
			if (shutdown)
				return;
			
			var t = new Thread(() -> handleProcess(procExec, out, exceptionHandler));
			processes.add(t);
			t.start();
		}
	}
	
	private void handleProcess(ProcessExecutor procExec, ChrislieOutput out, ExceptionHandler exceptionHandler) {
		Thread t = Thread.currentThread();
		try {
			var result = procExec.execute();
			
			// synchronize is required to prevent printing output while/after shell command has shut down
			synchronized (processes) {
				if (shutdown)
					return;
				
				doOutput(result, out, exceptionHandler);
			}
		} catch (IOException e) {
			exceptionHandler.escalateException(new ListenerException("failed to start process", e));
		} catch (InterruptedException e) {
			t.interrupt();
			if (shutdown)
				return;
			
			exceptionHandler.escalateException(new ListenerException("got interrupted while waiting for process to finish", e));
		} catch (TimeoutException e) {
			exceptionHandler.escalateException(new ListenerException("process timed out", e));
		} catch (InvalidExitValueException e) {
			exceptionHandler.escalateException(new ListenerException("process exited with invalid exit code"));
		} finally {
			synchronized (processes) {
				if (!shutdown)
					processes.remove(t);
			}
		}
	}
	
	private void doOutput(ProcessResult result, ChrislieOutput out, ExceptionHandler exceptionHandler) {
		if (cfg.out != null) {
			cfg.out.apply(out, s -> s.replace("${out}", result.outputUTF8())).send();
		} else {
			try {
				gson.fromJson(result.outputUTF8(), SerializedOutput.class).apply(out).send();
			} catch (JsonSyntaxException e) {
				exceptionHandler.escalateException(new ListenerException("failed to parse process output as json", e));
			}
		}
	}
	
	private static class Config {
		
		private @NotNull ExternalCommandListener.Config ext;
		
		/**
		 * Will redirect {@code stderr} to {@code stdout}.
		 */
		private boolean stderrToSdtOut;
		
		/**
		 * First element indicates binary followed by arg array.
		 */
		private @NotEmpty List<String> cmd;
		
		/**
		 * Optional path to working directory.
		 */
		private String dir;
		
		/**
		 * Additional environment variables that are passed to the process unmodified.
		 */
		private @NotNull Map<@NotNull String, @NotNull String> envMap = Map.of();
		
		/**
		 * List of all flex values that are passed to external listener. For security reasons it is impossible to specify wildcards.
		 */
		private @NotNull Set<@NotNull String> flex = Set.of();
		
		/**
		 * Numer of milliseconds after which process will forcefully be terminated. Set to 0 to disable (not recommended).
		 */
		private @PositiveOrZero long timeout;
		
		/**
		 * The method that's used to determine if an exit code should be considered valid.
		 */
		private @NonNull ExitCodeMethod exitMethod = ExitCodeMethod.NORMAL;
		
		/**
		 * A list of valid exit codes.
		 */
		private @NotNull int[] exitCodes = new int[0];
		
		/**
		 * If set process output will be fed into this serialized output. Otherwise the process output will be treated as a {@link SerializedOutput} JSON, resulting in a
		 * {@link ListenerException} if the output is not a valid JSON representation of a {@link SerializedOutput}.
		 */
		private SerializedOutput out;
		
		private enum ExitCodeMethod {
			/**
			 * Accept any exit code as valid.
			 */
			ANY,
			
			/**
			 * Only accept 0 as valid exit code.
			 */
			NORMAL,
			
			/**
			 * Consider any exit code listed as {@link #exitCodes} valid.
			 */
			LIST
		}
	}
}
