package net.aquiferic.diversitycore.verification;

import net.aquiferic.diversitycore.compat.jade.EnergyBarMode;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.aquiferic.diversitycore.api.energy.EnergyBarData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.ViewGroup;

/** Runs only when Jade is present in the isolated verification server. No GUI needed. */
public final class EnergyBarModeVerification {
    @SuppressWarnings("unchecked")
    public static void verify() throws ReflectiveOperationException {
        var universal = Class.forName("net.aquiferic.diversitycore.compat.jade.UniversalEnergyBars");
        var codecField = universal.getDeclaredField("CODEC");
        codecField.setAccessible(true);
        var codec = (StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Identifier, List<ViewGroup<EnergyView.Data>>>>) codecField.get(null);
        var convert = universal.getDeclaredMethod("convert", List.class);
        convert.setAccessible(true);
        var select = Class.forName("net.aquiferic.diversitycore.compat.jade.EnergyBarComponentProvider")
                .getDeclaredMethod("selectBars", BlockAccessor.class, EnergyBarMode.class);
        select.setAccessible(true);
        var uid = Identifier.fromNamespaceAndPath("diversitycore", "energy_bar");
        var group = new ViewGroup<>(List.of(new EnergyView.Data(25, 100),
                new EnergyView.Data(Long.MAX_VALUE, Long.MAX_VALUE), new EnergyView.Data(0, 0)));
        group.getExtraData().putString("Unit", "RF");
        var second = new ViewGroup<>(List.of(new EnergyView.Data(50, 200)));
        var payload = Map.entry(JadeIds.UNIVERSAL_ENERGY_STORAGE_DEFAULT, List.of(group, second));
        @SuppressWarnings("deprecation")
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            codec.encode(buffer, payload);
            var decoded = codec.decode(buffer);
            var bars = (List<EnergyBarData>) convert.invoke(null, decoded.getValue());
            require(bars.size() == 3 && bars.getFirst().fill() == 0.25F, "All valid stores must survive the wire codec");
            require(bars.get(1).stored() == Long.MAX_VALUE, "Long capacities must not truncate");
            require(bars.getFirst().label().equals(Component.translatable(
                    "jade.diversitycore.energy_with_unit", 25L, 100L, "RF")), "Preserve energy units");
            require(bars.get(2).label().equals(Component.translatable(
                    "jade.diversitycore.energy_with_unit", 50L, 200L, "FE")), "Default unit is FE");
        } finally {
            buffer.release();
        }
        var dedicated = EnergyBarData.infinite(Component.literal("infinite"));
        var tag = new CompoundTag();
        tag.put(JadeIds.UNIVERSAL_ENERGY_STORAGE.toString(), new CompoundTag());
        var accessor = (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
                new Class<?>[]{BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getServerData" -> tag;
                    case "decodeFromNbt" -> Optional.of(args[0] == codec ? payload : dedicated);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        require(selectBars(select, accessor, EnergyBarMode.ALL).size() == 3,
                "All mode includes external stores without interface data");
        require(selectBars(select, accessor, EnergyBarMode.TRUE).isEmpty(),
                "Diversity-only mode excludes external stores");
        require(selectBars(select, accessor, EnergyBarMode.FALSE).isEmpty(), "Off mode changes nothing");
        tag.put(uid.toString(), new CompoundTag());
        require(selectBars(select, accessor, EnergyBarMode.ALL).equals(List.of(dedicated)),
                "All mode prefers interface labels and avoids duplicate bars");
        require(selectBars(select, accessor, EnergyBarMode.TRUE).equals(List.of(dedicated)),
                "Diversity-only mode includes interface stores");
        require(selectBars(select, accessor, EnergyBarMode.FALSE).isEmpty(),
                "Off mode also excludes interface stores");
        require(EnergyBarMode.valueOf("TRUE") == EnergyBarMode.TRUE
                && EnergyBarMode.valueOf("FALSE") == EnergyBarMode.FALSE, "Old boolean choices retain their meaning");
        System.out.println("Jade three-state energy bar verification passed");
    }

    @SuppressWarnings("unchecked")
    private static List<EnergyBarData> selectBars(java.lang.reflect.Method method, BlockAccessor accessor, EnergyBarMode mode)
            throws ReflectiveOperationException {
        return (List<EnergyBarData>) method.invoke(null, accessor, mode);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
