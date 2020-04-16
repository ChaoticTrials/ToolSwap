package de.melanx.toolswap;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(ToolSwap.MODID)
public class ToolSwap {

    public static final String MODID = "toolswap";
    public static final String MOD_NAME = "Automatic Tool Swap";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final KeyBinding toggle = new KeyBinding(MODID + ".key.toggle_toolswap_mode", GLFW.GLFW_KEY_G, MOD_NAME);
    public static ToolSwap instance;
    private boolean isOn = true;

    public ToolSwap() {
        instance = this;

        ClientRegistry.registerKeyBinding(toggle);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onWorldTick(TickEvent.PlayerTickEvent event) {
        if (toggle.isPressed()) {
            isOn = !isOn;
            TranslationTextComponent on_off;
            if (isOn) {
                TranslationTextComponent on = new TranslationTextComponent(MODID + ".key.toggle_toolswap_notification.state_on");
                on.setStyle(new Style().setColor(TextFormatting.GREEN));
                on_off = on;
            } else {
                TranslationTextComponent off = new TranslationTextComponent(MODID + ".key.toggle_toolswap_notification.state_off");
                off.setStyle(new Style().setColor(TextFormatting.DARK_RED));
                on_off = off;
            }
            TranslationTextComponent statusMessage = new TranslationTextComponent(MODID + ".key.toggle_toolswap_notification", isOn);
            statusMessage.appendText(": ").appendSibling(on_off);
            event.player.sendStatusMessage(statusMessage, true);
            LOGGER.debug("Set tool swap mode to " + isOn);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onBlockDestroy(PlayerEvent.BreakSpeed event) {
        if (isOn) {
            if (event.getEntity().getEntityWorld().getGameTime() % 3 != 0) return;

            BlockState state = event.getState();
            if (event.getEntity() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getEntity();
                ItemStack heldItem = player.getHeldItemMainhand();
                if (!player.isSneaking()) {
                    ItemStack axe = null;
                    ItemStack pickaxe = null;
                    ItemStack shovel = null;
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
}
