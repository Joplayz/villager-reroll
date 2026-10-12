package com.joplayx.client.util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
public enum Workstation {
    LECTERN("lectern", "librarian", Blocks.LECTERN),
    COMPOSTER("composter", "farmer", Blocks.COMPOSTER),
    BARREL("barrel", "fisherman", Blocks.BARREL),
    FLETCHING_TABLE("fletching_table", "fletcher", Blocks.FLETCHING_TABLE),
    LOOM("loom", "shepherd", Blocks.LOOM),
    BLAST_FURNACE("blast_furnace", "armorer", Blocks.BLAST_FURNACE),
    SMITHING_TABLE("smithing_table", "toolsmith", Blocks.SMITHING_TABLE),
    GRINDSTONE("grindstone", "weaponsmith", Blocks.GRINDSTONE),
    BREWING_STAND("brewing_stand", "cleric", Blocks.BREWING_STAND),
    CAULDRON("cauldron", "leatherworker", Blocks.CAULDRON),
    STONECUTTER("stonecutter", "mason", Blocks.STONECUTTER),
    CARTOGRAPHY_TABLE("cartography_table", "cartographer", Blocks.CARTOGRAPHY_TABLE),
    SMOKER("smoker", "butcher", Blocks.SMOKER);
    public final String id;
    public final String profession;
    public final Block block;
    Workstation(String id, String profession, Block block) {
        this.id = id; this.profession = profession; this.block = block;
    }
    public static Workstation from(String id) {
        for (var value : values()) if (value.id.equals(id)) return value;
        return LECTERN;
    }
}
