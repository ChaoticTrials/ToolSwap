package de.melanx.toolswap.helper;

import de.melanx.toolswap.ToolSwap;
import de.melanx.toolswap.config.ClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Util {

    private static final MutableComponent WARNING = Component.translatable(ToolSwap.MODID + ".warning").withStyle(ChatFormatting.DARK_RED);

    public static boolean validItem(ItemStack item) {
        return item.has(DataComponents.TOOL) && Util.hasEnoughDurability(item);
    }

    public static boolean shouldIgnore(ItemStack heldItem) {
        return !switch (ClientConfig.ignoreEmptyHand.get()) {
            case EMPTY_HAND -> heldItem.isEmpty();
            case ITEMS -> !heldItem.isEmpty();
            case TOOLS -> heldItem.has(DataComponents.TOOL);
            case NO_TOOLS -> !heldItem.has(DataComponents.TOOL);
            default -> true;
        };
    }

    public static void saveItem(LocalPlayer player) {
        if (Util.hasEnoughDurability(player.getMainHandItem())) {
            return;
        }

        MultiPlayerGameMode controller = Minecraft.getInstance().gameMode;
        AbstractContainerMenu container = player.containerMenu;
        int emptySlot = -1;

        ItemStack currentTool = player.getInventory().getItem(player.getInventory().selected);
        ItemStack equalTool = Util.findEqualTool(player.getInventory(), currentTool);
        if (currentTool != equalTool) {
            emptySlot = player.getInventory().findSlotMatchingItem(equalTool);
        }

        if (emptySlot == -1) {
            for (Slot slot : container.slots) {
                if (slot.index > 9 && slot.getItem().isEmpty()) {
                    emptySlot = slot.index;
                    break;
                }
            }
        }

        if (emptySlot != -1) {
            //noinspection ConstantConditions
            controller.handleInventoryMouseClick(container.containerId, player.getInventory().selected + 36, 0, ClickType.PICKUP, player);
            controller.handleInventoryMouseClick(container.containerId, emptySlot, 0, ClickType.PICKUP, player);
            controller.handleInventoryMouseClick(container.containerId, player.getInventory().selected + 36, 0, ClickType.PICKUP, player);
        } else {
            player.displayClientMessage(Util.WARNING, true);
        }
    }

    public static void sortTools(List<ToolEntry> tools) {
        Comparator<ToolEntry> comparator = switch (ClientConfig.sortType.get()) {
            case LEVEL -> Comparator.comparing(ToolEntry::damagePerBlock);
            case LEVEL_INVERTED -> Comparator.comparing(ToolEntry::damagePerBlock).reversed();
            case ENCHANTED_FIRST ->
                    Comparator.comparing(ToolEntry::isEnchanted).reversed().thenComparing(ToolEntry::damagePerBlock);
            case ENCHANTED_LAST ->
                    Comparator.comparing(ToolEntry::isEnchanted).thenComparing(ToolEntry::damagePerBlock);
            default -> null;
        };

        if (comparator == null) {
            if (ClientConfig.sortType.get() == ClientConfig.SortType.RIGHT_TO_LEFT) {
                Collections.reverse(tools);
            }

            return;
        }

        tools.sort(comparator);
    }

    private static boolean hasEnoughDurability(ItemStack stack) {
        return !ClientConfig.saveBreakingTools.get() || !stack.isDamageableItem() || stack.getDamageValue() != stack.getMaxDamage() - ClientConfig.minDurability.get();
    }

    private static ItemStack findEqualTool(Inventory inventory, ItemStack stack) {
        if ((stack.has(DataComponents.TOOL))) {
            Tool tool = stack.get(DataComponents.TOOL);
            //noinspection DataFlowIssue
            List<Tool.Rule> rules = tool.rules();

            for (ItemStack item : inventory.items) {
                //noinspection DataFlowIssue
                if (Util.validItem(item) && item.get(DataComponents.TOOL).rules() == rules) {
                    return item;
                }
            }
        }

        return stack;
    }
}
