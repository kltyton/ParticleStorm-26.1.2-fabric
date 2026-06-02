package org.mesdag.particlestorm;

public final class PSClientConfigs {
    public static boolean showEmitterOutline = true;

    private PSClientConfigs() {
    }

    public static void onLoad() {
        showEmitterOutline = true;
    }
}
