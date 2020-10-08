package de.melanx.toolswap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
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
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mod(ToolSwap.MODID)
public class ToolSwap {

    public static final String MODID = "toolswap";
    public static final String MOD_NAME = "Automatic Tool Swap";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final KeyBinding toggle = new KeyBinding(MODID + ".key.toggle_toolswap_mode", GLFW.GLFW_KEY_G, MOD_NAME);
    private static final File config = FMLPaths.CONFIGDIR.get().resolve("." + MODID).toFile();

    public ToolSwap() {
        ClientRegistry.registerKeyBinding(toggle);
        MinecraftForge.EVENT_BUS.register(this);
        try {
            if (!config.exists()) {
                config.createNewFile();
            }
            boolean isOn = !getContent().equals("0");
            FileWriter writer = new FileWriter(config);
            if (isOn) {
                writer.write("1");
            } else {
                writer.write("0");
            }
            writer.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onWorldTick(TickEvent.PlayerTickEvent event) {
        if (toggle.isPressed()) {
            toggleMode();
            TranslationTextComponent on_off;
            if (isOn()) {
                TranslationTextComponent on = new TranslationTextComponent(MODID + ".key.toggle_toolswap_notification.state_on");
                on.mergeStyle(Style.EMPTY.setFormatting(TextFormatting.GREEN));
                on_off = on;
            } else {
                TranslationTextComponent off = new TranslationTextComponent(MODID + ".key.toggle_toolswap_notification.state_off");
                off.mergeStyle(Style.EMPTY.setFormatting(TextFormatting.DARK_RED));
                on_off = off;
            }
            TranslationTextComponent statusMessage = new TranslationTextComponent(MODID + ".key.toggle_toolswap_notification", isOn());
            statusMessage.appendString(": ").append(on_off);
            event.player.sendStatusMessage(statusMessage, true);
            LOGGER.debug("Set tool swap mode to " + isOn());
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onBlockDestroy(PlayerEvent.BreakSpeed event) {
        if (isOn()) {
            if (event.getEntity().getEntityWorld().getGameTime() % 3 != 0) return;

            HashMap<ToolType, ItemStack> tools = new HashMap<>();
            BlockState state = event.getState();
            Block block = state.getBlock();
            if (event.getEntity() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getEntity();
                ItemStack heldItem = player.getHeldItemMainhand();
                if (!player.isCrouching()) {
                    if (heldItem.getToolTypes().contains(block.getHarvestTool(state))) return;
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = player.inventory.getStackInSlot(i);
                        if (stack.getToolTypes().contains(ToolType.AXE)) {
                            tools.put(ToolType.AXE, stack);
                        } else if (stack.getToolTypes().contains(ToolType.PICKAXE)) {
                            tools.put(ToolType.PICKAXE, stack);
                        } else if (stack.getToolTypes().contains(ToolType.SHOVEL)) {
                            tools.put(ToolType.SHOVEL, stack);
                        } else if (stack.getToolTypes().contains(ToolType.HOE)) {
                            tools.put(ToolType.HOE, stack);
                        }
                    }

                    if (tools.isEmpty()) return;
                    ToolType toolType = block.getHarvestTool(state);

                    if (toolType == null) {
                        float blockHardness = state.getBlockHardness(player.getEntityWorld(), event.getPos());
                        if (blockHardness > 0) {
                            for (Map.Entry<ToolType, ItemStack> entry : tools.entrySet()) {
                                ToolItem toolItem = (ToolItem) entry.getValue().getItem();
                                if (entry.getValue().getDestroySpeed(state) >= toolItem.getTier().getEfficiency()) {
                                    toolType = entry.getKey();
                                }
                            }
                        }
                    }

                    if (toolType != null) {
                        for (Map.Entry<ToolType, ItemStack> entry : tools.entrySet()) {
                            if (entry.getKey() == toolType && state.getHarvestLevel() <= ((ToolItem) entry.getValue().getItem()).getTier().getHarvestLevel()) {
                                player.inventory.currentItem = player.inventory.getSlotFor(entry.getValue());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void toggleMode() {
        try {
            FileInputStream stream = new FileInputStream(config);
            String setting = IOUtils.toString(stream);
            FileWriter writer = new FileWriter(config);
            if (setting.equals("1")) {
                writer.write("0");
            } else {
                writer.write("1");
            }
            writer.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }

    private boolean isOn() {
        return getContent().equals("1");
    }

    private String getContent() {
        try {
            FileInputStream stream = new FileInputStream(config);
            String setting = IOUtils.toString(stream);
            return setting.trim();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
        return "";
    }
}
