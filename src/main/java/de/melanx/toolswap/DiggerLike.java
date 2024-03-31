package de.melanx.toolswap;

import net.minecraft.world.item.Tier;

public interface DiggerLike {

    Tier getTier();

    default int getHarvestLevel() {
        //noinspection deprecation
        return this.getTier().getLevel();
    }

    default float getEfficiency() {
        return this.getTier().getSpeed();
    }
}
