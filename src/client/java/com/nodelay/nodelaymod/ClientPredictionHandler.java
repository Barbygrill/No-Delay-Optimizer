package com.nodelay.nodelaymod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;


@Environment(EnvType.CLIENT)
public final class ClientPredictionHandler {
    public static boolean enabled = true;
    private static boolean lastUseDown = false;
// cleannnn
    private static final long RETRY_MS = 90L;

    private static final long PEARL_COOLDOWN_MS = 150L;

    private static long lastPearlAttemptMs = 0L;

    private static long lastCrossbowAttemptMs = 0L;

    private static boolean primingCrossbow = false;

    private static Hand primedHand = Hand.MAIN_HAND;

    private ClientPredictionHandler() {

    }


    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        KeyBinding useKey = client.options.useKey;
        boolean useDown = useKey.isPressed();
        if (!enabled) {
            if (!useDown) {
                primingCrossbow = false;
            }
            lastUseDown = useDown;
            return;
        }

        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager im = client.interactionManager;
        if (im == null) {
            return;
        }


        if (useDown && !lastUseDown) {
            ItemStack main = player.getMainHandStack();
            ItemStack off = player.getOffHandStack();

            if (isCrossbow(main)) {
                primedHand = Hand.MAIN_HAND;
                startOrFireCrossbow(client, player, im, primedHand);
            } else if (isPearl(main)) {
                throwPearl(client, player, im, Hand.MAIN_HAND);
            } else if (isPearl(off)) {
                throwPearl(client, player, im, Hand.OFF_HAND);
            } else if (isCrossbow(off)) {
                primedHand = Hand.OFF_HAND;
                startOrFireCrossbow(client, player, im, primedHand);
            }
        }


        if (useDown && primingCrossbow) {
            ItemStack stack = player.getStackInHand(primedHand);

            if (isCrossbow(stack) && CrossbowItem.isCharged(stack)) {
                fireCrossbowNow(client, player, im, primedHand);
            } else if (elapsed(lastCrossbowAttemptMs) > RETRY_MS) {

                im.interactItem(player, primedHand);
                lastCrossbowAttemptMs = now();
            }
        }


        if (useDown) {
            Hand pearlHand = handWithPearl(player);
            if (pearlHand != null && elapsed(lastPearlAttemptMs) > RETRY_MS) {

                throwPearl(client, player, im, pearlHand);
            }
        }


        if (!useDown) {
            primingCrossbow = false;
        }
        lastUseDown = useDown;
    }


    private static void startOrFireCrossbow(MinecraftClient client, ClientPlayerEntity player,
                                            ClientPlayerInteractionManager im, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!isCrossbow(stack)) {
            return;
        }

        if (CrossbowItem.isCharged(stack)) {
            fireCrossbowNow(client, player, im, hand);
        } else {

            im.interactItem(player, hand);
            primingCrossbow = true;
            lastCrossbowAttemptMs = now();
        }
    }


    private static void fireCrossbowNow(MinecraftClient client, ClientPlayerEntity player,
                                        ClientPlayerInteractionManager im, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        player.swingHand(hand);

        zeroHudCooldown(player, stack.getItem());

        im.interactItem(player, hand);
        primingCrossbow = false;
        lastCrossbowAttemptMs = now();
    }


    private static void throwPearl(MinecraftClient client, ClientPlayerEntity player,
                                   ClientPlayerInteractionManager im, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        player.swingHand(hand);

        zeroHudCooldown(player, stack.getItem());

        im.interactItem(player, hand);
        lastPearlAttemptMs = now();
    }


    private static boolean isCrossbow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CrossbowItem;
    }


    private static boolean isPearl(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EnderPearlItem;
    }


    private static Hand handWithPearl(PlayerEntity player) {
        if (isPearl(player.getMainHandStack())) {
            return Hand.MAIN_HAND;
        }
        if (isPearl(player.getOffHandStack())) {
            return Hand.OFF_HAND;
        }
        return null;
    }


    private static void zeroHudCooldown(PlayerEntity player, Item item) {
        player.getItemCooldownManager().set(item, 0);
    }


    private static long now() {
        return System.currentTimeMillis();
    }


    private static long elapsed(long since) {
        return now() - since;
    }
}