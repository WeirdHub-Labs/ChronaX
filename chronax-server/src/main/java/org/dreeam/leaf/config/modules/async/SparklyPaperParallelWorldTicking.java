package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.ChronaXRootConfig;
import org.dreeam.leaf.config.ChronaXRuntimeProfile;
import org.dreeam.leaf.config.EnumConfigCategory;
import org.dreeam.leaf.config.LeafConfig;
import org.dreeam.leaf.config.annotations.Experimental;

public class SparklyPaperParallelWorldTicking extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.ASYNC.getBaseKeyName() + ".parallel-world-ticking";
    }

    @Experimental
    public static boolean enabled = false;
    public static int threads = 0;
    public static boolean logContainerCreationStacktraces = false;
    public static boolean disableHardThrow = false;
    @Deprecated
    public static Boolean runAsyncTasksSync;
    // STRICT, BUFFERED, DISABLED
    public static String asyncUnsafeReadHandling = "BUFFERED";
    public static int maxBufferedReadRequests = 32768;
    public static int maxReadRequestsPerTick = 8192;

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(getBasePath(), """
                **Experimental feature**
                Enables parallel world ticking to improve performance on multi-core systems.""",
            """
                **实验性功能**
                启用并行世界处理以提高多核 CPU 使用率.""");

        final int defaultThreads = ChronaXRuntimeProfile.defaultParallelWorldTickingThreads();
        enabled = config.getBoolean(getBasePath() + ".enabled", ChronaXRuntimeProfile.defaultParallelWorldTickingEnabled());
        threads = config.getInt(getBasePath() + ".threads", defaultThreads);
        maxBufferedReadRequests = config.getInt(getBasePath() + ".max-buffered-read-requests", ChronaXRuntimeProfile.defaultPwtMaxBufferedReadRequests());
        maxReadRequestsPerTick = config.getInt(getBasePath() + ".max-read-requests-per-tick", ChronaXRuntimeProfile.defaultPwtMaxReadRequestsPerTick());

        final Boolean rootEnabled = ChronaXRootConfig.getBoolean("leaf-overrides.async.parallel-world-ticking.enabled");
        if (rootEnabled != null) {
            enabled = rootEnabled;
        }
        final Integer rootThreads = ChronaXRootConfig.getInt("leaf-overrides.async.parallel-world-ticking.threads");
        if (rootThreads != null) {
            threads = rootThreads;
        }
        final Integer rootMaxBufferedReadRequests = ChronaXRootConfig.getInt("leaf-overrides.async.parallel-world-ticking.max-buffered-read-requests");
        if (rootMaxBufferedReadRequests != null) {
            maxBufferedReadRequests = rootMaxBufferedReadRequests;
        }
        final Integer rootMaxReadRequestsPerTick = ChronaXRootConfig.getInt("leaf-overrides.async.parallel-world-ticking.max-read-requests-per-tick");
        if (rootMaxReadRequestsPerTick != null) {
            maxReadRequestsPerTick = rootMaxReadRequestsPerTick;
        }

        if (enabled) {
            if (threads <= 0) threads = Math.max(defaultThreads, 1);
        } else {
            threads = 0;
        }
        if (maxBufferedReadRequests <= 0) {
            maxBufferedReadRequests = ChronaXRuntimeProfile.defaultPwtMaxBufferedReadRequests();
        }
        if (maxReadRequestsPerTick <= 0) {
            maxReadRequestsPerTick = ChronaXRuntimeProfile.defaultPwtMaxReadRequestsPerTick();
        }

        logContainerCreationStacktraces = config.getBoolean(getBasePath() + ".log-container-creation-stacktraces", logContainerCreationStacktraces);
        logContainerCreationStacktraces = enabled && logContainerCreationStacktraces;
        disableHardThrow = config.getBoolean(getBasePath() + ".disable-hard-throw", disableHardThrow);
        disableHardThrow = enabled && disableHardThrow;
        final Boolean rootLogContainerCreationStacktraces = ChronaXRootConfig.getBoolean("leaf-overrides.async.parallel-world-ticking.log-container-creation-stacktraces");
        if (rootLogContainerCreationStacktraces != null) {
            logContainerCreationStacktraces = enabled && rootLogContainerCreationStacktraces;
        }
        final Boolean rootDisableHardThrow = ChronaXRootConfig.getBoolean("leaf-overrides.async.parallel-world-ticking.disable-hard-throw");
        if (rootDisableHardThrow != null) {
            disableHardThrow = enabled && rootDisableHardThrow;
        }
        asyncUnsafeReadHandling = config.getString(getBasePath() + ".async-unsafe-read-handling", asyncUnsafeReadHandling).toUpperCase();
        final String rootAsyncUnsafeReadHandlingAfterConfig = ChronaXRootConfig.getString("leaf-overrides.async.parallel-world-ticking.async-unsafe-read-handling");
        if (rootAsyncUnsafeReadHandlingAfterConfig != null) {
            asyncUnsafeReadHandling = rootAsyncUnsafeReadHandlingAfterConfig.toUpperCase();
        }

        if (!asyncUnsafeReadHandling.equals("STRICT") && !asyncUnsafeReadHandling.equals("BUFFERED") && !asyncUnsafeReadHandling.equals("DISABLED")) {
            LeafConfig.LOGGER.warn("Invalid value for {}.async-unsafe-read-handling: {}, fallback to STRICT.", getBasePath(), asyncUnsafeReadHandling);
            asyncUnsafeReadHandling = "STRICT";
        }
        if (!enabled) {
            asyncUnsafeReadHandling = "DISABLED";
        }

        // Transfer old config
        runAsyncTasksSync = config.getBoolean(getBasePath() + ".run-async-tasks-sync");
        if (runAsyncTasksSync != null && runAsyncTasksSync) {
            LeafConfig.LOGGER.warn("The setting '{}.run-async-tasks-sync' is deprecated, removed automatically. Use 'async-unsafe-read-handling: BUFFERED' for buffered reads instead.", getBasePath());
        }

        if (enabled) {
            LeafConfig.LOGGER.info("Using {} threads for Parallel World Ticking", threads);
        }
    }
}
