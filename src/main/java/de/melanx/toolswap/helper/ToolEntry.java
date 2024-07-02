package de.melanx.toolswap.helper;

import de.melanx.toolswap.ToolSwap;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record ToolEntry(Tool tool, ItemStack stack) {

    private static final Set<TieredItem> LOGGED_ITEMS = new HashSet<>();
    private static final Set<TagKey<Block>> LOGGED_TAGS = new HashSet<>();

    private static final Map<TagKey<Block>, Integer> HARVEST_LEVELS = Map.of(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, 0,
            BlockTags.INCORRECT_FOR_STONE_TOOL, 1,
            BlockTags.INCORRECT_FOR_IRON_TOOL, 2,
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 3,
            BlockTags.INCORRECT_FOR_GOLD_TOOL, 0,
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4
    );

    public Tool getTool() {
        return this.tool;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Nullable
    public TieredItem tieredItem() {
        return this.stack.getItem() instanceof TieredItem tieredItem ? tieredItem : null;
    }

    public boolean isEnchanted() {
        return this.stack.isEnchanted();
    }

    public int damagePerBlock() {
        return this.tool.damagePerBlock();
    }

    public int getHarvestLevel() {
        TieredItem item = this.tieredItem();
        if (item == null) {
            return 0;
        }
        Tier tier = item.getTier();
        TagKey<Block> incorrectBlocksForDrops = tier.getIncorrectBlocksForDrops();
        Integer harvestLevel = HARVEST_LEVELS.get(incorrectBlocksForDrops);
        if (harvestLevel == null) {
            if (LOGGED_ITEMS.add(item) || LOGGED_TAGS.add(incorrectBlocksForDrops)) {
                ToolSwap.LOGGER.info("Please notify the author of this. Missing tag compat of item {}: {}", item, incorrectBlocksForDrops);
            }

            return 0;
        }

        return harvestLevel;
    }

    public float getEfficiency() {
        TieredItem item = this.tieredItem();
        return item != null ? item.getTier().getSpeed() : 0.0F;
    }

    @Override
    public String toString() {
        return this.stack.toString();
    }
}
