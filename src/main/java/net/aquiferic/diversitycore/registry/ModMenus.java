package net.aquiferic.diversitycore.registry;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.content.menu.CreativePowerBankMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DiversityCore.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<CreativePowerBankMenu>> CREATIVE_POWER_BANK =
        MENUS.register("creative_power_bank", () -> new MenuType<>(CreativePowerBankMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {
    }
}
