package de.melanx.toolswap;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;

public class ToolEntry {

    public final ToolType type;
    public final ItemStack stack;

    public ToolEntry(ToolType type, ItemStack stack) {
        this.type = type;
        this.stack = stack;
    }

    public ToolType getType() {
        return this.type;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    @Nullable
    public ToolItem getToolItem() {
        return this.stack.getItem() instanceof ToolItem ? (ToolItem) this.stack.getItem() : null;
    }

    public int getHarvestLevel() {
        ToolItem item = this.getToolItem();
        return item != null ? item.getTier().getHarvestLevel() : -1;
    }

    public float getEfficiency() {
        ToolItem item = this.getToolItem();
        return item != null ? item.getTier().getEfficiency() : 0.0F;
    }

    @Override
    public String toString() {
        return this.getType().getName() + ": " + this.stack.toString();
    }
}
