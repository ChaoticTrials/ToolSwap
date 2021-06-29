package de.melanx.toolswap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
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
    public static final KeyBinding TOGGLE = new KeyBinding(ToolSwap.MODID + ".key.toggle_toolswap_mode", GLFW.GLFW_KEY_G, "Automatic Tool Swap");
    private static final ImmutableList<ToolType> TOOL_TYPES = ImmutableList.of(ToolType.AXE, ToolType.HOE, ToolType.PICKAXE, ToolType.SHOVEL);
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("." + ToolSwap.MODID).toFile();
    private static int PREV_SLOT = -1;
    private static boolean TOGGLE_STATE = false;
    public static TranslationTextComponent WARNING;

    static {
        WARNING = new TranslationTextComponent(ToolSwap.MODID + ".warning");
        WARNING.mergeStyle(TextFormatting.DARK_RED);
    }

    public ClientToolSwap() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_CONFIG);
        ClientConfig.loadConfig(ClientConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(ToolSwap.MODID + "-client.toml"));
        ClientRegistry.registerKeyBinding(TOGGLE);
        MinecraftForge.EVENT_BUS.register(this);
        try {
            if (!CONFIG_FILE.exists()) {
                //noinspection ResultOfMethodCallIgnored
                CONFIG_FILE.createNewFile();
            }
            TOGGLE_STATE = !ClientToolSwap.getContent().equals("0");
            FileWriter writer = new FileWriter(CONFIG_FILE);
            if (TOGGLE_STATE) {
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
    public void onWorldTick(TickEvent.ClientTickEvent event) {
        if (TOGGLE.isPressed()) {
            ClientToolSwap.toggleMode();
            TranslationTextComponent on_off;
            if (TOGGLE_STATE) {
                TranslationTextComponent on = new TranslationTextComponent(ToolSwap.MODID + ".key.toggle_toolswap_notification.state_on");
                on.mergeStyle(Style.EMPTY.setFormatting(TextFormatting.GREEN));
                on_off = on;
            } else {
                TranslationTextComponent off = new TranslationTextComponent(ToolSwap.MODID + ".key.toggle_toolswap_notification.state_off");
                off.mergeStyle(Style.EMPTY.setFormatting(TextFormatting.DARK_RED));
                on_off = off;
            }
            TranslationTextComponent statusMessage = new TranslationTextComponent(ToolSwap.MODID + ".key.toggle_toolswap_notification", TOGGLE_STATE);
            statusMessage.appendString(": ").append(on_off);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendStatusMessage(statusMessage, true);
            }
            LOGGER.debug("Set tool swap mode to " + TOGGLE_STATE);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onBlockDestroy(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            ItemStack heldItem = player.getHeldItemMainhand();
            if (ClientToolSwap.toolAboutBreaking(heldItem)) {
                ClientToolSwap.saveItem(player);
            }

            if (TOGGLE_STATE) {
                List<ToolEntry> tools = new ArrayList<>();
                List<ItemStack> swords = new ArrayList<>();
                BlockState state = event.getState();
                Block block = state.getBlock();
                if (!player.isCrouching()) {
                    if (!state.isIn(Blocks.COBWEB) &&
                            (heldItem.getToolTypes().contains(block.getHarvestTool(state))
                                    && (ClientConfig.ignoreHarvestLevel.get()
                                    || heldItem.getItem() instanceof ToolItem
                                    && ((ToolItem) heldItem.getItem()).getTier().getHarvestLevel() == state.getHarvestLevel()))) {
                        return;
                    }

                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = player.inventory.getStackInSlot(i);
                        if (ClientToolSwap.toolAboutBreaking(stack)) continue;
                        TOOL_TYPES.forEach(type -> {
                            if (stack.getToolTypes().contains(type)) {
                                tools.add(new ToolEntry(type, stack));
                            }
                        });
                        if (stack.getItem() instanceof SwordItem) {
                            swords.add(stack);
                        }
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
                            swords = Lists.reverse(swords);
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

                    if (state.isIn(Blocks.COBWEB)) {
                        if (swords.isEmpty()) {
                            return;
                        }

                        if (PREV_SLOT == -1) {
                            PREV_SLOT = player.inventory.currentItem;
                        }
                        player.inventory.currentItem = player.inventory.getSlotFor(swords.get(0));
                        return;
                    }

                    if (finalToolList.isEmpty()) return;
                    ToolType toolType = block.getHarvestTool(state);
                    if (PREV_SLOT == -1) {
                        PREV_SLOT = player.inventory.currentItem;
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
                                return;
                            }
                        }
                    }

                    if (heldItem.getItem().isDamageable()) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = player.inventory.getStackInSlot(i);
                            if (!stack.getItem().isDamageable()) {
                                player.inventory.currentItem = i;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (PREV_SLOT != -1 && event.side.isClient() && !Minecraft.getInstance().gameSettings.keyBindAttack.isKeyDown()) {
            resetCurrentSlot(player);
        }
    }

    private static boolean toolAboutBreaking(ItemStack stack) {
        return ClientConfig.saveBreakingTools.get() && stack.isDamageable() && stack.getDamage() == stack.getMaxDamage() - ClientConfig.minDurability.get();
    }

    private static void saveItem(PlayerEntity player) {
        PlayerController controller = Minecraft.getInstance().playerController;
        Container container = player.openContainer;
        int emptySlot = -1;

        ItemStack currentTool = player.inventory.getStackInSlot(player.inventory.currentItem);
        ItemStack equalTool = ClientToolSwap.findEqualTool(player.inventory, currentTool);
        if (currentTool != equalTool) {
            emptySlot = player.inventory.getSlotFor(equalTool);
        }

        if (emptySlot == -1) {
            for (Slot slot : container.inventorySlots) {
                if (slot.slotNumber > 9 && slot.getStack().isEmpty()) {
                    emptySlot = slot.slotNumber;
                    break;
                }
            }
        }

        if (emptySlot != -1) {
            //noinspection ConstantConditions
            controller.windowClick(container.windowId, player.inventory.currentItem + 36, 0, ClickType.PICKUP, player);
            controller.windowClick(container.windowId, emptySlot, 0, ClickType.PICKUP, player);
        } else {
            player.sendStatusMessage(WARNING, true);
        }
    }

    private static ItemStack findEqualTool(PlayerInventory inventory, ItemStack stack) {
        if (stack.getItem().getToolTypes(stack).isEmpty()) {
            return stack;
        }

        for (ItemStack item : inventory.mainInventory) {
            if (item.isItemEqualIgnoreDurability(stack) && !ClientToolSwap.toolAboutBreaking(item)) {
                return item;
            }
        }

        return stack;
    }

    private static void resetCurrentSlot(PlayerEntity player) {
        if (PREV_SLOT >= 0) {
            player.inventory.currentItem = PREV_SLOT;
            PREV_SLOT = -1;
        }
    }

    private static void toggleMode() {
        try {
            FileInputStream stream = new FileInputStream(CONFIG_FILE);
            String setting = IOUtils.toString(stream);
            FileWriter writer = new FileWriter(CONFIG_FILE);
            if (setting.equals("1")) {
                writer.write("0");
                TOGGLE_STATE = false;
            } else {
                writer.write("1");
                TOGGLE_STATE = true;
            }
            writer.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }

    private static String getContent() {
        try {
            FileInputStream stream = new FileInputStream(CONFIG_FILE);
            String setting = IOUtils.toString(stream);
            return setting.trim();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
        return "";
    }
}
