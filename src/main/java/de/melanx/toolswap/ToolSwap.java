package de.melanx.toolswap;

import de.melanx.toolswap.config.ClientConfig;
import de.melanx.toolswap.handler.EventHandler;
import de.melanx.toolswap.handler.ToggleHandler;
import de.melanx.toolswap.handler.ToggleState;
import de.melanx.toolswap.helper.ToolEntry;
import de.melanx.toolswap.helper.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mod(value = ToolSwap.MODID, dist = Dist.CLIENT)
public final class ToolSwap {

    private final ToggleState toggleState;
    private final ToggleHandler toggleHandler;

    public static final String MODID = "toolswap";
    public static final Logger LOGGER = LoggerFactory.getLogger(ToolSwap.class);
    private static ToolSwap INSTANCE;

    public ToolSwap(IEventBus modBus, ModContainer modContainer) {
        INSTANCE = this;
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_CONFIG);
        EventHandler.initialize(modBus, NeoForge.EVENT_BUS);
        this.toggleHandler = ToggleHandler.initialize();
        this.toggleState = ToggleState.initialize();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public static ToolSwap getInstance() {
        return INSTANCE;
    }

    public ToggleHandler getToggleHandler() {
        return this.toggleHandler;
    }

    public ToggleState getToggleState() {
        return this.toggleState;
    }

    public void searchForSwitching(Level level, Player player, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel) || !(player instanceof LocalPlayer localPlayer)) {
            return;
        }

        //noinspection ConstantConditions
        if (!Objects.equals(localPlayer.getGameProfile().getId(), Minecraft.getInstance().player.getGameProfile().getId())) {
            return;
        }

        ItemStack heldItem = localPlayer.getMainHandItem();
        Util.saveItem(localPlayer);

        if (!this.toggleState.shouldSwapTools() || Util.shouldIgnore(heldItem)) {
            return;
        }

        if (ClientConfig.sneakToPrevent.get() && localPlayer.isShiftKeyDown()) {
            return;
        }

        BlockState state = clientLevel.getBlockState(pos);

        List<ToolEntry> tools = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = localPlayer.getInventory().getItem(i);
            if (!Util.validItem(stack)) {
                continue;
            }

            Tool tool = stack.get(DataComponents.TOOL);
            if (tool == null || !tool.isCorrectForDrops(state)) {
                continue;
            }

            tools.add(new ToolEntry(tool, stack));
        }

        Util.sortTools(tools);

        if (this.toggleHandler.switchIfPossible(localPlayer, state, tools)) {
            return;
        }

        if (heldItem.has(DataComponents.MAX_DAMAGE)) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = localPlayer.getInventory().getItem(i);
                if (!stack.has(DataComponents.MAX_DAMAGE)) {
                    this.toggleHandler.switchTo(localPlayer, i);
                    return;
                }
            }
        }
    }
}
