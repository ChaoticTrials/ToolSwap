package de.melanx.toolswap;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.util.text.ITextComponent;
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
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Mod(ToolSwap.MODID)
public class ToolSwap {

    public static final String MODID = "toolswap";
    public static final String MOD_NAME = "Automatic Tool Swap";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final ITextComponent WARNING = getWarningComponent();
    public static final KeyBinding toggle = new KeyBinding(MODID + ".key.toggle_toolswap_mode", GLFW.GLFW_KEY_G, MOD_NAME);
    private static final ImmutableList<ToolType> TOOL_TYPES = ImmutableList.of(ToolType.AXE, ToolType.HOE, ToolType.PICKAXE, ToolType.SHOVEL);
    private static final File config = FMLPaths.CONFIGDIR.get().resolve("." + MODID).toFile();
    private static int prevSlot = -1;
    private static int cooldown = 0;

    public ToolSwap() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_CONFIG);
        ClientConfig.loadConfig(ClientConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID + "-client.toml"));
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
    public void setup(FMLCommonSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
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
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            ItemStack heldItem = player.getHeldItemMainhand();
            if (toolAboutBreaking(heldItem)) {
                PlayerController controller = Minecraft.getInstance().playerController;
                Container container = player.openContainer;
                int emptySlot = -1;
                for (Slot slot : container.inventorySlots) {
                    if (slot.slotNumber > 8 && slot.getStack().isEmpty()) {
                        emptySlot = slot.slotNumber;
                    }
                }

                if (emptySlot != -1) {
                    controller.windowClick(container.windowId, player.inventory.currentItem + 36, 0, ClickType.PICKUP, player);
                    controller.windowClick(container.windowId, emptySlot, 0, ClickType.PICKUP, player);
                } else {
                    player.sendStatusMessage(WARNING, true);
                }
            }
            if (isOn()) {
                if (cooldown <= 0) {
                    List<ToolEntry> tools = new ArrayList<>();
                    BlockState state = event.getState();
                    Block block = state.getBlock();
                    if (!player.isCrouching()) {
                        if (heldItem.getToolTypes().contains(block.getHarvestTool(state)) &&
                                (ClientConfig.ignoreHarvestLevel.get() || heldItem.getItem() instanceof ToolItem &&
                                ((ToolItem) heldItem.getItem()).getTier().getHarvestLevel() == state.getHarvestLevel()))
                            return;

                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = player.inventory.getStackInSlot(i);
                            if (toolAboutBreaking(stack)) continue;
                            TOOL_TYPES.forEach(type -> {
                                if (stack.getToolTypes().contains(type)) {
                                    tools.add(new ToolEntry(type, stack));
                                }
                            });
                        }
                        tools.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));

                        if (tools.isEmpty()) return;
                        ToolType toolType = block.getHarvestTool(state);
                        if (prevSlot == -1) {
                            prevSlot = player.inventory.currentItem;
                        }

                        if (toolType == null) {
                            float blockHardness = state.getBlockHardness(player.getEntityWorld(), event.getPos());
                            if (blockHardness > 0) {
                                for (ToolEntry entry : tools) {
                                    if (entry.getStack().getDestroySpeed(state) >= entry.getEfficiency()) {
                                        toolType = entry.getType();
                                    }
                                }
                            }
                        }

                        if (toolType != null) {
                            for (ToolEntry entry : tools) {
                                if (entry.getType() == toolType && state.getHarvestLevel() <= entry.getHarvestLevel()) {
                                    player.inventory.currentItem = player.inventory.getSlotFor(entry.getStack());
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    cooldown--;
                }
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onBlockBreak(TickEvent.PlayerTickEvent event) {
        if (prevSlot != -1 && event.side.isClient() && !Minecraft.getInstance().gameSettings.keyBindAttack.isKeyDown()) {
            event.player.inventory.currentItem = prevSlot;
            prevSlot = -1;
            cooldown = 5;
        }
    }

    private boolean toolAboutBreaking(ItemStack stack) {
        return ClientConfig.saveBreakingTools.get() && stack.isDamageable() && stack.getDamage() == stack.getMaxDamage() - 1;
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

    private static ITextComponent getWarningComponent() {
        TranslationTextComponent component = new TranslationTextComponent(MODID + ".warning");
        component.mergeStyle(TextFormatting.DARK_RED);
        return component;
    }
}
