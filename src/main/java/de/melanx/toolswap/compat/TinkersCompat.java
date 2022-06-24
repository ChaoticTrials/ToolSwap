package de.melanx.toolswap.compat;

import de.melanx.toolswap.ToolSwap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import slimeknights.tconstruct.library.tools.definition.harvest.FixedTierHarvestLogic;
import slimeknights.tconstruct.library.tools.definition.harvest.IHarvestLogic;
import slimeknights.tconstruct.library.tools.definition.harvest.TagHarvestLogic;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;

import java.lang.reflect.Field;

public class TinkersCompat {

    private static final String MODID = "tconstruct";

    public static boolean tinkers(TagKey<Block> blocks, ItemStack stack) {
        if (stack.getItem() instanceof ModifiableItem item) {
            IHarvestLogic harvestLogic = item.getToolDefinition().getData().getHarvestLogic();
            if (harvestLogic instanceof TagHarvestLogic logic) {
                try {
                    Field tag = TagHarvestLogic.class.getDeclaredField("tag");
                    tag.setAccessible(true);
                    //noinspection unchecked
                    TagKey<Block> logicTag = (TagKey<Block>) tag.get(logic);
                    return logicTag.equals(blocks);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    ToolSwap.LOGGER.warn("Unable to access Tinkers tag", e);
                    return false;
                }
            }

            if (harvestLogic instanceof FixedTierHarvestLogic logic) {
                try {
                    Field tag = FixedTierHarvestLogic.class.getDeclaredField("tag");
                    tag.setAccessible(true);
                    //noinspection unchecked
                    TagKey<Block> logicTag = (TagKey<Block>) tag.get(logic);
                    return logicTag.equals(blocks);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    ToolSwap.LOGGER.warn("Unable to access Tinkers tag", e);
                    return false;
                }
            }

            ToolSwap.LOGGER.warn("Unhandled harvest logic caught: " + harvestLogic);
        }

        return false;
    }

    public static boolean isTinkersLoaded() {
        return ModList.get().isLoaded(MODID);
    }
}
