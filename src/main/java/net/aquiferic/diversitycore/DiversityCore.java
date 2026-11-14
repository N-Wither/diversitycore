package net.aquiferic.diversitycore;

import com.mojang.logging.LogUtils;
import net.aquiferic.diversitycore.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DiversityCore.MODID)
public final class DiversityCore {
    public static final String MODID = "diversitycore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DiversityCore(IEventBus modEventBus) {
        LOGGER.info("[Diversity Core] Registering contents...");
        ModRegistries.register(modEventBus);
    }
}
