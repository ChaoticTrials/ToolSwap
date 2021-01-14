package de.melanx.toolswap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientToolSwap {

    public static final Logger LOGGER = LogManager.getLogger(ClientToolSwap.class);
    public static final String MOD_NAME = "Automatic Tool Swap";
    public static final ITextComponent WARNING = getWarningComponent();
    public static final KeyBinding toggle = new KeyBinding(ToolSwap.MODID + ".key.toggle_toolswap_mode", GLFW.GLFW_KEY_G, MOD_NAME);
    private static final ImmutableList<ToolType> TOOL_TYPES = ImmutableList.of(ToolType.AXE, ToolType.HOE, ToolType.PICKAXE, ToolType.SHOVEL);
    private static final File config = FMLPaths.CONFIGDIR.get().resolve("." + ToolSwap.MODID).toFile();
    private static int prevSlot = -1;
    private static int cooldown = 0;
    private static boolean toggleState = false;
    
    public ClientToolSwap() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_CONFIG);
        ClientConfig.loadConfig(ClientConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(ToolSwap.MODID + "-client.toml"));
        ClientRegistry.registerKeyBinding(toggle);
        MinecraftForge.EVENT_BUS.register(this);
        try {
            if (!config.exists()) {
                config.createNewFile();
            }
            toggleState = !getContent().equals("0");
            FileWriter writer = new FileWriter(config);
            if (toggleState) {
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
            if (toggleState) {
                TranslationTextComponent on = new TranslationTextComponent(ToolSwap.MODID + ".key.toggle_toolswap_notification.state_on");
                on.mergeStyle(Style.EMPTY.setFormatting(TextFormatting.GREEN));
                on_off = on;
            } else {
                TranslationTextComponent off = new TranslationTextComponent(ToolSwap.MODID + ".key.toggle_toolswap_notification.state_off");
                off.mergeStyle(Style.EMPTY.setFormatting(TextFormatting.DARK_RED));
                on_off = off;
            }
            TranslationTextComponent statusMessage = new TranslationTextComponent(ToolSwap.MODID + ".key.toggle_toolswap_notification", toggleState);
            statusMessage.appendString(": ").append(on_off);
            event.player.sendStatusMessage(statusMessage, true);
            LOGGER.debug("Set tool swap mode to " + toggleState);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onBlockDestroy(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            ItemStack heldItem = player.getHeldItemMainhand();
            if (toolAboutBreaking(heldItem)) {
                saveItem(player);
            }
            if (toggleState) {
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
                        List<ToolEntry> finalToolList = new ArrayList<>();
                        switch (ClientConfig.sortType.get()) {
                            case LEVEL:
                                tools.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                                finalToolList = tools;
                                break;
                            case LEVEL_INVERTED:
                                tools.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                                finalToolList = Lists.reverse(tools);
                                break;
                            case RIGHT_TO_LEFT:
                                finalToolList = Lists.reverse(tools);
                                break;
                            case ENCHANTED_FIRST:
                                List<ToolEntry> enchanted = new ArrayList<>();
                                List<ToolEntry> unenchanted = new ArrayList<>();
                                tools.forEach(toolEntry -> {
                                    if (toolEntry.getStack().isEnchanted()) {
                                        enchanted.add(toolEntry);
                                    } else {
                                        unenchanted.add(toolEntry);
                                    }
                                });
                                enchanted.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                                finalToolList.addAll(Lists.reverse(enchanted));
                                unenchanted.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                                finalToolList.addAll(Lists.reverse(unenchanted));
                                break;
                            case ENCHANTED_LAST:
                                List<ToolEntry> enchanted1 = new ArrayList<>();
                                List<ToolEntry> unenchanted1 = new ArrayList<>();
                                tools.forEach(toolEntry -> {
                                    if (toolEntry.getStack().isEnchanted()) {
                                        enchanted1.add(toolEntry);
                                    } else {
                                        unenchanted1.add(toolEntry);
                                    }
                                });
                                unenchanted1.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                                finalToolList.addAll(Lists.reverse(unenchanted1));
                                enchanted1.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                                finalToolList.addAll(Lists.reverse(enchanted1));
                                break;
                            default: // LEFT_TO_RIGHT
                                finalToolList = tools;
                                break;
                        }


                        if (finalToolList.isEmpty()) return;
                        ToolType toolType = block.getHarvestTool(state);
                        if (prevSlot == -1) {
                            prevSlot = player.inventory.currentItem;
                        }

                        if (toolType == null) {
                            float blockHardness = state.getBlockHardness(player.getEntityWorld(), event.getPos());
                            if (blockHardness > 0) {
                                for (ToolEntry entry : finalToolList) {
                                    if (entry.getStack().getDestroySpeed(state) >= entry.getEfficiency()) {
                                        toolType = entry.getType();
                                    }
                                }
                            }
                        }

                        if (toolType != null) {
                            for (ToolEntry entry : finalToolList) {
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
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (prevSlot != -1 && event.side.isClient() && !Minecraft.getInstance().gameSettings.keyBindAttack.isKeyDown()) {
            resetCurrentSlot(player);
        }
    }

    private boolean toolAboutBreaking(ItemStack stack) {
        return ClientConfig.saveBreakingTools.get() && stack.isDamageable() && stack.getDamage() == stack.getMaxDamage() - 1;
    }

    private static void saveItem(PlayerEntity player) {
        PlayerController controller = Minecraft.getInstance().playerController;
        Container container = player.openContainer;
        int emptySlot = -1;
        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber > 9 && slot.getStack().isEmpty()) {
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

    private static void resetCurrentSlot(PlayerEntity player) {
        if (prevSlot >= 0) {
            player.inventory.currentItem = prevSlot;
            prevSlot = -1;
            cooldown = 5;
        }
    }

    private static void toggleMode() {
        try {
            FileInputStream stream = new FileInputStream(config);
            String setting = IOUtils.toString(stream);
            FileWriter writer = new FileWriter(config);
            if (setting.equals("1")) {
                writer.write("0");
                toggleState = false;
            } else {
                writer.write("1");
                toggleState = true;
            }
            writer.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
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
        TranslationTextComponent component = new TranslationTextComponent(ToolSwap.MODID + ".warning");
        component.mergeStyle(TextFormatting.DARK_RED);
        return component;
    }
}
