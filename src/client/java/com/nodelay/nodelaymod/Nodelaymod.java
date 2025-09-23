package com.nodelay.nodelaymod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class Nodelaymod implements ClientModInitializer {
    private static final String CATEGORY = "key.nodelaymod.category";
    private static KeyBinding toggleNdoKey;

    @Override
    public void onInitializeClient() {
        toggleNdoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nodelaymod.toggle_ndo",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(ClientPredictionHandler::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleNdoKey.wasPressed()) {
                boolean newState = !ClientPredictionHandler.enabled;
                ClientPredictionHandler.enabled = newState;
                if (client.player != null) {
                    Text message = Text.literal("NDO " + (newState ? "ON" : "OFF"))
                            .formatted(newState ? Formatting.GREEN : Formatting.RED);
                    client.player.sendMessage(message, false);
                }
            }
        });
    }
}
