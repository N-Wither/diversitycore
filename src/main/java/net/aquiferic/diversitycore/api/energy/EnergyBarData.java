package net.aquiferic.diversitycore.api.energy;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Server snapshot for one energy bar, with a translatable label resolved by the client. */
public record EnergyBarData(long stored, long capacity, Component label) {
    public EnergyBarData {
        if (stored < 0 || capacity <= 0) {
            throw new IllegalArgumentException("Energy must be non-negative and capacity must be positive");
        }
        label = Objects.requireNonNull(label, "label").copy();
    }

    public static EnergyBarData of(long stored, long capacity) {
        return new EnergyBarData(stored, capacity,
                Component.translatable("jade.diversitycore.energy", stored, capacity));
    }

    public static EnergyBarData infinite(Component label) {
        return new EnergyBarData(1, 1, label);
    }

    public float fill() {
        return (float) Math.min(1.0D, (double) stored / capacity);
    }
}
