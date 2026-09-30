package net.mymod.reisenmatica;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class Schematic {

    public int width;
    public int height;
    public int length;
    public byte[] blockIds;
    public byte[] blockMeta;

    public String name = "Unnamed";
    public int version = 0;
    public long timeCreated = System.currentTimeMillis();

    private int index(int x, int y, int z) {
        return (y * length + z) * width + x;
    }

    public int idAt(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) {
            return 0;
        }
        return blockIds[index(x, y, z)] & 0xFF;
    }

    public int metaAt(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= height || z >= length) {
            return 0;
        }
        return blockMeta[index(x, y, z)] & 0xF;
    }

    public static Schematic capture(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);

        Schematic s = new Schematic();
        s.width = maxX - minX + 1;
        s.height = maxY - minY + 1;
        s.length = maxZ - minZ + 1;

        int volume = s.width * s.height * s.length;
        s.blockIds = new byte[volume];
        s.blockMeta = new byte[volume];

        for (int y = 0; y < s.height; y++) {
            for (int z = 0; z < s.length; z++) {
                for (int x = 0; x < s.width; x++) {
                    int idx = s.index(x, y, z);
                    s.blockIds[idx] = (byte) WorldBridge.getBlockId(world, minX + x, minY + y, minZ + z);
                    s.blockMeta[idx] = (byte) WorldBridge.getBlockMeta(world, minX + x, minY + y, minZ + z);
                }
            }
        }
        return s;
    }

    public void rotate90() {
        int nw = length, nl = width;
        byte[] ids = new byte[blockIds.length];
        byte[] meta = new byte[blockMeta.length];
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int nx = length - 1 - z;
                    int nz = x;
                    int ni = (y * nl + nz) * nw + nx;
                    int oi = index(x, y, z);
                    ids[ni] = blockIds[oi];
                    meta[ni] = blockMeta[oi];
                    version++;
                }
            }
        }
        width = nw;
        length = nl;
        blockIds = ids;
        blockMeta = meta;
    }

    public Map<Integer, Integer> materials() {
        Map<Integer, Integer> map = new HashMap<>();
        for (byte b : blockIds) {
            int id = b & 0xFF;
            if (id != 0) {
                map.merge(id, 1, Integer::sum);
            }
        }
        return map;
    }

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        root.putString("Format", "ReisenMaticaSchematic");
        root.putInt("FormatVersion", 2);
        root.putString("Name", name);
        root.putLong("TimeCreated", timeCreated);
        root.putInt("Width", width);
        root.putInt("Height", height);
        root.putInt("Length", length);
        root.putByteArray("BlockIds", blockIds);
        root.putByteArray("BlockMeta", blockMeta);
        return root;
    }

    public static Schematic fromNbt(NbtCompound root) {
        Schematic s = new Schematic();
        s.name = root.getString("Name");
        s.timeCreated = root.getLong("TimeCreated");
        s.width = root.getInt("Width");
        s.height = root.getInt("Height");
        s.length = root.getInt("Length");
        s.blockIds = root.getByteArray("BlockIds");
        s.blockMeta = root.getByteArray("BlockMeta");
        return s;
    }
}