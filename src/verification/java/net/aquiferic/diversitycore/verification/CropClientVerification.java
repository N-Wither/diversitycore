package net.aquiferic.diversitycore.verification;

import net.aquiferic.diversitycore.datagen.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Loaded only by the explicit fixture client-data run. */
@EventBusSubscriber(modid = "diversitycore_verification", value = Dist.CLIENT)
public final class CropClientVerification {
    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        var content = CropVerification.content;
        content.validateCropSeeds();
        var output = event.getGenerator().getPackOutput();
        event.addProvider(new ModModelProvider(output, content));
        event.addProvider(new ModLootProvider(output, content, event.getReloadableLookupProvider()));
        event.addProvider(new ModBlockTagsProvider(output, content, event.getWorldLookupProvider()));
        event.addProvider(new ModItemTagsProvider(output, content, event.getWorldLookupProvider()));
        event.addProvider(new ModLanguageProvider(output, content, "en_us"));
        event.addProvider(new ModLanguageProvider(output, content, "zh_cn"));
    }

    private static boolean renderChecked;

    /** Validates actual GPU-client baked models after the initial resource reload, then exits. */
    @SubscribeEvent
    public static void verifyRendering(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (renderChecked || !Boolean.getBoolean("diversitycore.verifyCropRendering")) return;
        var client = net.minecraft.client.Minecraft.getInstance();
        if (!client.isGameLoadFinished()) return;
        renderChecked = true;
        var modelSet = client.getModelManager().getBlockStateModelSet();
        int checked = 0;
        for (var definition : CropVerification.content.cropDefinitions()) {
            var blocks = java.util.stream.Stream.concat(java.util.stream.Stream.of(definition.entry().block()), definition.entry().upperBlock().stream()).toList();
            for (var block : blocks) {
                for (var state : block.get().getStateDefinition().getPossibleStates()) {
                    var model = modelSet.get(state);
                    if (model == modelSet.missingModel()) throw new AssertionError("Missing crop model: " + state);
                    var parts = new java.util.ArrayList<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>();
                    model.collectParts(net.minecraft.client.renderer.block.BlockAndTintGetter.EMPTY, net.minecraft.core.BlockPos.ZERO,
                        state, net.minecraft.util.RandomSource.create(0), parts);
                    var quads = parts.stream().flatMap(part -> part.getQuads(null).stream()).toList();
                    if (quads.isEmpty()) throw new AssertionError("Empty crop geometry: " + state);
                    for (var quad : quads) {
                        if (quad.materialInfo().layer() != net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT) {
                            throw new AssertionError("Crop must use transparent cutout geometry: " + state);
                        }
                    }
                    checked++;
                }
            }
        }
        System.out.println("Crop client rendering verification passed (" + checked + " states, actual baked CUTOUT geometry)");
        client.stop();
    }
}
