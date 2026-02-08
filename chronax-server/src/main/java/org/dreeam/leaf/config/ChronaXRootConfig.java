package org.dreeam.leaf.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

public final class ChronaXRootConfig {
    private static final Logger LOGGER = LogManager.getLogger("ChronaXConfig");
    private static final File FILE = new File("chronax.yml");
    private static volatile YamlConfiguration config;
    private static final String DEFAULT_CONTENT = """
            # ChronaX root configuration
            # Performance-first default preset for ChronaX.
            # Edit values as needed for your hardware/plugin mix.
            config-version: 1

            chunk-system:
              gen-parallelism: on
              io-threads: 4
              worker-threads: 12

            chunk-loading:
              player-max-chunk-send-rate: 180
              player-max-chunk-load-rate: 220
              player-max-chunk-generate-rate: 70
              player-max-concurrent-chunk-loads: 12
              player-max-concurrent-chunk-generates: 6

            leaf-overrides:
              async:
                async-chunk-send: true
                async-mob-spawning: true
                async-playerdata-save: true
                async-pathfinding:
                  enabled: true
                  max-threads: -2
                  keepalive: 120
                  queue-size: 4096
                  reject-policy: CALLER_RUNS
                async-entity-tracker:
                  enabled: true
                  threads: 4
                parallel-world-ticking:
                  enabled: true
                  threads: 8
                  log-container-creation-stacktraces: false
                  disable-hard-throw: false
                  async-unsafe-read-handling: BUFFERED
              performance:
                throttle-hopper-when-full:
                  enabled: true
                  skip-ticks: 12
            """;

    private ChronaXRootConfig() {
    }

    public static void reload() {
        ensureExists();
        config = YamlConfiguration.loadConfiguration(FILE);
    }

    public static @Nullable Boolean getBoolean(final String path) {
        final YamlConfiguration yaml = ensureLoaded();
        final Object raw = yaml.get(path);
        if (raw == null) {
            return null;
        }
        if (raw instanceof ConfigurationSection) {
            // Allow section-style keys for fallback chains without warning noise.
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof String str) {
            final String normalized = str.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized)) {
                return false;
            }
        }
        LOGGER.warn("Invalid boolean in chronax.yml at '{}': '{}'", path, raw);
        return null;
    }

    public static @Nullable Integer getInt(final String path) {
        final YamlConfiguration yaml = ensureLoaded();
        final Object raw = yaml.get(path);
        if (raw == null) {
            return null;
        }
        if (raw instanceof ConfigurationSection) {
            // Allow section-style keys for fallback chains without warning noise.
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (final NumberFormatException ignored) {
                // fall through
            }
        }
        LOGGER.warn("Invalid integer in chronax.yml at '{}': '{}'", path, raw);
        return null;
    }

    public static @Nullable String getString(final String path) {
        final YamlConfiguration yaml = ensureLoaded();
        final String value = yaml.getString(path);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static @Nullable Boolean getFirstBoolean(final String... paths) {
        for (final String path : paths) {
            final Boolean value = getBoolean(path);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static @Nullable Integer getFirstInt(final String... paths) {
        for (final String path : paths) {
            final Integer value = getInt(path);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static @Nullable String getFirstString(final String... paths) {
        for (final String path : paths) {
            final String value = getString(path);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static YamlConfiguration ensureLoaded() {
        YamlConfiguration yaml = config;
        if (yaml == null) {
            reload();
            yaml = config;
        }
        return yaml;
    }

    private static void ensureExists() {
        if (FILE.exists()) {
            return;
        }
        try {
            Files.writeString(FILE.toPath(), DEFAULT_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            LOGGER.info("Created ChronaX root config '{}'", FILE.getPath());
        } catch (final IOException ex) {
            LOGGER.warn("Failed to create ChronaX root config '{}': {}", FILE.getPath(), ex.getMessage());
        }
    }
}
