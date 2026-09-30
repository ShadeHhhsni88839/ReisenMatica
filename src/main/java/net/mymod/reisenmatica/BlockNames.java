package net.mymod.reisenmatica;

public class BlockNames {

    private static final String[] NAMES = new String[96];

    static {
        String[] n = {
                "Air", "Stone", "Grass Block", "Dirt", "Cobblestone", "Wood Planks", "Sapling", "Bedrock",
                "Water (flowing)", "Water", "Lava (flowing)", "Lava", "Sand", "Gravel", "Gold Ore", "Iron Ore",
                "Coal Ore", "Log", "Leaves", "Sponge", "Glass", "Lapis Ore", "Lapis Block", "Dispenser",
                "Sandstone", "Note Block", "Bed", "Powered Rail", "Detector Rail", "Sticky Piston", "Cobweb", "Tall Grass",
                "Dead Bush", "Piston", "Piston Head", "Wool", "Piston Extension", "Dandelion", "Rose", "Brown Mushroom",
                "Red Mushroom", "Gold Block", "Iron Block", "Double Slab", "Slab", "Bricks", "TNT", "Bookshelf",
                "Mossy Cobblestone", "Obsidian", "Torch", "Fire", "Monster Spawner", "Wood Stairs", "Chest", "Redstone Wire",
                "Diamond Ore", "Diamond Block", "Crafting Table", "Wheat", "Farmland", "Furnace", "Furnace (lit)", "Sign",
                "Wood Door", "Ladder", "Rail", "Cobblestone Stairs", "Wall Sign", "Lever", "Stone Pressure Plate", "Iron Door",
                "Wood Pressure Plate", "Redstone Ore", "Redstone Ore (lit)", "Redstone Torch (off)", "Redstone Torch", "Stone Button", "Snow Layer", "Ice",
                "Snow Block", "Cactus", "Clay Block", "Sugar Cane", "Jukebox", "Fence", "Pumpkin", "Netherrack",
                "Soul Sand", "Glowstone", "Portal", "Jack o'Lantern", "Cake", "Repeater (off)", "Repeater (on)", "Locked Chest"
        };
        System.arraycopy(n, 0, NAMES, 0, Math.min(n.length, NAMES.length));
    }

    public static String name(int id) {
        if (id >= 0 && id < NAMES.length && NAMES[id] != null) {
            return NAMES[id];
        }
        return "Block #" + id;
    }
}