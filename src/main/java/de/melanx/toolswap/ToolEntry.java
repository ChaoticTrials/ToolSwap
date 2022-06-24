package de.melanx.toolswap;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record ToolEntry(TagKey<Block> type,
                        ItemStack stack, BlockState toHarvest) {

    public Item getToolItem() {
        return this.stack.getItem();
    }

    public int getHarvestLevel() {
        return this.getToolItem() instanceof DiggerItem item ? item.getTier().getLevel() : -1;
    }

    public float getEfficiency() {
        return this.stack.getItem().getDestroySpeed(this.stack, this.toHarvest);
    }

    @Override
    public String toString() {
        return this.type.toString() + ": " + this.stack.toString();
    }
}
