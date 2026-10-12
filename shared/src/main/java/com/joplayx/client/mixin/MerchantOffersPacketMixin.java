package com.joplayx.client.mixin;

import com.joplayx.client.util.OffersStore;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts merchant offers by injecting into the packet's handle() method.
 *
 * In 26.1.2, packet handling moved from named methods on ClientPacketListener
 * (like handleMerchantItems) into the packet class's own handle() method.
 * This matches the pattern used in Fabric API networking mixins which target
 * the packet class directly (e.g. ClientboundCustomPayloadPacketMixin).
 */
@Mixin(ClientboundMerchantOffersPacket.class)
public class MerchantOffersPacketMixin {

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = @At("TAIL"))
    private void onHandle(ClientGamePacketListener listener, CallbackInfo ci) {
        var packet = (ClientboundMerchantOffersPacket) (Object) this;
        OffersStore.set(packet.getContainerId(), packet.getOffers(), packet.getVillagerXp());
    }
}
