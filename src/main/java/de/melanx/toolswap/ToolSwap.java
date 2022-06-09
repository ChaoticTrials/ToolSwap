package de.melanx.toolswap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ToolSwap.MODID)
public class ToolSwap {

    public static final String MODID = "toolswap";
    public static final Logger LOGGER = LogManager.getLogger(ToolSwap.class);

    public ToolSwap() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new ClientToolSwap();
        }
    }
}
