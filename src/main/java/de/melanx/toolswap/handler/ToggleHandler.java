package de.melanx.toolswap.handler;

import de.melanx.toolswap.helper.ToolEntry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ToggleHandler {

    public static final int INVALID_SLOT = -1;
    private static ToggleHandler INSTANCE;
    private int previousSlot;

    private ToggleHandler() {
        this.previousSlot = INVALID_SLOT;
    }

    public static ToggleHandler initialize() {
        if (INSTANCE == null) {
            INSTANCE = new ToggleHandler();
        }

        return INSTANCE;
    }

    public void setPreviousSlot(LocalPlayer player) {
        this.setPreviousSlot(player.getInventory().selected);
    }

    public void setPreviousSlot(int slotId) {
        if (slotId == INVALID_SLOT) {
            return;
        }

        this.previousSlot = slotId;
    }

    public void resetSlot(LocalPlayer player) {
        this.switchTo(player, this.previousSlot);
        this.previousSlot = INVALID_SLOT;
    }

    public int getSlot() {
        return this.previousSlot;
    }

    public void switchTo(LocalPlayer player, ItemStack stack) {
        this.switchTo(player, player.getInventory().findSlotMatchingItem(stack));
    }

    public void switchTo(LocalPlayer player, int slotId) {
        if (slotId == INVALID_SLOT) {
            return;
        }

        if (player.getInventory().selected == slotId) {
            return;
        }

        this.setPreviousSlot(player.getInventory().selected);
        player.getInventory().selected = slotId;
    }

    public boolean switchIfPossible(LocalPlayer player, BlockState state, List<ToolEntry> toolEntries) {
        if (toolEntries.isEmpty()) {
            return false;
        }

        for (ToolEntry toolEntry : toolEntries) {
            Tool tool = toolEntry.getTool();
            if (tool.isCorrectForDrops(state)) {
                this.switchTo(player, toolEntry.stack());
                return true;
            }
        }

        return false;
    }
}
