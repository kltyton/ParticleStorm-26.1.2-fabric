package org.mesdag.particlestorm.api;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.PSModClient;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;

import java.util.HashMap;
import java.util.Map;

public class RegisterCustomParticleTypeEvent {
    private static final Map<ParticleType<?>, Provider<?>> PROVIDERS = new HashMap<>();

    public void register(ParticleType<?> type, Provider<?> provider) {
        PROVIDERS.put(type, provider);
    }

    public static void registerDefaults() {
        PROVIDERS.clear();
        PSModClient.registerCustomParticleType(new RegisterCustomParticleTypeEvent());
    }

    @SuppressWarnings("unchecked")
    public static <V extends Particle & IMolangParticleInstance> V createParticle(ParticleEmitter emitter) {
        Provider<?> provider = PROVIDERS.get(emitter.getPreset().type);
        if (provider == null) {
            throw new NullPointerException("Provider from '" + BuiltInRegistries.PARTICLE_TYPE.getKey(emitter.getPreset().type) + "' is not registered");
        }

        return (V) provider.create(emitter, PSGameClient.LOADER.id2Particle().get(emitter.particleId), (ClientLevel) emitter.level, emitter.getX(), emitter.getY(), emitter.getZ());
    }

    @FunctionalInterface
    public interface Provider<V extends Particle & IMolangParticleInstance> {
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z);
    }
}
