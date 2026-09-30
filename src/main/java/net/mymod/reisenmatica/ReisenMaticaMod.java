package net.mymod.reisenmatica;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReisenMaticaMod {

    static {
        EntrypointManager.registerLookup(java.lang.invoke.MethodHandles.lookup());
    }

    private static final KeyBinding KEY_POS1 = new KeyBinding("ReisenMatica: point 1", Keyboard.KEY_NUMPAD1);
    private static final KeyBinding KEY_POS2 = new KeyBinding("ReisenMatica: point 2", Keyboard.KEY_NUMPAD2);
    private static final KeyBinding KEY_SAVE = new KeyBinding("ReisenMatica: save", Keyboard.KEY_NUMPAD3);
    private static final KeyBinding KEY_NEXT = new KeyBinding("ReisenMatica: next schematic", Keyboard.KEY_NUMPAD4);
    private static final KeyBinding KEY_PREV = new KeyBinding("ReisenMatica: previous schematic", Keyboard.KEY_SUBTRACT);
    private static final KeyBinding KEY_TOGGLE = new KeyBinding("ReisenMatica: projection on/off", Keyboard.KEY_NUMPAD5);
    private static final KeyBinding KEY_ROTATE = new KeyBinding("ReisenMatica: rotate", Keyboard.KEY_NUMPAD6);
    private static final KeyBinding KEY_LOCK = new KeyBinding("ReisenMatica: lock/unlock position", Keyboard.KEY_NUMPAD7);
    private static final KeyBinding KEY_UP = new KeyBinding("ReisenMatica: move up", Keyboard.KEY_NUMPAD8);
    private static final KeyBinding KEY_DOWN = new KeyBinding("ReisenMatica: move down", Keyboard.KEY_NUMPAD9);
    private static final KeyBinding KEY_LIST = new KeyBinding("ReisenMatica: list schematics", Keyboard.KEY_NUMPAD0);
    private static final KeyBinding KEY_DELETE = new KeyBinding("ReisenMatica: delete schematic", Keyboard.KEY_DECIMAL);
    private static final KeyBinding KEY_OPAQUE = new KeyBinding("ReisenMatica: opaque +10%", Keyboard.KEY_ADD);
    private static final KeyBinding KEY_CLEAR = new KeyBinding("ReisenMatica: opaque -10%", Keyboard.KEY_MULTIPLY);
    private static final KeyBinding KEY_TEXTURES = new KeyBinding("ReisenMatica: change between textures/colors", Keyboard.KEY_DIVIDE);
    private static final KeyBinding KEY_MENU = new KeyBinding("ReisenMatica: open menu", Keyboard.KEY_NUMPADENTER);

    private static boolean has1, has2;
    private static int x1, y1, z1, x2, y2, z2;

    public static Schematic current;
    private static File currentFile;
    public static boolean projectionOn = false;
    private static int loadIndex = -1;
    private static boolean pendingDelete = false;

    public static boolean anchored = false;
    public static int anchorX, anchorY, anchorZ;
    public static int yOffset = 0;

    public static float opacity = 0.5f;
    public static boolean textured = true;

    @EventListener
    private static void registerKeys(KeyBindingRegisterEvent event) {
        event.keyBindings.add(KEY_POS1);
        event.keyBindings.add(KEY_POS2);
        event.keyBindings.add(KEY_SAVE);
        event.keyBindings.add(KEY_NEXT);
        event.keyBindings.add(KEY_PREV);
        event.keyBindings.add(KEY_TOGGLE);
        event.keyBindings.add(KEY_ROTATE);
        event.keyBindings.add(KEY_LOCK);
        event.keyBindings.add(KEY_UP);
        event.keyBindings.add(KEY_DOWN);
        event.keyBindings.add(KEY_LIST);
        event.keyBindings.add(KEY_DELETE);
        event.keyBindings.add(KEY_OPAQUE);
        event.keyBindings.add(KEY_CLEAR);
        event.keyBindings.add(KEY_TEXTURES);
        event.keyBindings.add(KEY_MENU);
    }

    @EventListener
    private static void onKey(KeyStateChangedEvent event) {
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) {
            return;
        }
        Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
        if (mc.currentScreen != null || mc.world == null || mc.player == null) {
            return;
        }
        int key = Keyboard.getEventKey();

        if (key == KEY_MENU.code) {
            mc.setScreen(new ReisenMaticaScreen());
            return;
        }

        boolean wasPendingDelete = pendingDelete;
        pendingDelete = false;

        if (key == KEY_POS1.code) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.type == HitResultType.BLOCK) {
                x1 = hit.blockX; y1 = hit.blockY; z1 = hit.blockZ;
                has1 = true;
                msg(mc, "\u00a7aPoint 1: " + x1 + " " + y1 + " " + z1);
            } else {
                msg(mc, "\u00a7cLook at a block.");
            }
        } else if (key == KEY_POS2.code) {
            HitResult hit = mc.crosshairTarget;
            if (hit != null && hit.type == HitResultType.BLOCK) {
                x2 = hit.blockX; y2 = hit.blockY; z2 = hit.blockZ;
                has2 = true;
                msg(mc, "\u00a7aPoint 2: " + x2 + " " + y2 + " " + z2);
            } else {
                msg(mc, "\u00a7cLook at a block.");
            }
        } else if (key == KEY_SAVE.code) {
            if (!has1 || !has2) {
                msg(mc, "\u00a7cSet both points first.");
                return;
            }
            try {
                Schematic s = Schematic.capture(mc.world, x1, y1, z1, x2, y2, z2);
                String file = SchematicIO.saveNew(s);
                msg(mc, "\u00a7aSaved: " + file + " (" + s.width + "x" + s.height + "x" + s.length + ")");
                msg(mc, "\u00a77Open menu (Numpad Enter) or press Num4 to load it.");
                printMaterials(mc, s);
            } catch (Exception e) {
                msg(mc, "\u00a7cSave error: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (key == KEY_NEXT.code) {
            step(mc, 1);
        } else if (key == KEY_PREV.code) {
            step(mc, -1);
        } else if (key == KEY_TOGGLE.code) {
            projectionOn = !projectionOn;
            msg(mc, projectionOn ? "\u00a7aProjection ON" : "\u00a77Projection OFF");
        } else if (key == KEY_ROTATE.code) {
            if (current == null) {
                msg(mc, "\u00a7cNo schematic loaded.");
                return;
            }
            current.rotate90();
            msg(mc, "\u00a7aRotated: " + current.width + "x" + current.height + "x" + current.length);
        } else if (key == KEY_LOCK.code) {
            if (anchored) {
                anchored = false;
                msg(mc, "\u00a77Position unlocked (follows crosshair)");
            } else {
                HitResult hit = mc.crosshairTarget;
                if (hit != null && hit.type == HitResultType.BLOCK) {
                    anchored = true;
                    anchorX = hit.blockX;
                    anchorY = hit.blockY;
                    anchorZ = hit.blockZ;
                    msg(mc, "\u00a7aPosition locked at " + anchorX + " " + anchorY + " " + anchorZ);
                } else {
                    msg(mc, "\u00a7cLook at a block to lock.");
                }
            }
        } else if (key == KEY_UP.code) {
            yOffset++;
            msg(mc, "\u00a77Height offset: " + yOffset);
        } else if (key == KEY_DOWN.code) {
            yOffset--;
            msg(mc, "\u00a77Height offset: " + yOffset);
        } else if (key == KEY_LIST.code) {
            listSchematics(mc);
        } else if (key == KEY_DELETE.code) {
            if (currentFile == null || !currentFile.exists()) {
                msg(mc, "\u00a7cNo schematic selected.");
                return;
            }
            if (!wasPendingDelete) {
                pendingDelete = true;
                msg(mc, "\u00a7eDelete " + currentFile.getName() + "? Press again to confirm.");
            } else {
                String name = currentFile.getName();
                if (currentFile.delete()) {
                    msg(mc, "\u00a7aDeleted: " + name);
                    currentFile = null;
                    current = null;
                    loadIndex = -1;
                } else {
                    msg(mc, "\u00a7cCould not delete " + name);
                }
            }
        } else if (key == KEY_OPAQUE.code) {
            opacity = Math.min(1.0f, opacity + 0.1f);
            msg(mc, "\u00a77Opacity: " + Math.round(opacity * 100) + "%");
        } else if (key == KEY_CLEAR.code) {
            opacity = Math.max(0.1f, opacity - 0.1f);
            msg(mc, "\u00a77Opacity: " + Math.round(opacity * 100) + "%");
        } else if (key == KEY_TEXTURES.code) {
            textured = !textured;
            msg(mc, textured ? "\u00a7aTextured mode" : "\u00a77Color mode");
        }
    }

    public static File getCurrentFile() {
        return currentFile;
    }

    public static void setCurrent(Schematic s, File f) {
        current = s;
        currentFile = f;
        projectionOn = true;
        if (f != null) {
            selectFile(f);
        }
    }

    public static void onDeleted(File f) {
        if (currentFile != null && f != null && currentFile.getName().equals(f.getName())) {
            currentFile = null;
            current = null;
            loadIndex = -1;
        }
    }

    private static void selectFile(File f) {
        File[] files = SchematicIO.list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(f.getName())) {
                loadIndex = i;
                break;
            }
        }
        currentFile = f;
    }

    private static void step(Minecraft mc, int dir) {
        File[] files = SchematicIO.list();
        if (files.length == 0) {
            msg(mc, "\u00a7cNo schematics in the schematics folder.");
            return;
        }
        if (loadIndex < 0) {
            loadIndex = dir > 0 ? -1 : 0;
        }
        loadIndex = ((loadIndex + dir) % files.length + files.length) % files.length;
        try {
            current = SchematicIO.load(files[loadIndex]);
            currentFile = files[loadIndex];
            msg(mc, "\u00a7aLoaded " + (loadIndex + 1) + "/" + files.length + ": " + current.name
                    + " (" + current.width + "x" + current.height + "x" + current.length + ")");
            printMaterials(mc, current);
        } catch (Exception e) {
            msg(mc, "\u00a7cLoad error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void listSchematics(Minecraft mc) {
        File[] files = SchematicIO.list();
        if (files.length == 0) {
            msg(mc, "\u00a7cNo schematics saved.");
            return;
        }
        msg(mc, "\u00a7eSchematics (" + files.length + "):");
        int shown = 0;
        for (int i = 0; i < files.length; i++) {
            if (shown++ >= 8) {
                msg(mc, "\u00a77... and " + (files.length - 8) + " more");
                break;
            }
            String dims = "?";
            try {
                Schematic s = SchematicIO.load(files[i]);
                dims = s.width + "x" + s.height + "x" + s.length;
            } catch (Exception ignored) {
            }
            boolean selected = currentFile != null && files[i].getName().equals(currentFile.getName());
            msg(mc, (selected ? "\u00a7a> " : "\u00a7f  ") + (i + 1) + ". " + files[i].getName() + " " + dims);
        }
    }

    private static void printMaterials(Minecraft mc, Schematic s) {
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(s.materials().entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        msg(mc, "\u00a7eBlocks needed:");
        int shown = 0;
        for (Map.Entry<Integer, Integer> e : list) {
            if (shown++ >= 10) {
                msg(mc, "\u00a77... and " + (list.size() - 10) + " more types");
                break;
            }
            msg(mc, "\u00a7f " + BlockNames.name(e.getKey()) + ": " + e.getValue());
        }
    }

    private static void msg(Minecraft mc, String text) {
        mc.inGameHud.addChatMessage(text);
    }
}