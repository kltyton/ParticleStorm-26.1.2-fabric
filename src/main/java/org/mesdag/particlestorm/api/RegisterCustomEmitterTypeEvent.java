package org.mesdag.particlestorm.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class RegisterCustomEmitterTypeEvent {
    public static final String TYPE_KEY = "type";
    private static Map<Identifier, BiFunction<Level, CompoundTag, ? extends ParticleEmitter>> map = new HashMap<>();

    private RegisterCustomEmitterTypeEvent() {}

    public static void postEvent() {
        map = new HashMap<>();
    }

    public <E extends ParticleEmitter> void register(Identifier id, BiFunction<Level, CompoundTag, E> factory) {
        map.put(id, factory);
    }

    public static ParticleEmitter create(Level level, CompoundTag tag) {
        if (tag.contains(TYPE_KEY)) {
            Identifier id = Identifier.tryParse(tag.getString(TYPE_KEY).orElse(""));
            BiFunction<Level, CompoundTag, ? extends ParticleEmitter> factory = map.get(id);
            if (factory != null) {
                return factory.apply(level, tag);
            }
        }
        return new ParticleEmitter(level, tag);
    }
}
