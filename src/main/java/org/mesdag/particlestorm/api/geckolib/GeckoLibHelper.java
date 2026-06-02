package org.mesdag.particlestorm.api.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.IBlockEntity;
import org.mesdag.particlestorm.mixed.IEntity;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class GeckoLibHelper {
    private static final Map<AnimatableManager<?>, Map<String, BoundEmitter>> LOCATOR_EMITTERS = new WeakHashMap<>();

    private GeckoLibHelper() {
    }

    public static void processParticleEffect(KeyFrameEvent<? extends GeoAnimatable, ParticleKeyframeData> event) {
        try {
            ParticleContext context = createContext(event.animatable(), event.renderState());
            if (context == null) {
                return;
            }

            Identifier particleId = Identifier.parse(event.keyframeData().getEffect());
            String locator = event.keyframeData().getLocatorName();
            ParticleEmitter emitter = getOrCreateEmitter(event.renderState(), context, locator, particleId);
            if (emitter == null) {
                return;
            }

            if (locator == null || locator.isBlank()) {
                emitter.setPos(context.basePos());
            }
        } catch (RuntimeException exception) {
            PSDiagnostics.warnOnce("geckolib-particle-keyframe:" + event.keyframeData().getEffect(), "GeckoLib particle keyframe failed effect={} locator={} error={}",
                    event.keyframeData().getEffect(),
                    event.keyframeData().getLocatorName(),
                    exception.getMessage()
            );
        }
    }

    public static void attachLocatorListeners(GeoRenderState renderState, RenderPassInfo<?> renderPassInfo) {
        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        Map<String, BoundEmitter> emitters = LOCATOR_EMITTERS.get(manager);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, BoundEmitter>> iterator = emitters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BoundEmitter> entry = iterator.next();
            BoundEmitter bound = entry.getValue();
            ParticleEmitter emitter = PSGameClient.LOADER.getEmitter(bound.emitterId());
            if (emitter == null || emitter.isRemoved()) {
                iterator.remove();
                continue;
            }

            renderPassInfo.addLocatorPositionListener(entry.getKey(), (worldPos, modelPos, localPos) -> updateEmitterPosition(emitter, bound, worldPos, modelPos, localPos));
        }
    }

    public static void removeEmitters(GeoRenderState renderState) {
        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        Map<String, BoundEmitter> emitters = LOCATOR_EMITTERS.remove(manager);
        if (emitters != null) {
            for (BoundEmitter bound : emitters.values()) {
                PSGameClient.LOADER.removeEmitter(bound.emitterId(), false);
            }
        }
    }

    public static void setCurrentEntity(Object animatable, @Nullable Entity entity) {
        if (animatable instanceof GeoWithCurrentEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    private static @Nullable ParticleEmitter getOrCreateEmitter(GeoRenderState renderState, ParticleContext context, @Nullable String locator, Identifier particleId) {
        if (locator == null || locator.isBlank()) {
            ParticleEmitter emitter = new ParticleEmitter(context.level(), context.basePos(), particleId, MolangExp.EMPTY);
            PSGameClient.LOADER.addEmitter(emitter, false);
            attachContext(emitter, context);
            return emitter;
        }

        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return null;
        }

        Map<String, BoundEmitter> emitters = LOCATOR_EMITTERS.computeIfAbsent(manager, ignored -> new Object2ObjectOpenHashMap<>());
        BoundEmitter bound = emitters.get(locator);
        ParticleEmitter current = bound == null ? null : PSGameClient.LOADER.getEmitter(bound.emitterId());
        if (current != null && !current.isRemoved() && particleId.equals(current.particleId)) {
            return current;
        }

        if (current != null) {
            PSGameClient.LOADER.removeEmitter(current, false);
        }

        ParticleEmitter emitter = new ParticleEmitter(context.level(), context.basePos(), particleId, MolangExp.EMPTY);
        PSGameClient.LOADER.addEmitter(emitter, false);
        attachContext(emitter, context);
        emitter.parentMode = ParticleEmitter.ParentMode.LOCATOR;
        emitters.put(locator, new BoundEmitter(emitter.id, context.basePos(), context.entity(), context.blockEntity()));
        return emitter;
    }

    private static void attachContext(ParticleEmitter emitter, ParticleContext context) {
        if (context.entity() != null) {
            emitter.attachEntity(context.entity());
        } else if (context.blockEntity() != null) {
            emitter.attachedBlock = context.blockEntity();
        }
    }

    private static void updateEmitterPosition(ParticleEmitter emitter, BoundEmitter bound, @Nullable Vec3 worldPos, @Nullable Vec3 modelPos, @Nullable Vec3 localPos) {
        Vec3 target = worldPos != null ? worldPos : modelPos != null ? bound.basePos().add(modelPos.scale(1.0 / 16.0)) : localPos;
        if (target == null) {
            return;
        }

        emitter.setPos(target);
        if (bound.entity() != null) {
            Vec3 relative = target.subtract(bound.entity().position());
            emitter.parentPosition = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        } else if (bound.blockEntity() != null) {
            Vec3 base = bound.blockEntity().getBlockPos().getBottomCenter();
            Vec3 relative = target.subtract(base);
            emitter.parentPosition = new Vector3f((float) relative.x, (float) relative.y, (float) relative.z);
        }
    }

    private static @Nullable ParticleContext createContext(GeoAnimatable animatable, GeoRenderState renderState) {
        Entity entity = null;
        BlockEntity blockEntity = null;
        Level level;
        Vec3 basePos;
        VariableTable variableTable;

        if (animatable instanceof Entity entityAnimatable) {
            entity = entityAnimatable;
            level = entity.level();
            basePos = entity.position();
            variableTable = IEntity.of(entity).particlestorm$getVariableTable();
        } else if (animatable instanceof GeoWithCurrentEntity withCurrentEntity && withCurrentEntity.getCurrentEntity() != null) {
            entity = withCurrentEntity.getCurrentEntity();
            level = entity.level();
            basePos = entity.position();
            variableTable = IEntity.of(entity).particlestorm$getVariableTable();
        } else if (animatable instanceof BlockEntity blockEntityAnimatable && blockEntityAnimatable.getLevel() != null) {
            blockEntity = blockEntityAnimatable;
            level = blockEntity.getLevel();
            basePos = blockEntity.getBlockPos().getBottomCenter();
            variableTable = IBlockEntity.of(blockEntity).particlestorm$getVariableTable();
        } else if (Minecraft.getInstance().level != null) {
            level = Minecraft.getInstance().level;
            basePos = renderState.getOrDefaultGeckolibData(DataTickets.POSITION, Vec3.ZERO);
            variableTable = new VariableTable(null);
        } else {
            return null;
        }

        if (basePos == Vec3.ZERO) {
            PSDiagnostics.infoOnce("geckolib-no-render-position:" + animatable.getClass().getName(), "GeckoLib particle keyframe has no render position for {}", animatable);
        }
        return new ParticleContext(level, basePos, variableTable, entity, blockEntity);
    }

    private record ParticleContext(Level level, Vec3 basePos, VariableTable variableTable, @Nullable Entity entity, @Nullable BlockEntity blockEntity) {
    }

    private record BoundEmitter(int emitterId, Vec3 basePos, @Nullable Entity entity, @Nullable BlockEntity blockEntity) {
    }
}
