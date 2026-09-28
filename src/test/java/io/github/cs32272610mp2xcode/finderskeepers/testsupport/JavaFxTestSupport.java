package io.github.cs32272610mp2xcode.finderskeepers.testsupport;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.application.Platform;

/** Shared lifecycle and thread helpers for JavaFX tests. */
public final class JavaFxTestSupport {
    private static final int FX_TIMEOUT_SECONDS = 10;

    private static boolean started;

    private JavaFxTestSupport() {
    }

    /** Starts the JavaFX toolkit once for all tests in the current worker. */
    public static synchronized void startToolkit() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch startup = new CountDownLatch(1);
        try {
            Platform.startup(startup::countDown);
        } catch (IllegalStateException exception) {
            startup.countDown();
        }
        if (!startup.await(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("JavaFX toolkit did not start in time");
        }
        Platform.setImplicitExit(false);
        started = true;
    }

    /** Runs a task on the JavaFX application thread and returns its result. */
    public static <T> T runOnFxThread(Callable<T> task)
            throws InterruptedException, ExecutionException, TimeoutException {
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        return future.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
