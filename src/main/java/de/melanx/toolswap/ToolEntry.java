package de.melanx.toolswap;

import net.minecraft.tags.Tag;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public record ToolEntry(Tag<Block> type,
                        ItemStack stack) {

    public Tag<Block> getType() {
        return this.type;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Nullable
    public DiggerItem getToolItem() {
        return this.stack.getItem() instanceof DiggerItem ? (DiggerItem) this.stack.getItem() : null;
    }

    public int getHarvestLevel() {
        DiggerItem item = this.getToolItem();
        return item != null ? item.getTier().getLevel() : -1;
    }

    public float getEfficiency() {
        DiggerItem item = this.getToolItem();
        return item != null ? item.getTier().getLevel() : 0.0F;
    }

    @Override
    public String toString() {
        return this.getType().toString() + ": " + this.stack.toString();
    }
}
