package org.example.jpb.core.benchmark;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.example.jpb.core.benchmark.enums.ForkBenchmarkStatus;
import org.example.jpb.core.benchmark.model.ForkBenchmarkRequest;
import org.example.jpb.core.benchmark.model.ForkBenchmarkResult;
import org.example.jpb.core.benchmark.model.ForkBenchmarkTransport;

public final class JavaForkProcessLauncher
	implements org.example.jpb.core.benchmark.model.ForkProcessLauncher {

	private static final long TERMINATION_GRACE_PERIOD_MILLIS = 5_000L;
	private static final int MAX_STDERR_BYTES = 64 * 1024;

	private final ForkBenchmarkTransport transport;

	public JavaForkProcessLauncher(ForkBenchmarkTransport transport) {
		this.transport = transport;
	}

	@Override
	public ForkBenchmarkResult launch(ForkBenchmarkRequest request) throws IOException {
		Process process = new ProcessBuilder(buildCommand()).redirectErrorStream(false).start();

		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		Thread stderrDrainer = startDrainer(
			process.getErrorStream(),
			stderr,
			MAX_STDERR_BYTES,
			"fork-stderr-" + request.getForkIndex()
		);

		try {
			writeRequest(process, request);

			boolean completed = process.waitFor(
				request.getConfig().getForkTimeoutMillis(),
				TimeUnit.MILLISECONDS
			);

			if (!completed) {
				destroyProcess(process);
				joinQuietly(stderrDrainer);

				return failed(
					request,
					"Fork timed out after " +
					request.getConfig().getForkTimeoutMillis() +
					" ms. stderr: " +
					stderrText(stderr)
				);
			}

			joinQuietly(stderrDrainer);

			if (process.exitValue() != 0) {
				return failed(
					request,
					"Fork exited with code " + process.exitValue() + ". stderr: " + stderrText(stderr)
				);
			}

			return transport.readResult(process.getInputStream());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			destroyProcess(process);

			return failed(request, "Fork execution was interrupted. stderr: " + stderrText(stderr));
		} finally {
			closeQuietly(process.getOutputStream());
			closeQuietly(process.getInputStream());
			closeQuietly(process.getErrorStream());
		}
	}

	private List<String> buildCommand() {
		String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();

		return List.of(
			javaExecutable,
			"-cp",
			System.getProperty("java.class.path"),
			ForkBenchmarkWorker.class.getName()
		);
	}

	private void writeRequest(Process process, ForkBenchmarkRequest request) throws IOException {
		try (OutputStream outputStream = process.getOutputStream()) {
			transport.writeRequest(outputStream, request);
			outputStream.flush();
		}
	}

	private Thread startDrainer(
		InputStream inputStream,
		ByteArrayOutputStream target,
		int maxBytes,
		String threadName
	) {
		Thread thread = new Thread(() -> drain(inputStream, target, maxBytes), threadName);

		thread.setDaemon(true);
		thread.start();

		return thread;
	}

	private void drain(InputStream inputStream, ByteArrayOutputStream target, int maxBytes) {
		byte[] buffer = new byte[4_096];
		int totalBytes = 0;

		try (inputStream) {
			int bytesRead;

			while ((bytesRead = inputStream.read(buffer)) != -1) {
				int writableBytes = Math.min(bytesRead, maxBytes - totalBytes);

				if (writableBytes > 0) {
					target.write(buffer, 0, writableBytes);
					totalBytes += writableBytes;
				}
			}
		} catch (IOException ignored) {
			// process termination can close stderr while its being drained, don't worry
		}
	}

	private void destroyProcess(Process process) {
		process.destroy();

		try {
			if (!process.waitFor(TERMINATION_GRACE_PERIOD_MILLIS, TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
				process.waitFor(TERMINATION_GRACE_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
		}
	}

	private ForkBenchmarkResult failed(ForkBenchmarkRequest request, String errorMessage) {
		return ForkBenchmarkResult
			.builder()
			.forkIndex(request.getForkIndex())
			.status(ForkBenchmarkStatus.FAILED)
			.errorMessage(errorMessage)
			.build();
	}

	private String stderrText(ByteArrayOutputStream stderr) {
		String text = stderr.toString(StandardCharsets.UTF_8).trim();

		return text.isEmpty() ? "<empty>" : text;
	}

	private void joinQuietly(Thread thread) {
		try {
			thread.join(TERMINATION_GRACE_PERIOD_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void closeQuietly(AutoCloseable closeable) {
		try {
			closeable.close();
		} catch (Exception ignored) {
			// cleanup hides the original failure
		}
	}
}
