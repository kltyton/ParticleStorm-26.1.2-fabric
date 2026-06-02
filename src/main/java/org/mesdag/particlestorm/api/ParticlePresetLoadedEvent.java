package org.mesdag.particlestorm.api;

import org.mesdag.particlestorm.particle.ParticlePreset;

public class ParticlePresetLoadedEvent {
    private final ParticlePreset preset;

    public ParticlePresetLoadedEvent(ParticlePreset preset) {
        this.preset = preset;
    }

    public ParticlePreset getPreset() {
        return preset;
    }
}
