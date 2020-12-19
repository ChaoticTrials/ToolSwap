package de.melanx.toolswap;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;

public class ClientConfig {
    public static final ForgeConfigSpec CLIENT_CONFIG;
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    static {
        init(CLIENT_BUILDER);
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public static ForgeConfigSpec.BooleanValue saveBreakingTools;

    public static void init(ForgeConfigSpec.Builder builder) {
        saveBreakingTools = builder.comment("If this is on, tool with 1 durability left will be saved. [WIP - at the moment only working for axes, hoes, pickaxes and shovels for BREAKING a block]")
                .define("save", true);
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        ToolSwap.LOGGER.debug("Loading config file {}", path);
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
        configData.load();
        spec.setConfig(configData);
    }
}
