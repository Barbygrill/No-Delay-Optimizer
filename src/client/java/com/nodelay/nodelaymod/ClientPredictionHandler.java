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
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public final class ClientPredictionHandler {
    public static boolean enabled = true;

    private static final long XBOW_RETRY_MS = 200L;
    private static final int XBOW_MAX_RETRIES = 1;

    private static final long[] PEARL_RETRY_OFFSETS_MS = new long[]{120L, 200L};
    private static final int PEARL_MAX_RETRIES = 2;

    private static boolean lastUseDown = false;

    private enum XbowState { IDLE, PRIMING, PENDING_FIRE }
    private static XbowState xbowState = XbowState.IDLE;
    private static Hand xbowHand = Hand.MAIN_HAND;
    private static int xbowRetries = 0;
    private static long lastXbowAttemptMs = 0L;

    private static Hand pearlHand = null;
    private static long pearlPressMs = 0L;
    private static int pearlRetries = 0;
    private static int pearlStartCount = -1;

    public static volatile boolean queued = false;
    public static volatile Hand queuedHand = null;

    private ClientPredictionHandler() {}

    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) return;
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager im = client.interactionManager;
        if (im == null) return;

        KeyBinding useKey = client.options.useKey;
        boolean useDown = useKey.isPressed();

        if (!enabled) {
            if (!useDown) resetAll();
            lastUseDown = useDown;
            return;
        }

        if (useDown && !lastUseDown) {
            xbowRetries = 0;
            pearlRetries = 0;
            pearlStartCount = -1;
            ItemStack main = player.getMainHandStack();
            ItemStack off = player.getOffHandStack();

            if (isCrossbow(main)) {
                xbowHand = Hand.MAIN_HAND;
                beginCrossbowPress(player, main, xbowHand);
            } else if (isCrossbow(off)) {
                xbowHand = Hand.OFF_HAND;
                beginCrossbowPress(player, off, xbowHand);
            } else {
                Hand h = handWithPearl(player);
                if (h != null) {
                    pearlHand = h;
                    queueInteract(pearlHand);
                    pearlPressMs = now();
                    pearlRetries = 0;
                    pearlStartCount = player.getStackInHand(pearlHand).getCount();
                } else {
                    pearlHand = null;
                }
            }
        }

        if (useDown) {
            if (xbowState == XbowState.PRIMING) {
                ItemStack stack = player.getStackInHand(xbowHand);
                if (isCrossbow(stack) && CrossbowItem.isCharged(stack)) {
                    xbowState = XbowState.PENDING_FIRE;
                    queueInteract(xbowHand);
                    lastXbowAttemptMs = now();
                } else if (elapsed(lastXbowAttemptMs) > XBOW_RETRY_MS && xbowRetries < XBOW_MAX_RETRIES) {
                    queueInteract(xbowHand);
                    xbowRetries++;
                    lastXbowAttemptMs = now();
                }
            }

            if (pearlHand != null && hasPearl(player, pearlHand) && !pearlAcceptedByServer(player, pearlHand)) {
                if (pearlRetries < PEARL_MAX_RETRIES) {
                    long dt = now() - pearlPressMs;
                    long nextOffset = PEARL_RETRY_OFFSETS_MS[pearlRetries];
                    if (dt >= nextOffset) {
                        queueInteract(pearlHand);
                        pearlRetries++;
                    }
                }
            }
        }

        if (!useDown && lastUseDown) {
            xbowState = XbowState.IDLE;
            xbowRetries = 0;
            pearlHand = null;
            pearlRetries = 0;
            pearlStartCount = -1;
        }

        lastUseDown = useDown;
    }

    private static void beginCrossbowPress(PlayerEntity player, ItemStack stack, Hand hand) {
        if (!isCrossbow(stack)) {
            xbowState = XbowState.IDLE;
            return;
        }
        if (CrossbowItem.isCharged(stack)) {
            xbowState = XbowState.PENDING_FIRE;
            queueInteract(hand);
            lastXbowAttemptMs = now();
        } else {
            xbowState = XbowState.PRIMING;
            queueInteract(hand);
            lastXbowAttemptMs = now();
        }
    }

    private static boolean hasPearl(PlayerEntity player, Hand hand) {
        return isPearl(player.getStackInHand(hand));
    }

    private static boolean pearlAcceptedByServer(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        boolean cd = player.getItemCooldownManager().isCoolingDown(item);
        boolean countDrop = pearlStartCount >= 0 && stack.getCount() < pearlStartCount && pearlStartCount != 0;
        return cd || countDrop;
    }

    private static void queueInteract(Hand hand) {
        if (!queued) {
            queued = true;
            queuedHand = hand;
        }
    }

    private static boolean isCrossbow(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isPearl(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EnderPearlItem;
    }

    private static Hand handWithPearl(PlayerEntity player) {
        if (isPearl(player.getMainHandStack())) return Hand.MAIN_HAND;
        if (isPearl(player.getOffHandStack())) return Hand.OFF_HAND;
        return null;
    }

    public static void zeroHudCooldown(PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        Object mgr = player.getItemCooldownManager();
        Class<?> cls = mgr.getClass();
        try {
            Method m = cls.getMethod("set", ItemStack.class, int.class);
            m.invoke(mgr, stack, 0);
            return;
        } catch (Exception ignored) {}
        try {
            Method m = cls.getMethod("set", Item.class, int.class);
            m.invoke(mgr, stack.getItem(), 0);
            return;
        } catch (Exception ignored) {}
        try {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            Method m = cls.getMethod("set", Identifier.class, int.class);
            m.invoke(mgr, id, 0);
        } catch (Exception ignored) {}
    }

    private static void resetAll() {
        xbowState = XbowState.IDLE;
        xbowRetries = 0;
        pearlHand = null;
        pearlRetries = 0;
        pearlStartCount = -1;
        queued = false;
        queuedHand = null;
    }

    private static long now() { return System.currentTimeMillis(); }
    private static long elapsed(long since) { return now() - since; }
}
