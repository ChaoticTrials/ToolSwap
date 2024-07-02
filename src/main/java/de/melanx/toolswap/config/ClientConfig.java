package de.melanx.toolswap.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {

    public static final ModConfigSpec CLIENT_CONFIG;
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    static {
        init(CLIENT_BUILDER);
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public static ModConfigSpec.BooleanValue saveBreakingTools;
    public static ModConfigSpec.IntValue minDurability;
    public static ModConfigSpec.BooleanValue ignoreHarvestLevel;
    public static ModConfigSpec.BooleanValue sneakToPrevent;
    public static ModConfigSpec.EnumValue<SortType> sortType;
    public static ModConfigSpec.EnumValue<IgnoreMode> ignoreEmptyHand;

    public static void init(ModConfigSpec.Builder builder) {
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
    }

    public enum IgnoreMode {
        ALWAYS,
        EMPTY_HAND,
        ITEMS,
        TOOLS,
        NO_TOOLS
    }

    public enum SortType {
        LEVEL,
        LEVEL_INVERTED,
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        ENCHANTED_FIRST,
        ENCHANTED_LAST
    }
}
