package net.aquiferic.diversitycore.verification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.aquiferic.diversitycore.api.energy.EnergyBarData;
import net.aquiferic.diversitycore.api.energy.EnergyBarProvider;
import net.aquiferic.diversitycore.datagen.TranslationSources;
import net.aquiferic.diversitycore.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class EnergyBarVerification {
    static void verify() {
        require(EnergyBarData.of(0, 100).fill() == 0, "Empty energy bar");
        require(EnergyBarData.of(50, 100).fill() == 0.5F, "Half-full energy bar");
        require(EnergyBarData.of(Long.MAX_VALUE, Long.MAX_VALUE).fill() == 1, "Long capacity must not overflow");
        require(EnergyBarData.of(200, 100).fill() == 1, "Over-capacity energy must clamp");
        require(EnergyBarData.infinite(Component.literal("infinite")).fill() == 1, "Infinite energy bar");
        expectInvalid(() -> EnergyBarData.of(-1, 100));
        expectInvalid(() -> EnergyBarData.of(0, 0));
        var block = ModBlocks.CREATIVE_POWER_BANK.get();
        require(block instanceof EnergyBarProvider, "Creative bank opts in through public API");
        var data = ((EnergyBarProvider) block).getEnergyBar(null, BlockPos.ZERO, block.defaultBlockState());
        require(data != null && data.fill() == 1, "Creative bank provides a full bar without Jade classes");
        try {
            var directory = Files.createTempDirectory(Path.of("."), "translation-verification-");
            try {
                Files.writeString(directory.resolve("fr_fr.json"), "{\"test.key\":\"Énergie\"}");
                var translations = TranslationSources.read(directory);
                require(translations.size() == 1 && translations.get("fr_fr").get("test.key").equals("Énergie"),
                        "New locales are discovered and preserve UTF-8 without English substitution");
                Files.writeString(directory.resolve("fr_fr.json"), "{\"test.key\":123}");
                expectInvalid(() -> TranslationSources.read(directory));
            } finally {
                Files.deleteIfExists(directory.resolve("fr_fr.json"));
                Files.delete(directory);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        System.out.println("Energy bar and translation source verification passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new IllegalStateException("Invalid energy or translation must fail early");
    }
}
