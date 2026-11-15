package net.aquiferic.diversitycore.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import net.aquiferic.diversitycore.api.registry.CropTextureRatio;
import net.aquiferic.diversitycore.api.registry.ContentRegistry;
import net.aquiferic.diversitycore.api.registry.CropModelType;
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
            cropModels(blocks, definition.entry().block().get(), definition.textures(), definition.model(), definition.textureRatio());
            definition.entry().upperBlock().ifPresent(top -> cropModels(blocks, top.get(), definition.upperTextures(), definition.model(), definition.textureRatio()));
        }
        for (var definition : content.simpleCropDefinitions()) {
            cropModels(blocks, definition.entry().block().get(), java.util.Optional.of(net.minecraft.world.level.block.CropBlock.AGE),
                    definition.textures(), definition.model(), definition.textureRatio());
        }
    }

    private static void cropModels(BlockModelGenerators blocks, CropPlantBlock block, List<Identifier> textures, CropModelType type, CropTextureRatio ratio) {
        cropModels(blocks, block, block.ageProperty(), textures, type, ratio);
    }

    private static void cropModels(BlockModelGenerators blocks, net.minecraft.world.level.block.Block block,
            java.util.Optional<net.minecraft.world.level.block.state.properties.IntegerProperty> ageProperty,
            List<Identifier> textures, CropModelType type, CropTextureRatio ratio) {
        var models = new HashMap<Identifier, Identifier>();
        var stages = new java.util.ArrayList<Identifier>();
        for (int age = 0; age < textures.size(); age++) {
            Identifier texture = textures.get(age);
            Identifier model = models.get(texture);
            if (model == null) {
                model = ModelLocationUtils.getModelLocation(block, "_stage" + age);
                if (type == CropModelType.CROSS || ratio.width() != ratio.height()) {
                    blocks.modelOutput.accept(model, () -> cropGeometry(texture, type, ratio));
                } else {
                    ModelTemplates.CROP.create(model, TextureMapping.crop(new Material(texture)), blocks.modelOutput);
                }
                models.put(texture, model);
            }
            stages.add(model);
        }
        if (ageProperty.isEmpty()) {
            blocks.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(stages.getFirst())));
        } else {
            blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(ageProperty.get())
                    .generate(age -> BlockModelGenerators.plainVariant(stages.get(age)))));
        }
    }

    /** Full UVs preserve sprite proportions; mirrored back faces keep pixels at the same world position. */
    private static JsonObject cropGeometry(Identifier texture, CropModelType type, CropTextureRatio ratio) {
        var result = new JsonObject();
        result.addProperty("parent", type == CropModelType.CROSS ? "minecraft:block/cross" : "minecraft:block/crop");
        var textures = new JsonObject();
        String slot = type == CropModelType.CROSS ? "cross" : "crop";
        textures.addProperty(slot, texture.toString());
        result.add("textures", textures);
        var elements = new JsonArray();
        float height = ratio.modelHeight();
        if (type == CropModelType.CROSS) {
            for (int angle : new int[] {45, -45}) {
                var plane = plane(0.8F, 0, 8, 15.2F, height, 8, "north", "south", slot, false);
                var rotation = new JsonObject();
                rotation.add("origin", vector(8, 8, 8));
                rotation.addProperty("axis", "y");
                rotation.addProperty("angle", angle);
                rotation.addProperty("rescale", true);
                plane.add("rotation", rotation);
                elements.add(plane);
            }
        } else {
            for (int offset : new int[] {4, 12}) {
                elements.add(plane(0, -1, offset, 16, height - 1, offset, "north", "south", slot, offset == 12));
                elements.add(plane(offset, -1, 0, offset, height - 1, 16, "west", "east", slot, offset == 12));
            }
        }
        result.add("elements", elements);
        return result;
    }

    private static JsonObject plane(float x1, float y1, float z1, float x2, float y2, float z2,
            String front, String back, String slot, boolean reverse) {
        var plane = new JsonObject();
        plane.add("from", vector(x1, y1, z1));
        plane.add("to", vector(x2, y2, z2));
        plane.addProperty("shade_direction_override", "up");
        var faces = new JsonObject();
        for (String side : new String[] {front, back}) {
            var face = new JsonObject();
            var uv = new JsonArray();
            boolean mirrored = side.equals(back) ^ reverse;
            for (int value : new int[] {mirrored ? 16 : 0, 0, mirrored ? 0 : 16, 16}) uv.add(value);
            face.add("uv", uv);
            face.addProperty("texture", "#" + slot);
            faces.add(side, face);
        }
        plane.add("faces", faces);
        return plane;
    }

    private static JsonArray vector(float x, float y, float z) {
        var vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.level.block.Block>> getKnownBlocks() {
        return java.util.stream.Stream.concat(content.simpleCropDefinitions().stream().map(d -> d.entry().block()), java.util.stream.Stream.concat(content.blockDefinitions().stream().map(d -> d.holder()),
            content.cropDefinitions().stream().flatMap(d -> java.util.stream.Stream.concat(
                java.util.stream.Stream.of(d.entry().block()), d.entry().upperBlock().stream()))));
    }

    @Override
    protected java.util.stream.Stream<? extends net.minecraft.core.Holder<net.minecraft.world.item.Item>> getKnownItems() {
        return java.util.stream.Stream.concat(content.itemDefinitions().stream().map(d -> d.holder()),
            content.blockDefinitions().stream().filter(d -> d.item() != null).map(d -> d.item()));
    }
}
