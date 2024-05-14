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
    public static ForgeConfigSpec.IntValue minDurability;
    public static ForgeConfigSpec.BooleanValue ignoreHarvestLevel;
    public static ForgeConfigSpec.BooleanValue sneakToPrevent;
    public static ForgeConfigSpec.EnumValue<SortType> sortType;
    public static ForgeConfigSpec.EnumValue<IgnoreMode> ignoreEmptyHand;
    public static ForgeConfigSpec.EnumValue<SwapMode> swapMode;

    public static void init(ForgeConfigSpec.Builder builder) {
        saveBreakingTools = builder.comment("If this is on, tool with 1 durability left will be saved. Only works for BREAKING a block, not stripping, flattening, or tilting.")
                .define("save", false);
        minDurability = builder.comment("If items should be saved, this is the minimum durability which they are allowed to have.")
                .defineInRange("min_durability", 1, 1, Integer.MAX_VALUE);
        ignoreHarvestLevel = builder.comment("If this is on, harvest level of tools will be ignored on breaking blocks. Otherwise it will always search for the lowest possible tool.")
                .define("ignore_harvest_level", true);
        sneakToPrevent = builder.comment("If this is on, sneaking will not swap your tool.")
                .define("sneak_to_prevent", true);
        sortType = builder.comment("Set the mode in which order the tools will be chosen.",
                        "  LEVEL = sorted by harvest level, lowest first",
                        "  LEVEL_INVERTED = sorted by harvest level, highest first",
                        "  LEFT_TO_RIGHT = sorted from left to right",
                        "  RIGHT_TO_LEFT = sorted from right to left",
                        "  ENCHANTED_FIRST = sorted by harvest level, highest enchanted item first",
                        "  ENCHANTED_LAST = sorted by harvest level, highest unenchanted item first")
                .defineEnum("sorttype", SortType.LEVEL);
        ignoreEmptyHand = builder.comment("Choose the mode when swapping is fine:",
                        "  ALWAYS = Always swap, ignore item in hand",
                        "  EMPTY_HAND = Only swap if your hand is empty",
                        "  ITEMS = Only swap if you hold any item",
                        "  TOOLS = Only swap if you hold any tool (items with tag \"minecraft:tools\")",
                        "  NO_TOOLS = Only swap if you hold any item excluding tools (items with tag \"minecraft:tools\")")
                .defineEnum("ignore_empty_hand", IgnoreMode.ALWAYS);
        swapMode = builder.comment("Choose the mode for how to detect items",
                        "  TAG = Default, best for modpacks with other tool mods",
                        "  VANILLA = If TAG mode doesn't work, try vanilla mode on vanilla servers")
                .defineEnum("swap_mode", SwapMode.TAG);
    }

    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        ToolSwap.LOGGER.debug("Loading config file {}", path);
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
        configData.load();
        spec.setConfig(configData);
    }

    public enum IgnoreMode {
        ALWAYS,
        EMPTY_HAND,
        ITEMS,
        TOOLS,
        NO_TOOLS
    }

    public enum SwapMode {
        TAG,
        VANILLA
    }
}
