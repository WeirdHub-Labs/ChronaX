package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.async.path.PathfindTaskRejectPolicy;
import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.ChronaXRootConfig;
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

        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        enabled = config.getBoolean(getBasePath() + ".enabled", enabled);
        asyncPathfindingMaxThreads = config.getInt(getBasePath() + ".max-threads", asyncPathfindingMaxThreads);
        asyncPathfindingKeepalive = config.getInt(getBasePath() + ".keepalive", asyncPathfindingKeepalive);
        asyncPathfindingQueueSize = config.getInt(getBasePath() + ".queue-size", asyncPathfindingQueueSize);
        final String defaultRejectPolicy = availableProcessors >= 12 && asyncPathfindingQueueSize < 512
            ? PathfindTaskRejectPolicy.FLUSH_ALL.toString()
            : PathfindTaskRejectPolicy.CALLER_RUNS.toString();
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
            asyncPathfindingMaxThreads = Math.max(availableProcessors + asyncPathfindingMaxThreads, 1);
        else if (asyncPathfindingMaxThreads == 0)
            asyncPathfindingMaxThreads = Math.max(availableProcessors / 4, 1);
        if (!enabled)
            asyncPathfindingMaxThreads = 0;
        else
            LeafConfig.LOGGER.info("Using {} threads for Async Pathfinding", asyncPathfindingMaxThreads);

        if (asyncPathfindingQueueSize <= 0)
            asyncPathfindingQueueSize = asyncPathfindingMaxThreads * 256;

        asyncPathfindingRejectPolicy = PathfindTaskRejectPolicy.fromString(rejectPolicy);

        if (enabled) {
            org.dreeam.leaf.async.path.AsyncPathProcessor.init();
        }
    }
}
