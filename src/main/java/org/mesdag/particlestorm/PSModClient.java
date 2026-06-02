package org.mesdag.particlestorm;

import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

public final class PSModClient {
    private PSModClient() {
    }

    public static void registerCustomParticleType(RegisterCustomParticleTypeEvent event) {
        event.register(ParticleStorm.MOLANG, (emitter, particlePreset, level, x, y, z) ->
                new MolangParticleInstance(particlePreset, level, x, y, z, level.getRandom())
        );
    }
}
