package org.mesdag.particlestorm;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PSClientConfigs {
    private static final String DEBUG = "debug";
    private static final String SHOW_EMITTER_OUTLINE = "showEmitterOutline";

    public static boolean debug = false;
    public static boolean showEmitterOutline = true;

    private PSClientConfigs() {
    }

    public static void onLoad() {
        Properties properties = new Properties();
        properties.setProperty(DEBUG, "false");
        properties.setProperty(SHOW_EMITTER_OUTLINE, "true");

        // NeoForge native config directory; keeps the existing user-visible properties-file format.
        Path path = FMLPaths.CONFIGDIR.get().resolve(ParticleStorm.MODID + ".properties");
        if (Files.notExists(path)) {
            writeDefaults(path, properties);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            ParticleStorm.LOGGER.warn("Failed to load ParticleStorm config '{}', using defaults", path, exception);
        }

        debug = Boolean.parseBoolean(properties.getProperty(DEBUG, "false"));
        showEmitterOutline = Boolean.parseBoolean(properties.getProperty(SHOW_EMITTER_OUTLINE, "true"));
    }

    private static void writeDefaults(Path path, Properties properties) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "ParticleStorm configuration");
            }
        } catch (IOException exception) {
            ParticleStorm.LOGGER.warn("Failed to create ParticleStorm config '{}'", path, exception);
        }
    }
}
