package de.melanx.toolswap.handler;

import de.melanx.toolswap.SwapKeys;
import de.melanx.toolswap.ToolSwap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandler {

    private static EventHandler INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandler.class);

    private EventHandler(IEventBus modBus, IEventBus neoforgeBus) {
        neoforgeBus.addListener(this::leftClickBlock);
        neoforgeBus.addListener(this::onClientTick);
        neoforgeBus.addListener(this::onPlayerTick);
        neoforgeBus.addListener(this::onOpenScreen);

        modBus.addListener(this::onKeyRegistration);
    }

    public static void initialize(IEventBus modBus, IEventBus neoforgeBus) {
        if (INSTANCE == null) {
            INSTANCE = new EventHandler(modBus, neoforgeBus);
        }
    }

    private void onKeyRegistration(RegisterKeyMappingsEvent event) {
        event.register(SwapKeys.TOGGLE.get());
    }

    private void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            ToolSwap.getInstance().searchForSwitching(event.getLevel(), event.getEntity(), event.getPos());
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        while (SwapKeys.TOGGLE.get().consumeClick()) {
            ToolSwap.getInstance().getToggleState().toggleConfig();
            boolean shouldSwapTools = ToolSwap.getInstance().getToggleState().shouldSwapTools();

            MutableComponent state = Component.translatable(
                    ToolSwap.MODID + ".key.toggle_toolswap_notification.state_" + (shouldSwapTools ? "on" : "off")
            ).withStyle(Style.EMPTY.applyFormat(shouldSwapTools ? ChatFormatting.GREEN : ChatFormatting.DARK_RED));

            MutableComponent statusMessage = Component.translatable(ToolSwap.MODID + ".key.toggle_toolswap_notification", shouldSwapTools);
            statusMessage.append(": ").append(state);

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(statusMessage, true);
            }

            LOGGER.debug("Set tool swap mode to {}", shouldSwapTools);
        }
    }

    private void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer player && ToolSwap.getInstance().getToggleHandler().getSlot() != ToggleHandler.INVALID_SLOT && !Minecraft.getInstance().options.keyAttack.isDown()) {
            ToolSwap.getInstance().getToggleHandler().resetSlot(player);
        }
    }

    private void onOpenScreen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof PauseScreen) {
            ToolSwap.getInstance().getToggleState().save();
        }
    }
}
