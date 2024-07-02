package de.melanx.toolswap;

import net.minecraft.client.ToggleKeyMapping;
import net.neoforged.jarjar.nio.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class SwapKeys {

    public static final Lazy<ToggleKeyMapping> TOGGLE = Lazy.of(() -> new ToggleKeyMapping(
            ToolSwap.MODID + ".key.toggle_toolswap_mode",
            GLFW.GLFW_KEY_G,
            "Automatic Tool Swap",
            () -> false));
}
