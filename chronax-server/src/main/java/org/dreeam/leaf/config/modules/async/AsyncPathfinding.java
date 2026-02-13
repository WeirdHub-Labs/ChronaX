package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.async.path.PathfindTaskRejectPolicy;
import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.ChronaXRootConfig;
import org.dreeam.leaf.config.ChronaXRuntimeProfile;
import org.dreeam.leaf.config.EnumConfigCategory;
import org.dreeam.leaf.config.LeafConfig;

public class AsyncPathfinding extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.ASYNC.getBaseKeyName() + ".async-pathfinding";
    }

    public static boolean enabled = true;
    public static int asyncPathfindingMaxThreads = 0;
    public static int asyncPathfindingKeepalive = 60;
    public static int asyncPathfindingQueueSize = 0;
    public static PathfindTaskRejectPolicy asyncPathfindingRejectPolicy = PathfindTaskRejectPolicy.FLUSH_ALL;
    private static boolean asyncPathfindingInitialized;

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(getBasePath() + ".reject-policy", """
                The policy to use when the queue is full and a new task is submitted.
                FLUSH_ALL: All pending tasks will be run on server thread.
                CALLER_RUNS: Newly submitted task will be run on server thread.""",
            """
                当队列满时, 新提交的任务将使用以下策略处理.
                FLUSH_ALL: 所有等待中的任务都将在主线程上运行.
                CALLER_RUNS: 新提交的任务将在主线程上运行."""
        );
        if (asyncPathfindingInitialized) {
            config.getConfigSection(getBasePath());
            return;
        }
        asyncPathfindingInitialized = true;

        final int threadBudget = ChronaXRuntimeProfile.threadBudget();
        final int defaultThreads = ChronaXRuntimeProfile.defaultAsyncPathfindingThreads();
        enabled = config.getBoolean(getBasePath() + ".enabled", ChronaXRuntimeProfile.defaultAsyncPathfindingEnabled());
        asyncPathfindingMaxThreads = config.getInt(getBasePath() + ".max-threads", defaultThreads);
        asyncPathfindingKeepalive = config.getInt(getBasePath() + ".keepalive", asyncPathfindingKeepalive);
        asyncPathfindingQueueSize = config.getInt(getBasePath() + ".queue-size", asyncPathfindingQueueSize);
        final String defaultRejectPolicy = PathfindTaskRejectPolicy.CALLER_RUNS.toString();
        String rejectPolicy = config.getString(getBasePath() + ".reject-policy", defaultRejectPolicy);

        final Boolean rootEnabled = ChronaXRootConfig.getBoolean("leaf-overrides.async.async-pathfinding.enabled");
        if (rootEnabled != null) {
            enabled = rootEnabled;
        }
        final Integer rootMaxThreads = ChronaXRootConfig.getInt("leaf-overrides.async.async-pathfinding.max-threads");
        if (rootMaxThreads != null) {
            asyncPathfindingMaxThreads = rootMaxThreads;
        }
        final Integer rootKeepalive = ChronaXRootConfig.getInt("leaf-overrides.async.async-pathfinding.keepalive");
        if (rootKeepalive != null) {
            asyncPathfindingKeepalive = rootKeepalive;
        }
        final Integer rootQueueSize = ChronaXRootConfig.getInt("leaf-overrides.async.async-pathfinding.queue-size");
        if (rootQueueSize != null) {
            asyncPathfindingQueueSize = rootQueueSize;
        }
        final String rootRejectPolicy = ChronaXRootConfig.getString("leaf-overrides.async.async-pathfinding.reject-policy");
        if (rootRejectPolicy != null) {
            rejectPolicy = rootRejectPolicy;
        }

        if (asyncPathfindingMaxThreads < 0)
            asyncPathfindingMaxThreads = Math.max(threadBudget + asyncPathfindingMaxThreads, 1);
        else if (asyncPathfindingMaxThreads == 0)
            asyncPathfindingMaxThreads = Math.max(defaultThreads, 1);
        if (!enabled)
            asyncPathfindingMaxThreads = 0;
        else {
            asyncPathfindingMaxThreads = Math.max(asyncPathfindingMaxThreads, 1);
            LeafConfig.LOGGER.info("Using {} threads for Async Pathfinding", asyncPathfindingMaxThreads);
        }

        if (asyncPathfindingQueueSize <= 0)
            asyncPathfindingQueueSize = Math.max(asyncPathfindingMaxThreads * (ChronaXRuntimeProfile.isCompatibilityFirst() ? 128 : 256), 256);

        asyncPathfindingRejectPolicy = PathfindTaskRejectPolicy.fromString(rejectPolicy);

        if (enabled) {
            org.dreeam.leaf.async.path.AsyncPathProcessor.init();
        }
    }
}
