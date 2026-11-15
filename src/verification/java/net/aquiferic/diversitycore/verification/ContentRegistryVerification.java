package net.aquiferic.diversitycore.verification;

import java.util.concurrent.atomic.AtomicInteger;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.api.registry.ItemModelType;
import net.aquiferic.diversitycore.datagen.ModLootProvider;
import net.aquiferic.diversitycore.datagen.ModItemTagsProvider;
import net.aquiferic.diversitycore.datagen.ModBlockTagsProvider;
import net.aquiferic.diversitycore.util.QS;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Declaration contract checks executed by Gradle check without a third-party test framework. */
@Mod("diversitycore_verification")
public final class ContentRegistryVerification {
    private final ContentRegistry registered = new ContentRegistry("diversitycore_verification");
    private final AtomicInteger factoryCalls = new AtomicInteger();

    public ContentRegistryVerification(IEventBus bus) {
        PowerBankVerification.register(bus);
        WrenchVerification.register(bus);
        declarationsStayDeferred();
        conflictsDoNotPartiallyRegister();
        buildersAreSingleUse();
        invalidDeclarationsFailEarly();
        tagTargetsAreIndependent();
        itemTagVarargsExpandImmediateParents();
        invalidTagsFailEarly();
        noItemRejectsItemTags();
        quickTagConversionsAreTyped();
        registered.items().create("custom_item", properties -> {
            factoryCalls.incrementAndGet();
            return new CustomItem(properties);
        }).maxStackSize(16)
            .tag("diversitycore_verification:item_only", "c:verification/shared")
            .tag(TagKey.create(Registries.ITEM, Identifier.parse("c:verification/shared")))
            .register();
        registered.blocks().create("cube", CustomBlock::new)
            .tag("diversitycore_verification:block_only")
            .tag(TagKey.create(Registries.BLOCK, Identifier.parse("diversitycore_verification:block_only")))
            .tagItem("diversitycore_verification:item_only")
            .tagItem(TagKey.create(Registries.ITEM, Identifier.parse("diversitycore_verification:item_only")))
            .tagBoth("diversitycore_verification:both")
            .tagBoth("c:verification/shared")
            .register();
        registered.blocks().create("hidden").noItem().tag("diversitycore_verification:block_only").register();
        CropVerification.register(bus, registered);
        RecipeVerification.register(bus, registered);
        var unfinished = registered.items().create("late");
        registered.register(bus);
        expect(IllegalStateException.class, () -> registered.items().create("late"));
        expect(IllegalStateException.class, unfinished::register);
        expect(IllegalStateException.class, () -> registered.register(bus));
        bus.addListener(this::verifyRegisteredContent);
    }

    private void verifyRegisteredContent(GatherDataEvent.Server event) {
        PowerBankVerification.verifyStorage();
        EnergyBarVerification.verify();
        if (net.neoforged.fml.ModList.get().isLoaded("jade")) {
            try {
                EnergyBarModeVerification.verify();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Jade energy bar verification failed", exception);
            }
        }
        var item = registered.itemDefinitions().getFirst().holder().get();
        require(item instanceof CustomItem, "Custom item factory");
        require(factoryCalls.get() == 1, "Factory must execute exactly once");
        var cube = registered.blockDefinitions().getFirst();
        require(cube.holder().get() instanceof CustomBlock, "Custom block factory");
        require(cube.holder().get().asItem() == cube.item().get(), "BlockItem must be linked to its block");
        require(net.aquiferic.diversitycore.registry.ModRegistries.CONTENT.creativeItems().size() == 2, "Main content remains isolated from fixtures");
        event.addProvider(new ModLootProvider(event.getGenerator().getPackOutput(), registered, event.getReloadableLookupProvider()));
        event.addProvider(new ModItemTagsProvider(event.getGenerator().getPackOutput(), registered, event.getWorldLookupProvider()));
        event.addProvider(new ModBlockTagsProvider(event.getGenerator().getPackOutput(), registered, event.getWorldLookupProvider()));
        System.out.println("Content API verification passed (10 scenarios, dedicated server)");
    }

    private static void declarationsStayDeferred() {
        var calls = new AtomicInteger();
        var content = new ContentRegistry("verification");
        var holder = content.items().create("tools/wrench", properties -> {
            calls.incrementAndGet();
            return new CustomItem(properties);
        }).maxStackSize(1).model(ItemModelType.HANDHELD)
            .displayName("zh_cn", "扳手").register();
        var block = content.blocks().create("cube", CustomBlock::new).register();
        content.blocks().create("hidden").noItem().register();
        require(calls.get() == 0 && !holder.isBound() && !block.isBound(), "Factories must remain deferred");
        require(content.creativeItems().size() == 2, "noItem must not create a creative entry");
        require(content.blockDefinitions().get(0).item().getId().equals(block.getId()), "Block item must share its block ID");
        require(content.blockDefinitions().get(1).item() == null, "noItem must not register an item");
        var definition = content.itemDefinitions().getFirst();
        require(definition.texture().toString().equals("verification:item/tools/wrench"), "Default nested texture path");
        require(definition.names().get("zh_cn").equals("扳手"), "Language metadata");
        expect(UnsupportedOperationException.class, () -> definition.names().put("en_us", "Changed"));
        expect(UnsupportedOperationException.class, () -> content.itemDefinitions().clear());
    }

    private static void conflictsDoNotPartiallyRegister() {
        var content = new ContentRegistry("conflicts");
        content.items().create("occupied").register();
        expect(IllegalArgumentException.class, () -> content.blocks().create("occupied").register());
        require(content.blockDefinitions().isEmpty(), "A rejected block must not be partially queued");
        content.blocks().create("occupied").noItem().register();
        expect(IllegalArgumentException.class, () -> content.items().create("occupied").register());
        content.blocks().create("cube").register();
        expect(IllegalArgumentException.class, () -> content.items().create("cube").register());
        expect(IllegalArgumentException.class, () -> content.blocks().create("cube").noItem().register());
        require(content.creativeItems().size() == 2, "Conflicts must not add creative entries");
    }

    private static void buildersAreSingleUse() {
        var content = new ContentRegistry("lifecycle");
        var builder = content.items().create("item").displayName("en_us", "Original");
        builder.register();
        expect(IllegalStateException.class, builder::register);
        expect(IllegalStateException.class, () -> builder.displayName("en_us", "Changed"));
        expect(IllegalStateException.class, () -> builder.tag("lifecycle:late"));
        var block = content.blocks().create("block");
        block.register();
        expect(IllegalStateException.class, block::register);
        expect(IllegalStateException.class, block::noItem);
        expect(IllegalStateException.class, () -> block.tag("lifecycle:late"));
        expect(IllegalStateException.class, () -> block.tagItem("lifecycle:late"));
        expect(IllegalStateException.class, () -> block.tagBoth("lifecycle:late"));
        require(content.itemDefinitions().getFirst().names().get("en_us").equals("Original"), "Submitted metadata is stable");
    }

    private static void invalidDeclarationsFailEarly() {
        var content = new ContentRegistry("validation");
        expect(IllegalArgumentException.class, () -> content.items().create("other:item"));
        expect(IllegalArgumentException.class, () -> content.items().create(""));
        expect(IllegalArgumentException.class, () -> content.items().create("item").maxStackSize(0));
        expect(IllegalArgumentException.class, () -> content.items().create("item").maxDamage(0));
        expect(IllegalArgumentException.class, () -> content.items().create("item").displayName("en-US", "Name"));
        require(content.itemDefinitions().isEmpty(), "Invalid declarations must not be submitted");
    }

    private static void tagTargetsAreIndependent() {
        var content = new ContentRegistry("tag_contracts");
        var itemKey = TagKey.create(Registries.ITEM, Identifier.parse("tag_contracts:shared"));
        var blockKey = TagKey.create(Registries.BLOCK, Identifier.parse("tag_contracts:shared"));
        content.items().create("item").tag("tag_contracts:shared").tag(itemKey).register();
        content.blocks().create("block")
            .tagBoth("tag_contracts:shared").tag(blockKey).tagItem(itemKey).register();
        content.blocks().create("block_only").tag(blockKey).register();
        content.blocks().create("item_only").tagItem(itemKey).register();
        var item = content.itemDefinitions().getFirst();
        var block = content.blockDefinitions().getFirst();
        require(item.tags().size() == 1 && item.tags().contains(itemKey), "Item tags deduplicate both overloads");
        require(block.tags().size() == 1 && block.tags().contains(blockKey), "Block tags deduplicate");
        require(block.itemTags().size() == 1 && block.itemTags().contains(itemKey), "Block item tags deduplicate");
        require(content.blockDefinitions().get(1).itemTags().isEmpty(), "Block tags must not leak into item tags");
        require(content.blockDefinitions().get(2).tags().isEmpty(), "Item tags must not leak into block tags");
        expect(UnsupportedOperationException.class, () -> item.tags().clear());
        expect(UnsupportedOperationException.class, () -> block.tags().clear());
        expect(UnsupportedOperationException.class, () -> block.itemTags().clear());
    }

    private static void itemTagVarargsExpandImmediateParents() {
        var content = new ContentRegistry("tag_varargs");
        var ingots = QS.itt("c:ingots");
        var aluminum = QS.itt("c:ingots/aluminum");
        var nested = QS.itt("c:materials/metals/aluminum");
        content.items().create("strings")
            .tag("c:ingots/aluminum", "c:materials/metals/aluminum", "c:plain", "c:ingots/aluminum")
            .register();
        require(content.itemDefinitions().getFirst().tags().equals(java.util.Set.of(
            ingots, aluminum, QS.itt("c:materials/metals"), nested, QS.itt("c:plain"))),
            "String varargs must add only immediate parents and deduplicate tags");
        content.items().create("keys").tag(aluminum, nested, aluminum).register();
        require(content.itemDefinitions().get(1).tags().equals(java.util.Set.of(aluminum, nested)),
            "TagKey varargs must deduplicate without adding parents");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void invalidTagsFailEarly() {
        var content = new ContentRegistry("tag_validation");
        String[] invalid = {null, "", "tools/wrench", ":wrench", "c:", "c:tools:wrench", "#c:tools/wrench",
            "C:tools/wrench", "c:Tools/wrench", " c:wrench", "c:wrench ", "c:扳手", "c:tools\\wrench",
            "c:/wrench", "c:wrench/", "c:tools//wrench", "c:.", "c:..", "c:tools/../wrench", "c:tools/./wrench",
            ".:wrench", "..:wrench", "c:wrench\n"};
        for (String id : invalid) {
            expect(IllegalArgumentException.class, () -> QS.itt(id));
            expect(IllegalArgumentException.class, () -> QS.blt(id));
            expect(IllegalArgumentException.class, () -> content.items().create("item").tag(id));
            expect(IllegalArgumentException.class, () -> content.blocks().create("block").tag(id));
            expect(IllegalArgumentException.class, () -> content.blocks().create("block").tagItem(id));
            var both = content.blocks().create("block");
            expect(IllegalArgumentException.class, () -> both.tagBoth(id));
            both.noItem(); // A rejected tagBoth must not leave an item tag behind.
        }
        var itemTag = TagKey.create(Registries.ITEM, Identifier.parse("tag_validation:valid"));
        var blockTag = TagKey.create(Registries.BLOCK, Identifier.parse("tag_validation:valid"));
        expect(IllegalArgumentException.class, () -> content.items().create("item").tag((TagKey<Item>) null));
        expect(IllegalArgumentException.class, () -> content.blocks().create("block").tag((TagKey<Block>) null));
        expect(IllegalArgumentException.class, () -> content.blocks().create("block").tagItem((TagKey<Item>) null));
        expect(IllegalArgumentException.class, () -> content.items().create("item").tag((TagKey) blockTag));
        expect(IllegalArgumentException.class, () -> content.blocks().create("block").tag((TagKey) itemTag));
        expect(IllegalArgumentException.class, () -> content.blocks().create("block").tagItem((TagKey) blockTag));
        var unsafeItem = TagKey.create(Registries.ITEM, Identifier.parse("tag_validation:../escape"));
        var unsafeBlock = TagKey.create(Registries.BLOCK, Identifier.parse("tag_validation:../escape"));
        expect(IllegalArgumentException.class, () -> content.items().create("item").tag(unsafeItem));
        expect(IllegalArgumentException.class, () -> content.blocks().create("block").tag(unsafeBlock));
        expect(IllegalArgumentException.class, () -> content.blocks().create("block").tagItem(unsafeItem));
        content.items().create("valid").tag("valid_mod-1.test:tools/wrench_2-v1.test").register();
        require(content.itemDefinitions().size() == 1, "Only the valid item must be submitted");
        require(content.blockDefinitions().isEmpty(), "Invalid tags must not submit blocks");
    }

    private static void noItemRejectsItemTags() {
        var content = new ContentRegistry("no_item_tags");
        var itemTag = TagKey.create(Registries.ITEM, Identifier.parse("no_item_tags:valid"));
        var hidden = content.blocks().create("hidden").noItem();
        expect(IllegalStateException.class, () -> hidden.tagItem("no_item_tags:valid"));
        expect(IllegalStateException.class, () -> hidden.tagItem(itemTag));
        expect(IllegalStateException.class, () -> hidden.tagBoth("no_item_tags:valid"));
        hidden.tag("no_item_tags:block_only").register();
        require(content.blockDefinitions().getFirst().tags().size() == 1, "Failed tagBoth must not add a block tag");
        require(content.blockDefinitions().getFirst().itemTags().isEmpty(), "noItem must not retain item tags");
        var itemFirst = content.blocks().create("item_first").tagItem(itemTag);
        expect(IllegalStateException.class, itemFirst::noItem);
        itemFirst.register();
        var bothFirst = content.blocks().create("both_first").tagBoth("no_item_tags:valid");
        expect(IllegalStateException.class, bothFirst::noItem);
        bothFirst.register();
        require(content.blockDefinitions().get(1).item() != null && content.blockDefinitions().get(2).item() != null,
            "Rejected noItem must preserve the block item");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void quickTagConversionsAreTyped() {
        String id = "qs_verification:tools/wrench";
        TagKey<Item> item = QS.itt(id);
        TagKey<Block> block = QS.blt(id);
        require(item.isFor(Registries.ITEM) && block.isFor(Registries.BLOCK), "QS must preserve registry types");
        require(item.location().equals(block.location()), "Matching IDs must remain in separate registries");
        require(QS.itt(item) == item && QS.blt(block) == block, "Validation must preserve existing keys");
        require(QS.idt("stone").equals(Identifier.parse("minecraft:stone")), "General identifiers retain vanilla namespace behavior");
        expect(IllegalArgumentException.class, () -> QS.itt((TagKey<Item>) null));
        expect(IllegalArgumentException.class, () -> QS.blt((TagKey<Block>) null));
        expect(IllegalArgumentException.class, () -> QS.itt((TagKey) block));
        expect(IllegalArgumentException.class, () -> QS.blt((TagKey) item));
        expect(IllegalArgumentException.class, () -> QS.itt(TagKey.create(Registries.ITEM, Identifier.parse("qs_verification:../escape"))));
        expect(IllegalArgumentException.class, () -> QS.blt(TagKey.create(Registries.BLOCK, Identifier.parse("qs_verification:../escape"))));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Expected " + type.getSimpleName(), error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static final class CustomItem extends Item {
        CustomItem(Properties properties) {
            super(properties);
        }
    }

    private static final class CustomBlock extends Block {
        CustomBlock(Properties properties) {
            super(properties);
        }
    }
}
