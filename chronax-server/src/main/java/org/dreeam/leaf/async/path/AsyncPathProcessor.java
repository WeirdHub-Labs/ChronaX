package org.dreeam.leaf.async.path;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dreeam.leaf.config.modules.async.AsyncPathfinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * used to handle the scheduling of async path processing
 */
public class AsyncPathProcessor {

    private static final String THREAD_PREFIX = "ChronaX Async Pathfinding";
    private static final Logger LOGGER = LogManager.getLogger(THREAD_PREFIX);
    private static final long SCALING_CHECK_INTERVAL_MILLIS = 5000L;
    private static long lastWarnMillis = System.currentTimeMillis();
    private static volatile long lastScalingCheckMillis;
    private static volatile int activeThreads;
    public static ThreadPoolExecutor PATH_PROCESSING_EXECUTOR = null;

    public static void init() {
        if (PATH_PROCESSING_EXECUTOR == null) {
            PATH_PROCESSING_EXECUTOR = new ThreadPoolExecutor(
                getCorePoolSize(),
                getMaxPoolSize(),
                getKeepAliveTime(), TimeUnit.SECONDS,
                getQueueImpl(),
                getThreadFactory(),
                getRejectedPolicy()
            );
            if (PATH_PROCESSING_EXECUTOR.getKeepAliveTime(TimeUnit.NANOSECONDS) > 0L) {
                PATH_PROCESSING_EXECUTOR.allowCoreThreadTimeOut(true);
            }
            activeThreads = Math.max(1, AsyncPathfinding.asyncPathfindingMaxThreads);
        } else {
            // Temp no-op
            //throw new IllegalStateException();
        }
    }

    protected static CompletableFuture<Void> queue(@NotNull AsyncPath path) {
        maybeScaleThreadPool();
        return CompletableFuture.runAsync(path::process, PATH_PROCESSING_EXECUTOR)
            .orTimeout(60L, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException e) {
                    LOGGER.warn("Async Pathfinding process timed out", e);
                } else LOGGER.warn("Error occurred while processing async path", throwable);
                return null;
            });
    }

    private static void maybeScaleThreadPool() {
        final ThreadPoolExecutor executor = PATH_PROCESSING_EXECUTOR;
        if (executor == null || !AsyncPathfinding.adaptiveThreadScaling) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - lastScalingCheckMillis < SCALING_CHECK_INTERVAL_MILLIS) {
            return;
        }
        lastScalingCheckMillis = now;

        final int configuredMaxThreads = Math.max(1, AsyncPathfinding.asyncPathfindingMaxThreads);
        final int queueCapacity = Math.max(AsyncPathfinding.asyncPathfindingQueueSize, 1);
        final int queueSize = executor.getQueue().size();
        double mspt = 0.0D;
        if (MinecraftServer.getServer() != null) {
            mspt = MinecraftServer.getServer().getAverageTickTimeNanos() / 1_000_000.0D;
        }

        int targetThreads = configuredMaxThreads;
        if (mspt >= 55.0D) {
            targetThreads = Math.max(1, configuredMaxThreads / 2);
        } else if (mspt >= 45.0D) {
            targetThreads = Math.max(1, (configuredMaxThreads * 3) / 4);
        }

        if (queueSize > (queueCapacity * 3) / 4) {
            targetThreads = configuredMaxThreads;
        } else if (queueSize < Math.max(queueCapacity / 8, 1)) {
            targetThreads = Math.max(1, Math.min(targetThreads, configuredMaxThreads / 2));
        }

        targetThreads = Math.max(1, Math.min(targetThreads, configuredMaxThreads));
        if (targetThreads == activeThreads) {
            return;
        }

        if (targetThreads > activeThreads) {
            executor.setMaximumPoolSize(targetThreads);
            executor.setCorePoolSize(targetThreads);
        } else {
            executor.setCorePoolSize(targetThreads);
            executor.setMaximumPoolSize(targetThreads);
        }
        activeThreads = targetThreads;
    }

    /**
     * takes a possibly unprocessed path, and waits until it is completed
     * the consumer will be immediately invoked if the path is already processed
     * the consumer will always be called on the main thread
     *
     * @param path            a path to wait on
     * @param afterProcessing a consumer to be called
     */
    public static void awaitProcessing(@Nullable Path path, Consumer<@Nullable Path> afterProcessing) {
        if (path != null && !path.isProcessed() && path instanceof AsyncPath asyncPath) {
            asyncPath.schedulePostProcessing(() -> afterProcessing.accept(path)); // Reduce double lambda allocation
        } else {
            afterProcessing.accept(path);
        }
    }

    private static int getCorePoolSize() {
        // Keep core threads aligned with configured max to preserve real parallelism.
        return Math.max(1, AsyncPathfinding.asyncPathfindingMaxThreads);
    }

    private static int getMaxPoolSize() {
        return Math.max(1, AsyncPathfinding.asyncPathfindingMaxThreads);
    }

    private static long getKeepAliveTime() {
        return AsyncPathfinding.asyncPathfindingKeepalive;
    }

    private static BlockingQueue<Runnable> getQueueImpl() {
        final int queueCapacity = AsyncPathfinding.asyncPathfindingQueueSize;

        return new LinkedBlockingQueue<>(queueCapacity);
    }

    private static @NotNull ThreadFactory getThreadFactory() {
        return new ThreadFactoryBuilder()
            .setNameFormat(THREAD_PREFIX + " Thread - %d")
            .setPriority(Thread.NORM_PRIORITY - 2)
            .setUncaughtExceptionHandler(Util::onThreadException)
            .build();
    }

    private static @NotNull RejectedExecutionHandler getRejectedPolicy() {
        return (Runnable rejectedTask, ThreadPoolExecutor executor) -> {
            final BlockingQueue<Runnable> workQueue = executor.getQueue();
            if (!executor.isShutdown()) {
                switch (AsyncPathfinding.asyncPathfindingRejectPolicy) {
                    case FLUSH_ALL -> {
                        Runnable pendingTask;
                        while ((pendingTask = workQueue.poll()) != null) {
                            pendingTask.run();
                        }
                        rejectedTask.run();
                    }
                    case CALLER_RUNS -> rejectedTask.run();
                }
            }

            if (System.currentTimeMillis() - lastWarnMillis > 30000L) {
                LOGGER.warn("Async pathfinding processor is busy! Pathfinding tasks will be treated as policy defined in config. Increasing max-threads in ChronaX config may help.");
                lastWarnMillis = System.currentTimeMillis();
            }
        };
    }
}
