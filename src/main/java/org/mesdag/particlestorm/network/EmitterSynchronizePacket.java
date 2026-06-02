package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterSynchronizePacket(int id, CompoundTag tag) implements CustomPacketPayload {
    public static final Type<EmitterSynchronizePacket> TYPE = new Type<>(ParticleStorm.asResource("emitter_synchronize"));

    public static final StreamCodec<ByteBuf, EmitterSynchronizePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            ByteBufCodecs.COMPOUND_TAG, p -> p.tag,
            EmitterSynchronizePacket::new
    );
    public static final String KEY = "particlestorm:emitters";

    @Override
    public Type<EmitterSynchronizePacket> type() {
        return TYPE;
    }

    public static void handleClient(EmitterSynchronizePacket payload, ClientPlayNetworking.Context context) {
        Player player = context.player();
        PSGameClient.LOADER.loadEmitter(player.level(), payload.id, payload.tag);
    }

    public static void handleServer(EmitterSynchronizePacket payload, ServerPlayNetworking.Context context) {
        ParticleStorm.LOGGER.debug("Received client emitter sync request for id {}", payload.id);
    }

    public static void syncToServer(ParticleEmitter emitter) {
        CompoundTag tag = new CompoundTag();
        emitter.serialize(tag);
        if (ClientPlayNetworking.canSend(TYPE)) {
            ClientPlayNetworking.send(new EmitterSynchronizePacket(emitter.id, tag));
        }
    }

    public static void syncToClient(ServerPlayer player, int id) {
        ParticleStorm.LOGGER.debug("No server-side persisted emitters are available for {}", player.getGameProfile());
    }
}
