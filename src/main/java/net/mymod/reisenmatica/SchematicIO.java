package net.mymod.reisenmatica;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

public class SchematicIO {

    public static final File DIR = new File("schematics");
    private static final String EXT = ".reisenmatica";

    public static String saveNew(Schematic schematic) throws IOException {
        DIR.mkdirs();
        int n = 1;
        File f;
        do {
            f = new File(DIR, "schem_" + n + EXT);
            n++;
        } while (f.exists());
        schematic.name = f.getName();
        try (OutputStream out = new FileOutputStream(f)) {
            NbtIo.writeCompressed(schematic.toNbt(), out);
        }
        return f.getName();
    }

    public static File[] list() {
        File[] files = DIR.listFiles((dir, name) -> name.endsWith(EXT));
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        return files;
    }

    public static Schematic load(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            NbtCompound root = NbtIo.readCompressed(in);
            Schematic s = Schematic.fromNbt(root);
            s.name = file.getName();
            return s;
        }
    }
}