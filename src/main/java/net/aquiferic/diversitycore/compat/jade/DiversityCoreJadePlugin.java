package net.aquiferic.diversitycore.compat.jade;

import net.minecraft.world.level.block.Block;
import net.aquiferic.diversitycore.content.block.CropPlantBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.ConfigIcon;

/** Discovered by Jade only; downstream mods need no separate plugin registration. */
@WailaPlugin
public final class DiversityCoreJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyBarServerProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(EnergyBarServerProvider.UID, ConfigIcon.none(), EnergyBarMode.TRUE);
        registration.registerBlockComponent(EnergyBarComponentProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(CropProgressComponentProvider.INSTANCE, CropPlantBlock.class);
    }
}
