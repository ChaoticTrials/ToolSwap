package de.melanx.toolswap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ToolSwap.MODID)
public class ToolSwap {

    public static final String MODID = "toolswap";
    public static final Logger LOGGER = LogManager.getLogger(ToolSwap.class);

    public ToolSwap() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new ClientToolSwap();
        } else {
            LOGGER.warn("###################################################");
            LOGGER.warn("#      AutomaticToolSwap was loaded on server     #");
            LOGGER.warn("#  Consider removing it to save some of your RAM  #");
            LOGGER.warn("###################################################");
        }
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }
}
