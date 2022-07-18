package de.melanx.toolswap;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientToolSwap {

    public static final Logger LOGGER = LogManager.getLogger(ClientToolSwap.class);
    public static final ToggleKeyMapping TOGGLE = new ToggleKeyMapping(ToolSwap.MODID + ".key.toggle_toolswap_mode", GLFW.GLFW_KEY_G, "Automatic Tool Swap", () -> false);
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("." + ToolSwap.MODID).toFile();
    private static int PREV_SLOT = -1;
    private static boolean TOGGLE_STATE = false;
    public static MutableComponent WARNING;

    static {
        WARNING = Component.translatable(ToolSwap.MODID + ".warning");
        WARNING.withStyle(ChatFormatting.DARK_RED);
    }

    public ClientToolSwap() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_CONFIG);
        ClientConfig.loadConfig(ClientConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(ToolSwap.MODID + "-client.toml"));
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onKeyRegistration);
        try {
            TOGGLE_STATE = !ClientToolSwap.getContent().equals("0");
            FileWriter writer = new FileWriter(CONFIG_FILE);
            writer.write(TOGGLE_STATE ? "1" : "0");
            writer.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void onKeyRegistration(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void keyInput(InputEvent.Key event) {
        if (event.getKey() == TOGGLE.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            ClientToolSwap.handleInput();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void mouseInput(InputEvent.MouseButton event) {
        if (event.getButton() == TOGGLE.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            ClientToolSwap.handleInput();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof LocalPlayer player && PREV_SLOT != -1 && event.side.isClient() && !Minecraft.getInstance().options.keyAttack.isDown()) {
            this.resetCurrentSlot(player);
        }
    }

    private static void handleInput() {
        ClientToolSwap.toggleMode();
        MutableComponent on_off;
        if (TOGGLE_STATE) {
            MutableComponent on = Component.translatable(ToolSwap.MODID + ".key.toggle_toolswap_notification.state_on");
            on.withStyle(Style.EMPTY.applyFormat(ChatFormatting.GREEN));
            on_off = on;
        } else {
            MutableComponent off = Component.translatable(ToolSwap.MODID + ".key.toggle_toolswap_notification.state_off");
            off.withStyle(Style.EMPTY.applyFormat(ChatFormatting.DARK_RED));
            on_off = off;
        }
        MutableComponent statusMessage = Component.translatable(ToolSwap.MODID + ".key.toggle_toolswap_notification", TOGGLE_STATE);
        statusMessage.append(": ").append(on_off);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(statusMessage, true);
        }
        LOGGER.debug("Set tool swap mode to " + TOGGLE_STATE);
    }

    public static void searchForSwitching(MultiPlayerGameMode multiPlayerGameMode, BlockPos pos) {
        ClientLevel level = multiPlayerGameMode.minecraft.level;
        LocalPlayer player = multiPlayerGameMode.minecraft.player;
        if (level == null || player == null) {
            return;
        }

        //noinspection ConstantConditions
        if (!Objects.equals(player.getGameProfile().getId(), Minecraft.getInstance().player.getGameProfile().getId())) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (ClientToolSwap.toolAboutBreaking(heldItem)) {
            ClientToolSwap.saveItem(player);
        }

        if (TOGGLE_STATE) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (!ClientConfig.sneakToPrevent.get() || !player.isShiftKeyDown()) {
                List<ToolEntry> tools = Lists.newArrayList();
                List<ItemStack> swords = Lists.newArrayList();
                List<ItemStack> shears = Lists.newArrayList();

                Set<TagKey<Block>> toolTypes = state.getTags()
                        .filter(tag -> tag.location().getPath().startsWith("mineable/"))
                        .collect(Collectors.toSet());
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (ClientToolSwap.toolAboutBreaking(stack)) continue;
                    for (TagKey<Block> type : toolTypes) {
                        if (stack.getItem() instanceof DiggerItem && type.location() == ((DiggerItem) stack.getItem()).blocks.location()) {
                            if (heldItem == stack && ClientConfig.ignoreHarvestLevel.get() && !state.is(Blocks.COBWEB)) {
                                return;
                            }
                            tools.add(new ToolEntry(type, stack));
                        }
                    }
                    if (stack.getItem() instanceof SwordItem) {
                        swords.add(stack);
                    }
                    if (stack.is(Tags.Items.SHEARS)) {
                        shears.add(stack);
                    }
                }
                List<ToolEntry> finalToolList = Lists.newArrayList();
                switch (ClientConfig.sortType.get()) {
                    case LEVEL -> {
                        tools.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                        finalToolList = tools;
                    }
                    case LEVEL_INVERTED -> {
                        tools.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                        finalToolList = Lists.reverse(tools);
                    }
                    case RIGHT_TO_LEFT -> {
                        finalToolList = Lists.reverse(tools);
                        swords = Lists.reverse(swords);
                        shears = Lists.reverse(shears);
                    }
                    case LEFT_TO_RIGHT -> finalToolList = tools;
                    //noinspection DuplicatedCode
                    case ENCHANTED_FIRST -> {
                        List<ToolEntry> enchanted = Lists.newArrayList();
                        List<ToolEntry> unenchanted = Lists.newArrayList();
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
                    }
                    //noinspection DuplicatedCode
                    case ENCHANTED_LAST -> {
                        List<ToolEntry> enchanted = Lists.newArrayList();
                        List<ToolEntry> unenchanted = Lists.newArrayList();
                        tools.forEach(toolEntry -> {
                            if (toolEntry.getStack().isEnchanted()) {
                                enchanted.add(toolEntry);
                            } else {
                                unenchanted.add(toolEntry);
                            }
                        });
                        unenchanted.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                        finalToolList.addAll(Lists.reverse(unenchanted));
                        enchanted.sort(Comparator.comparingInt(ToolEntry::getHarvestLevel));
                        finalToolList.addAll(Lists.reverse(enchanted));
                    }
                }

                if (state.is(Blocks.COBWEB)) {
                    if (swords.isEmpty()) {
                        return;
                    }

                    if (PREV_SLOT == -1) {
                        PREV_SLOT = player.getInventory().selected;
                    }
                    ClientToolSwap.switchTo(player, swords.get(0));
                    return;
                }

                if (state.is(BlockTags.WOOL)) {
                    if (shears.isEmpty()) {
                        return;
                    }

                    if (PREV_SLOT == -1) {
                        PREV_SLOT = player.getInventory().selected;
                    }
                    ClientToolSwap.switchTo(player, shears.get(0));
                    return;
                }

                if (finalToolList.isEmpty()) return;
                //noinspection deprecation
                Set<ResourceLocation> mineables = Registry.BLOCK.getHolderOrThrow(block.builtInRegistryHolder().key()).tags()
                        .map(TagKey::location)
                        .filter(location -> location.getPath().startsWith("mineable/"))
                        .collect(Collectors.toSet());
                if (PREV_SLOT == -1) {
                    PREV_SLOT = player.getInventory().selected;
                }

                if (mineables.isEmpty()) {
                    float blockHardness = state.getDestroySpeed(player.level, pos);
                    if (blockHardness > 0) {
                        for (ToolEntry entry : finalToolList) {
                            if (entry.getStack().getDestroySpeed(state) >= entry.getEfficiency()) {
                                ResourceLocation id = entry.getType().location();
                                mineables.add(id);
                            }
                        }
                    }
                }

                if (!mineables.isEmpty()) {
                    for (ToolEntry entry : finalToolList) {
                        for (ResourceLocation id : mineables) {
                            //noinspection ConstantConditions
                            if (Objects.equals(entry.getType().location(), id) && TierSortingRegistry.isCorrectTierForDrops(entry.getToolItem().getTier(), state)) {
                                ClientToolSwap.switchTo(player, entry.getStack());
                                return;
                            }
                        }
                    }
                }

                if (heldItem.getItem().canBeDepleted()) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.getItem().canBeDepleted()) {
                            ClientToolSwap.switchTo(player, i);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void switchTo(LocalPlayer player, ItemStack stack) {
        ClientToolSwap.switchTo(player, player.getInventory().findSlotMatchingItem(stack));
    }

    private static void switchTo(LocalPlayer player, int slotId) {
        if (player.getInventory().selected == slotId) {
            return;
        }

        player.getInventory().selected = slotId;
    }

    private static boolean toolAboutBreaking(ItemStack stack) {
        return ClientConfig.saveBreakingTools.get() && stack.isDamageableItem() && stack.getDamageValue() == stack.getMaxDamage() - ClientConfig.minDurability.get();
    }

    private static void saveItem(Player player) {
        MultiPlayerGameMode controller = Minecraft.getInstance().gameMode;
        AbstractContainerMenu container = player.containerMenu;
        int emptySlot = -1;

        ItemStack currentTool = player.getInventory().getItem(player.getInventory().selected);
        ItemStack equalTool = ClientToolSwap.findEqualTool(player.getInventory(), currentTool);
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
        } else {
            player.displayClientMessage(WARNING, true);
        }
    }

    private static ItemStack findEqualTool(Inventory inventory, ItemStack stack) {
        if ((stack.getItem() instanceof DiggerItem item)) {
            //noinspection deprecation
            Iterable<Holder<Block>> tagOrEmpty = Registry.BLOCK.getTagOrEmpty(item.blocks);
            if (!tagOrEmpty.iterator().hasNext()) {
                return stack;
            }

            for (ItemStack tool : inventory.items) {
                if (tool.sameItemStackIgnoreDurability(stack) && !ClientToolSwap.toolAboutBreaking(tool)) {
                    return tool;
                }
            }
        }

        return stack;
    }

    private void resetCurrentSlot(LocalPlayer player) {
        if (PREV_SLOT >= 0) {
            ClientToolSwap.switchTo(player, PREV_SLOT);
            PREV_SLOT = -1;
        }
    }

    private static void toggleMode() {
        try {
            FileInputStream stream = new FileInputStream(CONFIG_FILE);
            String setting = IOUtils.toString(stream, Charset.defaultCharset());
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
            String setting = IOUtils.toString(stream, StandardCharsets.UTF_8);
            return setting.trim();
        } catch (IOException e) {
            LOGGER.warn(e);
        }

        return "";
    }
}
