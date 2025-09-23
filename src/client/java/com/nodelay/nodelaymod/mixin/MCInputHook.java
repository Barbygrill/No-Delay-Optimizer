package com.nodelay.nodelaymod.mixin;

import com.nodelay.nodelaymod.ClientPredictionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MCInputHook {

    @Inject(method = "handleInputEvents", at = @At("TAIL"))
    private void ndo$afterHandleInput(CallbackInfo ci) {
        if (!ClientPredictionHandler.queued) return;
        ClientPredictionHandler.queued = false;
        Hand hand = ClientPredictionHandler.queuedHand;
        ClientPredictionHandler.queuedHand = null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.interactionManager == null || hand == null) return;
        PlayerEntity player = client.player;
        ItemStack stack = player.getStackInHand(hand);
        client.interactionManager.interactItem(player, hand);
        ClientPredictionHandler.zeroHudCooldown(player, stack);
    }
}
