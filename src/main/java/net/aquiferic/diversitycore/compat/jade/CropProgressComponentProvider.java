package net.aquiferic.diversitycore.compat.jade;

import java.util.Locale;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/** Uses the crop's actual age range rather than Jade's fixed vanilla age properties. */
final class CropProgressComponentProvider implements IComponentProvider<BlockAccessor> {
    static final CropProgressComponentProvider INSTANCE = new CropProgressComponentProvider();
    private static final Identifier UID = Identifier.fromNamespaceAndPath("diversitycore", "crop_progress");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!config.get(JadeIds.MC_CROP_PROGRESS)) return;
        var state = accessor.getBlockState();
        if (!(state.getBlock() instanceof CropPlantBlock crop)) return;
        var progress = crop.isMature(state)
                ? IThemeHelper.get().success(Component.translatable("tooltip.jade.crop_mature"))
                : IThemeHelper.get().info(String.format(Locale.ROOT, "%.0f%%", 100.0 * crop.getAge(state) / crop.getMaxAge()));
        tooltip.remove(JadeIds.MC_CROP_PROGRESS);
        tooltip.add(Component.translatable("tooltip.jade.crop_growth", progress));
    }

    @Override
    public Identifier getUid() { return UID; }

    @Override
    public boolean isRequired() { return true; }

    @Override
    public int getDefaultPriority() { return TooltipPosition.BODY + 1100; }
}
