package net.mymod.reisenmatica;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public class Projection {

    private static final int MAX_FILLED_BLOCKS = 50000;
    private static final int MAX_TEXTURED = 1500;
    private static final double TEXTURE_RANGE_SQ = 40.0 * 40.0;
    private static final long REBUILD_INTERVAL_MS = 500;

    private static BlockRenderManager blockRenderer;
    private static boolean texturedFailed = false;

    private static int displayList = -1;
    private static Schematic cachedSchematic = null;
    private static int cachedVersion = -1;
    private static int cachedOx, cachedOy, cachedOz;
    private static boolean cachedTextured;
    private static long lastRebuild = 0;

    private static double smoothX, smoothY, smoothZ;
    private static boolean smoothInit = false;

    public static void render() {
        Schematic s = ReisenMaticaMod.current;
        if (s == null || !ReisenMaticaMod.projectionOn) {
            return;
        }
        Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }

        int bx, by, bz;
        if (ReisenMaticaMod.anchored) {
            bx = ReisenMaticaMod.anchorX;
            by = ReisenMaticaMod.anchorY;
            bz = ReisenMaticaMod.anchorZ;
        } else {
            HitResult hit = mc.crosshairTarget;
            if (hit == null || hit.type != HitResultType.BLOCK) {
                return;
            }
            bx = hit.blockX;
            by = hit.blockY;
            bz = hit.blockZ;
        }

        int ox = bx - s.width / 2;
        int oy = by + 1 + ReisenMaticaMod.yOffset;
        int oz = bz - s.length / 2;

        if (!smoothInit) {
            smoothX = mc.player.x;
            smoothY = mc.player.y;
            smoothZ = mc.player.z;
            smoothInit = true;
        } else {
            double t = 0.6;
            smoothX += (mc.player.x - smoothX) * t;
            smoothY += (mc.player.y - smoothY) * t;
            smoothZ += (mc.player.z - smoothZ) * t;
        }
        double cx = smoothX, cy = smoothY, cz = smoothZ;

        boolean useTex = ReisenMaticaMod.textured && !texturedFailed;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_POLYGON_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        GL11.glTranslated(-cx, -cy, -cz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);

        int total = 0;
        for (byte b : s.blockIds) {
            if (b != 0) {
                total++;
            }
        }

        if (total <= MAX_FILLED_BLOCKS) {
            boolean needRebuild = displayList < 0
                    || cachedSchematic != s
                    || cachedVersion != s.version
                    || cachedOx != ox || cachedOy != oy || cachedOz != oz
                    || cachedTextured != useTex
                    || (System.currentTimeMillis() - lastRebuild) > REBUILD_INTERVAL_MS;

            if (needRebuild) {
                rebuildList(mc, s, ox, oy, oz, useTex);
                cachedSchematic = s;
                cachedVersion = s.version;
                cachedOx = ox; cachedOy = oy; cachedOz = oz;
                cachedTextured = useTex;
                lastRebuild = System.currentTimeMillis();
            }

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0f, -1.0f);
            GL11.glCallList(displayList);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

            if (useTex) {
                try {
                    drawTextured(mc, s, ox, oy, oz, ReisenMaticaMod.opacity, cx, cy, cz);
                } catch (Throwable t) {
                    texturedFailed = true;
                    t.printStackTrace();
                }
            }
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0f);
        if (ReisenMaticaMod.anchored) {
            GL11.glColor4f(0.2f, 1.0f, 0.3f, 1.0f);
        } else {
            GL11.glColor4f(1.0f, 0.9f, 0.1f, 1.0f);
        }
        GL11.glBegin(GL11.GL_LINES);
        box(ox, oy, oz, ox + s.width, oy + s.height, oz + s.length);
        GL11.glEnd();

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static void rebuildList(Minecraft mc, Schematic s, int ox, int oy, int oz, boolean useTex) {
        if (displayList >= 0) {
            GL11.glDeleteLists(displayList, 1);
        }
        displayList = GL11.glGenLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE);

        float alpha = ReisenMaticaMod.opacity;
        float wrongAlpha = 0.75f;

        GL11.glBegin(GL11.GL_QUADS);
        for (int y = 0; y < s.height; y++) {
            for (int z = 0; z < s.length; z++) {
                for (int x = 0; x < s.width; x++) {
                    int id = s.idAt(x, y, z);
                    if (id == 0) {
                        continue;
                    }
                    int wx = ox + x, wy = oy + y, wz = oz + z;
                    int worldId = WorldBridge.getBlockId(mc.world, wx, wy, wz);
                    if (worldId == id) {
                        continue;
                    }
                    if (worldId != 0) {
                        drawWrongCube(wx, wy, wz, wrongAlpha);
                    } else if (!useTex) {
                        drawGhostFaces(s, x, y, z, wx, wy, wz, id, alpha);
                    }
                }
            }
        }
        GL11.glEnd();

        GL11.glLineWidth(2.0f);
        GL11.glColor4f(1.0f, 0.15f, 0.15f, 0.85f);
        GL11.glBegin(GL11.GL_LINES);
        for (int y = 0; y < s.height; y++) {
            for (int z = 0; z < s.length; z++) {
                for (int x = 0; x < s.width; x++) {
                    int id = s.idAt(x, y, z);
                    if (id == 0) {
                        continue;
                    }
                    int wx = ox + x, wy = oy + y, wz = oz + z;
                    int worldId = WorldBridge.getBlockId(mc.world, wx, wy, wz);
                    if (worldId != 0 && worldId != id) {
                        box(wx, wy, wz, wx + 1, wy + 1, wz + 1);
                    }
                }
            }
        }
        GL11.glEnd();

        GL11.glEndList();
    }

    private static void drawTextured(Minecraft mc, Schematic s, int ox, int oy, int oz,
                                     float alpha, double cx, double cy, double cz) {
        if (blockRenderer == null) {
            blockRenderer = new BlockRenderManager();
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        mc.textureManager.bindTexture(mc.textureManager.getTextureId("/terrain.png"));
        GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
        GL14.glBlendColor(1.0f, 1.0f, 1.0f, alpha);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        int drawn = 0;
        for (int y = 0; y < s.height && drawn < MAX_TEXTURED; y++) {
            for (int z = 0; z < s.length && drawn < MAX_TEXTURED; z++) {
                for (int x = 0; x < s.width && drawn < MAX_TEXTURED; x++) {
                    int id = s.idAt(x, y, z);
                    if (id == 0 || !isSurface(s, x, y, z)) {
                        continue;
                    }
                    int wx = ox + x, wy = oy + y, wz = oz + z;
                    if (WorldBridge.getBlockId(mc.world, wx, wy, wz) != 0) {
                        continue;
                    }
                    double dx = wx + 0.5 - cx, dy = wy + 0.5 - cy, dz = wz + 0.5 - cz;
                    if (dx * dx + dy * dy + dz * dz > TEXTURE_RANGE_SQ) {
                        continue;
                    }
                    Block block = Block.BLOCKS[id];
                    if (block == null) {
                        continue;
                    }
                    GL11.glPushMatrix();
                    GL11.glTranslatef(wx + 0.5f, wy + 0.5f, wz + 0.5f);
                    blockRenderer.render(block, s.metaAt(x, y, z), 1.0f);
                    GL11.glPopMatrix();
                    drawn++;
                }
            }
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static boolean isSurface(Schematic s, int x, int y, int z) {
        return s.idAt(x - 1, y, z) == 0 || s.idAt(x + 1, y, z) == 0
                || s.idAt(x, y - 1, z) == 0 || s.idAt(x, y + 1, z) == 0
                || s.idAt(x, y, z - 1) == 0 || s.idAt(x, y, z + 1) == 0;
    }

    private static void drawGhostFaces(Schematic s, int x, int y, int z, int wx, int wy, int wz, int id, float alpha) {
        int rgb = colorOf(id);
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        double x0 = wx, y0 = wy, z0 = wz, x1 = wx + 1, y1 = wy + 1, z1 = wz + 1;

        if (s.idAt(x, y + 1, z) == 0) {
            GL11.glColor4f(r, g, b, alpha);
            GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x1, y1, z0);
            GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);
        }
        if (s.idAt(x, y - 1, z) == 0) {
            GL11.glColor4f(r * 0.5f, g * 0.5f, b * 0.5f, alpha);
            GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0);
            GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x0, y0, z1);
        }
        if (s.idAt(x, y, z - 1) == 0) {
            GL11.glColor4f(r * 0.8f, g * 0.8f, b * 0.8f, alpha);
            GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0);
            GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x0, y1, z0);
        }
        if (s.idAt(x, y, z + 1) == 0) {
            GL11.glColor4f(r * 0.8f, g * 0.8f, b * 0.8f, alpha);
            GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x1, y0, z1);
            GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);
        }
        if (s.idAt(x - 1, y, z) == 0) {
            GL11.glColor4f(r * 0.65f, g * 0.65f, b * 0.65f, alpha);
            GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y0, z1);
            GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y1, z0);
        }
        if (s.idAt(x + 1, y, z) == 0) {
            GL11.glColor4f(r * 0.65f, g * 0.65f, b * 0.65f, alpha);
            GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y0, z1);
            GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z0);
        }
    }

    private static void drawWrongCube(int wx, int wy, int wz, float alpha) {
        double x0 = wx, y0 = wy, z0 = wz, x1 = wx + 1, y1 = wy + 1, z1 = wz + 1;
        float r = 1.0f, g = 0.1f, b = 0.1f;

        GL11.glColor4f(r, g, b, alpha);
        GL11.glVertex3d(x0, y1, z0); GL11.glVertex3d(x1, y1, z0);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);

        GL11.glColor4f(r * 0.5f, g * 0.5f, b * 0.5f, alpha);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y0, z1); GL11.glVertex3d(x0, y0, z1);

        GL11.glColor4f(r * 0.8f, g * 0.8f, b * 0.8f, alpha);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x1, y0, z0);
        GL11.glVertex3d(x1, y1, z0); GL11.glVertex3d(x0, y1, z0);

        GL11.glColor4f(r * 0.8f, g * 0.8f, b * 0.8f, alpha);
        GL11.glVertex3d(x0, y0, z1); GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x0, y1, z1);

        GL11.glColor4f(r * 0.65f, g * 0.65f, b * 0.65f, alpha);
        GL11.glVertex3d(x0, y0, z0); GL11.glVertex3d(x0, y0, z1);
        GL11.glVertex3d(x0, y1, z1); GL11.glVertex3d(x0, y1, z0);

        GL11.glColor4f(r * 0.65f, g * 0.65f, b * 0.65f, alpha);
        GL11.glVertex3d(x1, y0, z0); GL11.glVertex3d(x1, y0, z1);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z0);
    }

    public static int colorOf(int id) {
        switch (id) {
            case 1: return 0x7D7D7D;
            case 2: return 0x5B9A32;
            case 3: return 0x866043;
            case 4: return 0x6E6E6E;
            case 5: return 0xB8945F;
            case 6: return 0x4CAF50;
            case 7: return 0x333333;
            case 8: case 9: return 0x2F43F4;
            case 10: case 11: return 0xFF6A00;
            case 12: return 0xDBCF9F;
            case 13: return 0x857F7E;
            case 14: return 0xF8D96A;
            case 15: return 0xD8AF93;
            case 16: return 0x444444;
            case 17: return 0x6B5230;
            case 18: return 0x2F6F1F;
            case 19: return 0xC9C93F;
            case 20: return 0xC0E8F0;
            case 24: return 0xD9CD9A;
            case 35: return 0xEEEEEE;
            case 41: return 0xF9EC4E;
            case 42: return 0xDDDDDD;
            case 45: return 0x966C5A;
            case 47: return 0x8A6B3F;
            case 49: return 0x1B1233;
            case 50: return 0xFFD24A;
            case 53: return 0xB8945F;
            case 54: return 0xA0722D;
            case 58: return 0x9A7040;
            case 61: case 62: return 0x777777;
            case 64: return 0xB08A4A;
            case 65: return 0xA07A3A;
            case 78: case 80: return 0xF5FBFB;
            case 79: return 0x9CC4FF;
            case 82: return 0xA0A6B3;
            default:
                int h = (int) ((id * 2654435761L) & 0xFFFFFF);
                return 0x404040 | (h & 0xBFBFBF);
        }
    }

    private static void box(double x0, double y0, double z0, double x1, double y1, double z1) {
        line(x0, y0, z0, x1, y0, z0); line(x1, y0, z0, x1, y0, z1);
        line(x1, y0, z1, x0, y0, z1); line(x0, y0, z1, x0, y0, z0);
        line(x0, y1, z0, x1, y1, z0); line(x1, y1, z0, x1, y1, z1);
        line(x1, y1, z1, x0, y1, z1); line(x0, y1, z1, x0, y1, z0);
        line(x0, y0, z0, x0, y1, z0); line(x1, y0, z0, x1, y1, z0);
        line(x1, y0, z1, x1, y1, z1); line(x0, y0, z1, x0, y1, z1);
    }

    private static void line(double ax, double ay, double az, double bx, double by, double bz) {
        GL11.glVertex3d(ax, ay, az);
        GL11.glVertex3d(bx, by, bz);
    }
}