package net.mymod.reisenmatica;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public final class WorldBridge {

    private WorldBridge() {
    }

    public static int getBlockId(World world, int x, int y, int z) {
        return world.getBlockId(x, y, z);
    }

    public static int getBlockMeta(World world, int x, int y, int z) {
        return world.getBlockMeta(x, y, z);
    }

    public static void setBlock(World world, int x, int y, int z, int id, int meta) {
        world.setBlock(x, y, z, id);
        if (meta != 0) {
            world.setBlockMeta(x, y, z, meta);
        }
    }

    public static NbtCompound getBlockEntityNbt(World world, int x, int y, int z) {
        BlockEntity be = world.getBlockEntity(x, y, z);
        if (be == null) {
            return null;
        }
        NbtCompound tag = new NbtCompound();
        be.writeNbt(tag);
        return tag;
    }

    public static void applyBlockEntityNbt(World world, int x, int y, int z, NbtCompound tag) {
        if (tag == null) {
            return;
        }
        BlockEntity be = world.getBlockEntity(x, y, z);
        if (be == null) {
            return;
        }
        be.readNbt(tag);
    }
}
