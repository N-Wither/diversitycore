package net.aquiferic.diversitycore.client;

import net.aquiferic.diversitycore.DiversityCore;
import net.aquiferic.diversitycore.client.screen.CreativePowerBankScreen;
import net.aquiferic.diversitycore.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** Client entry point and initialization for screens, renderers, and other client features. */
@Mod(value = DiversityCore.MODID, dist = Dist.CLIENT)
public final class DiversityCoreClient {
    public DiversityCoreClient(IEventBus bus) {
        bus.addListener(DiversityCoreClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.CREATIVE_POWER_BANK.get(), CreativePowerBankScreen::new);
    }
}
