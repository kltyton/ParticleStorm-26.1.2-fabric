package org.mesdag.particlestorm.api.geckolib;

import com.geckolib.animatable.GeoReplacedEntity;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface GeoWithCurrentEntity extends GeoReplacedEntity {
    @Nullable Entity getCurrentEntity();

    void setCurrentEntity(@Nullable Entity entity);
}
