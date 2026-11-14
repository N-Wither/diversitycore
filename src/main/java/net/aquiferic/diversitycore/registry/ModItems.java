package net.aquiferic.diversitycore.registry;

import static net.aquiferic.diversitycore.registry.ModRegistries.CONTENT;

import net.aquiferic.diversitycore.api.registry.ItemModelType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {
    public static final DeferredItem<Item> WRENCH = CONTENT.items().create("wrench")
        .maxStackSize(1)
        .displayName("en_us", "Wrench")
        .displayName("zh_cn", "扳手")
        .texture("diversitycore:item/wrench")
        .model(ItemModelType.HANDHELD)
        .tag("c:wrenches")
        .register();

    private ModItems() {
    }

    static void init() {
    }
}
