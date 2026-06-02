package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.loading.math.MolangQueries;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> {
    @Shadow
    protected @Nullable AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleKeyframeHandler;
    @Shadow
    @Final
    protected String name;

    @Unique
    private AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particlestorm$wrappedParticleHandler;

    @Inject(method = "extractControllerState", at = @At("HEAD"))
    private void particlestorm$ensureParticleHandler(T animatable, GeoRenderState renderState, AnimatableManager<T> manager, MolangQueries.Actor<T> actor, GeoModel<T> geoModel, CallbackInfoReturnable<ControllerState> cir) {
        particlestorm$wrapParticleHandler();
    }

    @Inject(method = "setParticleKeyframeHandler", at = @At("TAIL"))
    private void particlestorm$wrapCustomParticleHandler(AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleHandler, CallbackInfoReturnable<AnimationController<T>> cir) {
        particlestorm$wrapParticleHandler();
    }

    @Inject(method = "initializeNewAnimation", at = @At("HEAD"))
    private void particlestorm$removeEmitterOnNewAnimation(T animatable, GeoRenderState renderState, GeoModel<T> geoModel, double prevAnimSpeed, int prevTransitionTicks, CallbackInfo ci) {
        GeckoLibHelper.removeEmitters(renderState);
    }

    @Unique
    private void particlestorm$wrapParticleHandler() {
        if (particleKeyframeHandler == particlestorm$wrappedParticleHandler) {
            return;
        }

        AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> original = particleKeyframeHandler;
        particlestorm$wrappedParticleHandler = event -> {
            if (original != null) {
                original.handle(event);
            }
            GeckoLibHelper.processParticleEffect(event);
        };
        particleKeyframeHandler = particlestorm$wrappedParticleHandler;
    }
}
