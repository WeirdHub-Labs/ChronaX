package org.dreeam.leaf.config;

import java.util.Locale;

public final class ChronaXRuntimeProfile {
    private static final String COMPATIBILITY_FIRST = "compatibility-first";
    private static final String BALANCED = "balanced";
    private static final String MAX_THROUGHPUT = "max-throughput";

    private ChronaXRuntimeProfile() {
    }

    public static String profile() {
        final String configured = ChronaXRootConfig.getString("runtime-profile");
        return normalizeProfile(configured);
    }

    public static boolean isCompatibilityFirst() {
        return COMPATIBILITY_FIRST.equals(profile());
    }

    public static boolean isBalanced() {
        return BALANCED.equals(profile());
    }

    public static boolean isMaxThroughput() {
        return MAX_THROUGHPUT.equals(profile());
    }

    public static int threadBudget() {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final Integer configuredReserve = ChronaXRootConfig.getInt("thread-budget.reserve-cpu-threads");
        final Integer configuredHardCap = ChronaXRootConfig.getInt("thread-budget.hard-cap");

        final int reserve = configuredReserve != null ? Math.max(configuredReserve, 0) : defaultReserve();
        final int hardCap = configuredHardCap != null ? Math.max(configuredHardCap, 1) : availableProcessors;
        return Math.max(1, Math.min(availableProcessors - reserve, hardCap));
    }

    public static boolean defaultAsyncChunkSendEnabled() {
        return !isCompatibilityFirst();
    }

    public static boolean defaultAsyncMobSpawningEnabled() {
        return isBalanced() || isMaxThroughput();
    }

    public static boolean defaultAsyncPlayerDataSaveEnabled() {
        return isMaxThroughput();
    }

    public static boolean defaultAsyncPathfindingEnabled() {
        return !isCompatibilityFirst();
    }

    public static boolean defaultAsyncEntityTrackerEnabled() {
        return isBalanced() || isMaxThroughput();
    }

    public static boolean defaultParallelWorldTickingEnabled() {
        return isMaxThroughput();
    }

    public static int defaultAsyncPathfindingThreads() {
        if (isCompatibilityFirst()) {
            return 0;
        }
        return sharedThreads(isBalanced() ? 30 : 35, 1, isBalanced() ? 4 : 8);
    }

    public static int defaultAsyncEntityTrackerThreads() {
        if (!defaultAsyncEntityTrackerEnabled()) {
            return 0;
        }
        return sharedThreads(isBalanced() ? 20 : 25, 1, isBalanced() ? 3 : 6);
    }

    public static int defaultParallelWorldTickingThreads() {
        if (!defaultParallelWorldTickingEnabled()) {
            return 0;
        }
        return sharedThreads(45, 2, 12);
    }

    public static int defaultPwtMaxBufferedReadRequests() {
        if (isCompatibilityFirst()) {
            return 8192;
        }
        if (isBalanced()) {
            return 24576;
        }
        return 65536;
    }

    public static int defaultPwtMaxReadRequestsPerTick() {
        if (isCompatibilityFirst()) {
            return 2048;
        }
        if (isBalanced()) {
            return 8192;
        }
        return 16384;
    }

    private static int defaultReserve() {
        if (isCompatibilityFirst()) {
            return 2;
        }
        if (isBalanced()) {
            return 1;
        }
        return 0;
    }

    private static int sharedThreads(final int percent, final int min, final int max) {
        final int budget = threadBudget();
        final int calculated = Math.max(min, (budget * percent) / 100);
        return Math.min(calculated, max);
    }

    private static String normalizeProfile(final String configured) {
        if (configured == null || configured.isBlank()) {
            return COMPATIBILITY_FIRST;
        }

        final String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(COMPATIBILITY_FIRST) || normalized.equals("compatibility") || normalized.equals("safe")) {
            return COMPATIBILITY_FIRST;
        }
        if (normalized.equals(BALANCED) || normalized.equals("default")) {
            return BALANCED;
        }
        if (normalized.equals(MAX_THROUGHPUT) || normalized.equals("performance-first") || normalized.equals("aggressive")) {
            return MAX_THROUGHPUT;
        }

        LeafConfig.LOGGER.warn("Invalid runtime-profile '{}', falling back to '{}'.", configured, COMPATIBILITY_FIRST);
        return COMPATIBILITY_FIRST;
    }
}
