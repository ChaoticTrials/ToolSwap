package de.melanx.toolswap.handler;

import de.melanx.toolswap.ToolSwap;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ToggleState {

    private static final Path SWAP_STATE_FILE = FMLPaths.CONFIGDIR.get().resolve("." + ToolSwap.MODID);
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleState.class);
    private static ToggleState INSTANCE;

    private boolean shouldSwapTools = false;

    private ToggleState() {
        this.load();
    }

    public static ToggleState initialize() {
        if (INSTANCE == null) {
            INSTANCE = new ToggleState();
        }

        return INSTANCE;
    }

    private void load() {
        try {
            if (Files.notExists(SWAP_STATE_FILE)) {
                Files.writeString(SWAP_STATE_FILE, "true");
                this.shouldSwapTools = true;
                return;
            }

            String s = Files.readString(SWAP_STATE_FILE);
            this.shouldSwapTools = Boolean.parseBoolean(s);
        } catch (IOException e) {
            LOGGER.warn("Failed to load swap state", e);
        }
    }

    public void save() {
        try {
            Files.writeString(SWAP_STATE_FILE, String.valueOf(this.shouldSwapTools));
        } catch (IOException e) {
            LOGGER.warn("Failed to save swap state", e);
        }
    }

    public void toggleConfig() {
        this.shouldSwapTools = !this.shouldSwapTools;
    }

    public boolean shouldSwapTools() {
        return this.shouldSwapTools;
    }
}
