package net.aquiferic.diversitycore.datagen;

import java.util.HashMap;
import java.util.List;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.api.registry.ItemModelType;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public final class ModModelProvider extends ModelProvider {
    private final ContentRegistry content;

    public ModModelProvider(PackOutput output, ContentRegistry content) {
        super(output, content.namespace());
        this.content = content;
    }

    @Override
    protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        for (var definition : content.itemDefinitions()) {
            var template = definition.model() == ItemModelType.HANDHELD
                ? ModelTemplates.FLAT_HANDHELD_ITEM : ModelTemplates.FLAT_ITEM;
            items.generateFlatItem(definition.holder().get(), definition.texture(), template);
        }
        for (var definition : content.blockDefinitions()) {
            var block = definition.holder().get();
            var model = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(new Material(definition.texture())), blocks.modelOutput);
            blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(model)));
            if (definition.item() != null) {
                blocks.registerSimpleItemModel(definition.item().get(), model);
            }
        }
        for (var definition : content.cropDefinitions()) {
            cropModels(blocks, definition.entry().block().get(), definition.textures());
            definition.entry().upperBlock().ifPresent(top -> cropModels(blocks, top.get(), definition.upperTextures()));
        }
    }

    private static void cropModels(BlockModelGenerators blocks, CropPlantBlock block, List<Identifier> textures) {
        var models = new HashMap<Identifier, Identifier>();
        var stages = new java.util.ArrayList<Identifier>();
        for (int age = 0; age < textures.size(); age++) {
            Identifier texture = textures.get(age);
            Identifier model = models.get(texture);
            if (model == null) {
                model = ModelLocationUtils.getModelLocation(block, "_stage" + age);
                ModelTemplates.CROP.create(model, TextureMapping.crop(new Material(texture)), blocks.modelOutput);
                models.put(texture, model);
            }
            stages.add(model);
        }
        if (block.ageProperty().isEmpty()) {
            blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(stages.getFirst())));
        } else {
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(block.ageProperty().get())
                    .generate(age -> BlockModelGenerators.plainVariant(stages.get(age)))));
        }
    }

    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.level.block.Block>> getKnownBlocks() {
        return java.util.stream.Stream.concat(content.blockDefinitions().stream().map(d -> d.holder()),
            content.cropDefinitions().stream().flatMap(d -> java.util.stream.Stream.concat(
                java.util.stream.Stream.of(d.entry().block()), d.entry().upperBlock().stream())));
    }

    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.item.Item>> getKnownItems() {
        return java.util.stream.Stream.concat(content.itemDefinitions().stream().map(d -> d.holder()),
            content.blockDefinitions().stream().filter(d -> d.item() != null).map(d -> d.item()));
    }
}
