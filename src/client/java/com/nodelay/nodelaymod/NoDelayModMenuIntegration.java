package com.nodelay.nodelaymod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;


public class NoDelayModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.literal("No Delay Mod"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            builder.getOrCreateCategory(Text.literal("NDO"))
                    .addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable NDO"), ClientPredictionHandler.enabled)
                            .setDefaultValue(true)
                            .setSaveConsumer(newValue -> ClientPredictionHandler.enabled = newValue)
                            .build());
            return builder.build();
        };
    }
}