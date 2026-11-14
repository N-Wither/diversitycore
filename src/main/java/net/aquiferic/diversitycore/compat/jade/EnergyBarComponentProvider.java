package net.aquiferic.diversitycore.compat.jade;

import java.util.List;
import net.aquiferic.diversitycore.api.energy.EnergyBarData;

import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;

/** Applies the selected scope without changing Jade's energy data collection. */
final class EnergyBarComponentProvider implements IComponentProvider<BlockAccessor> {
    static final EnergyBarComponentProvider INSTANCE = new EnergyBarComponentProvider();
    static final Identifier UID = EnergyBarServerProvider.UID;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        EnergyBarMode mode = config.getEnum(UID);
        if (mode == EnergyBarMode.FALSE || !config.get(JadeIds.UNIVERSAL_ENERGY_STORAGE)
                || (!accessor.showDetails() && config.get(JadeIds.UNIVERSAL_ENERGY_STORAGE_DETAILED))) {
            return;
        }
        var bars = selectBars(accessor, mode);
        if (bars.isEmpty()) return;
        tooltip.remove(JadeIds.UNIVERSAL_ENERGY_STORAGE);
        for (var data : bars) {
            tooltip.add(new EnergyBarElement(data.label(), data.fill()).tag(UID));
        }
    }

    static List<EnergyBarData> selectBars(
            BlockAccessor accessor, EnergyBarMode mode) {
        if (mode == EnergyBarMode.FALSE) return List.of();
        var dedicated = EnergyBarServerProvider.INSTANCE.decodeFromData(accessor);
        return dedicated.map(List::of).orElseGet(() ->
                mode == EnergyBarMode.ALL ? UniversalEnergyBars.read(accessor) : List.of());
    }

    // Jade must invoke us regardless of scope; this UID now stores an enum, not a boolean.
    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY + 1100;
    }
}
