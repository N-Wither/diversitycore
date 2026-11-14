package net.aquiferic.diversitycore.compat.jade;

import java.util.List;
import java.util.Map;
import net.aquiferic.diversitycore.api.energy.EnergyBarData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.ViewGroup;

/** Reuses Jade's public energy payload, including multiple stores and per-group units. */
final class UniversalEnergyBars {
    static final StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<EnergyView.Data>>>> CODEC =
            ViewGroup.listCodec(EnergyView.Data.STREAM_CODEC).cast();

    private UniversalEnergyBars() {}

    static List<EnergyBarData> read(BlockAccessor accessor) {
        var tag = accessor.getServerData().get(JadeIds.UNIVERSAL_ENERGY_STORAGE.toString());
        if (tag == null) return List.of();
        return accessor.decodeFromNbt(CODEC, tag)
                .map(entry -> convert(entry.getValue())).orElseGet(List::of);
    }

    static List<EnergyBarData> convert(List<ViewGroup<EnergyView.Data>> groups) {
        return groups.stream().flatMap(group -> {
            String unit = group.getExtraData().getStringOr("Unit", "FE");
            return group.views.stream()
                    .filter(view -> view.current() >= 0 && view.capacity() > 0)
                    .map(view -> new EnergyBarData(view.current(), view.capacity(), Component.translatable(
                            "jade.diversitycore.energy_with_unit", view.current(), view.capacity(), unit)));
        }).toList();
    }
}
