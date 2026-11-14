package net.aquiferic.diversitycore.compat.jade;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.api.energy.EnergyBarData;
import net.aquiferic.diversitycore.api.energy.EnergyBarProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

final class EnergyBarServerProvider implements StreamServerDataProvider<BlockAccessor, EnergyBarData> {
    static final EnergyBarServerProvider INSTANCE = new EnergyBarServerProvider();
    static final Identifier UID = Identifier.fromNamespaceAndPath(DiversityCore.MODID, "energy_bar");
    private static final StreamCodec<RegistryFriendlyByteBuf, EnergyBarData> CODEC = new StreamCodec<>() {
        @Override
        public EnergyBarData decode(RegistryFriendlyByteBuf buffer) {
            return new EnergyBarData(buffer.readVarLong(), buffer.readVarLong(),
                    ComponentSerialization.STREAM_CODEC.decode(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, EnergyBarData data) {
            buffer.writeVarLong(data.stored());
            buffer.writeVarLong(data.capacity());
            ComponentSerialization.STREAM_CODEC.encode(buffer, data.label());
        }
    };

    private static @Nullable EnergyBarProvider find(BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EnergyBarProvider provider) {
            return provider;
        }
        return accessor.getBlock() instanceof EnergyBarProvider provider ? provider : null;
    }

    @Override
    public @Nullable EnergyBarData streamData(BlockAccessor accessor) {
        var provider = find(accessor);
        return provider == null ? null : provider.getEnergyBar(
                accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return find(accessor) != null;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnergyBarData> streamCodec() {
        return CODEC;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
