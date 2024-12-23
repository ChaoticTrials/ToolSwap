package de.melanx.toolswap.helper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;

public record ToolEntry(Tool tool, ItemStack stack) {

    public Tool getTool() {
        return this.tool;
    }

    public boolean isEnchanted() {
        return this.stack.isEnchanted();
    }

    public int damagePerBlock() {
        return this.tool.damagePerBlock();
    }

    @Override
    public String toString() {
        return this.stack.toString();
    }
}
