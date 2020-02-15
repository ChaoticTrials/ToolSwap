package de.melanx.toolswap;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(ToolSwap.MODID)
public class ToolSwap {

    public static final String MODID = "toolswap";
    public static ToolSwap instance;
    ItemStack axe;
    ItemStack pickaxe;
    ItemStack shovel;

    public ToolSwap() {
        instance = this;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockDestroy(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().getEntityWorld().getGameTime() % 3 != 0) return;

        BlockState state = event.getState();
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            ItemStack heldItem = player.getHeldItemMainhand();
            if (!player.isCrouching()) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.inventory.getStackInSlot(i);
                    if (stack.getToolTypes().contains(ToolType.AXE)) {
                        axe = stack;
                    } else if (stack.getToolTypes().contains(ToolType.PICKAXE)) {
                        pickaxe = stack;
                    } else if (stack.getToolTypes().contains(ToolType.SHOVEL)) {
                        shovel = stack;
                    }
                }

                Material material = state.getMaterial();
                if ((material == Material.WOOD || material == Material.BAMBOO || material == Material.GOURD) && axe != null) {
                    if (!(heldItem.getToolTypes().contains(ToolType.AXE))) {
                        player.inventory.currentItem = player.inventory.getSlotFor(axe);
                    }
                } else if ((material == Material.ROCK || material == Material.IRON || material == Material.ANVIL) && pickaxe != null) {
                    if (!(heldItem.getToolTypes().contains(ToolType.PICKAXE))) {
                        player.inventory.currentItem = player.inventory.getSlotFor(pickaxe);
                    }
                } else if ((material == Material.ORGANIC || material == Material.EARTH || material == Material.CLAY || material == Material.SAND || material == Material.SNOW || material == Material.SNOW_BLOCK) && shovel != null) {
                    if (!(heldItem.getToolTypes().contains(ToolType.SHOVEL))) {
                        player.inventory.currentItem = player.inventory.getSlotFor(shovel);
                    }
                }
            }
        }
    }
}
