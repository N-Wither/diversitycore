package net.aquiferic.diversitycore.compat.jade;

/** Three choices; TRUE/FALSE preserve Jade's automatic migration of old boolean values. */
public enum EnergyBarMode {
    ALL,
    /** Only blocks or block entities implementing EnergyBarProvider. */
    TRUE,
    /** Keep the original Jade display. */
    FALSE
}
