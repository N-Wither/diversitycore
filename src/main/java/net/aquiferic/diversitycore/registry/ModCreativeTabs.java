package net.aquiferic.diversitycore.registry;

import net.aquiferic.diversitycore.DiversityCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DiversityCore.MODID);

    // Keep the original template registry ID while updating the display name and contents.
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("example_tab", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.diversitycore"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.WRENCH.get().getDefaultInstance())
            .displayItems((parameters, output) ->
                ModRegistries.CONTENT.creativeItems().forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {
    }

    static void init() {
    }
}
