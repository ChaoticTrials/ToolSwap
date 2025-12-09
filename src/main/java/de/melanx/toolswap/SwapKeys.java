package de.melanx.toolswap;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class SwapKeys {

    public static final KeyMapping.Category TOOLSWAP_KEYS = new KeyMapping.Category(Identifier.fromNamespaceAndPath(ToolSwap.MODID, "toolswap_keys"));

    public static final Lazy<ToggleKeyMapping> TOGGLE = Lazy.of(() -> new ToggleKeyMapping(
            ToolSwap.MODID + ".key.toggle_toolswap_mode",
            GLFW.GLFW_KEY_G,
            TOOLSWAP_KEYS,
            () -> false,
            false));
}
