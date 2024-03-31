package de.melanx.toolswap;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public record ToolEntry(TagKey<Block> type,
        ItemStack stack) {

    public TagKey<Block> getType() {
        return this.type;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Nullable
    public DiggerLike getToolItem() {
        if (this.stack.getItem() instanceof DiggerLike diggerLike) {
            return diggerLike;
        }

        return this.stack.getItem() instanceof DiggerItem diggerItem ? diggerItem::getTier : null;
    }

    public int getHarvestLevel() {
        DiggerLike item = this.getToolItem();
        //noinspection deprecation
        return item != null ? item.getTier().getLevel() : -1;
    }

    public float getEfficiency() {
        DiggerLike item = this.getToolItem();
        return item != null ? item.getTier().getSpeed() : 0.0F;
    }

    @Override
    public String toString() {
        return this.getType().toString() + ": " + this.stack.toString();
    }
}
