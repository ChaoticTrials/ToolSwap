package de.melanx.toolswap;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.util.Lazy;

public class SwapKeys {

    public static final KeyMapping.Category TOOLSWAP_KEYS = new KeyMapping.Category(Identifier.fromNamespaceAndPath(ToolSwap.MODID, "keys"));

    public static final Lazy<ToggleKeyMapping> TOGGLE = Lazy.of(() -> new ToggleKeyMapping(
            ToolSwap.MODID + ".key.toggle_toolswap_mode",
            InputConstants.KEY_G,
            TOOLSWAP_KEYS,
            () -> false,
            false));
}
