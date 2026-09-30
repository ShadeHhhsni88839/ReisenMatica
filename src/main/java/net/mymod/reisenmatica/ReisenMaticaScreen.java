package net.mymod.reisenmatica;

import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReisenMaticaScreen extends Screen {

    private static final String[] BTN = {"Use / Show", "Rotate", "Textures", "Delete", "Close"};
    private static final int ROW_H = 12;
    private static final int MAX_PREVIEW_BLOCKS = 80000;

    private File[] files = new File[0];
    private String[] dims = new String[0];
    private int selected = -1;
    private int scroll = 0;
    private Schematic preview;
    private List<Map.Entry<Integer, Integer>> mats = new ArrayList<>();
    private int totalBlocks = 0;

    private float angle = 35f;
    private boolean spin = false;
    private boolean dragging = false;
    private boolean mouseWasDown = false;
    private int lastDragX;
    private long lastTime;
    private int pendingDelete = -1;
    private String status = "";

    private final int[] bx = new int[BTN.length];
    private int by, bw, bh;
    private int px0, py0, px1, py1;

    @Override
    public void init() {
        lastTime = System.currentTimeMillis();
        refresh();
        File cur = ReisenMaticaMod.getCurrentFile();
        selected = files.length > 0 ? 0 : -1;
        if (cur != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().equals(cur.getName())) {
                    selected = i;
                }
            }
        }
        loadPreview();
    }

    private void refresh() {
        files = SchematicIO.list();
        dims = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                Schematic s = SchematicIO.load(files[i]);
                dims[i] = s.width + "x" + s.height + "x" + s.length;
            } catch (Exception e) {
                dims[i] = "?";
            }
        }
        if (selected >= files.length) {
            selected = files.length - 1;
        }
    }

    private void loadPreview() {
        preview = null;
        mats.clear();
        totalBlocks = 0;
        if (selected < 0 || selected >= files.length) {
            return;
        }
        try {
            preview = SchematicIO.load(files[selected]);
            rebuildMaterials();
        } catch (Exception e) {
            status = "Load error: " + e.getMessage();
        }
    }

    private void rebuildMaterials() {
        mats = new ArrayList<>(preview.materials().entrySet());
        mats.sort((a, b) -> b.getValue() - a.getValue());
        totalBlocks = 0;
        for (Map.Entry<Integer, Integer> e : mats) {
            totalBlocks += e.getValue();
        }
    }

    private void layout() {
        bh = 20;
        by = height - 28;
        int gap = 4;
        bw = (width - 20 - gap * (BTN.length - 1)) / BTN.length;
        for (int i = 0; i < BTN.length; i++) {
            bx[i] = 10 + i * (bw + gap);
        }
        px0 = 10 + listW() + 10;
        px1 = width - matsW() - 20;
        py0 = listTop();
        py1 = listBottom();
    }

    private int listW() {
        return Math.max(130, width / 4);
    }

    private int matsW() {
        return Math.max(150, width / 4);
    }

    private int listTop() {
        return 30;
    }

    private int listBottom() {
        return height - 38;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        layout();

        int wheel = Mouse.getDWheel();
        if (wheel != 0 && !(mouseX >= px0 && mouseX < px1 && mouseY >= py0 && mouseY < py1)) {
            move(wheel > 0 ? -1 : 1);
        }

        boolean mouseDown = Mouse.isButtonDown(0);
        boolean inPreview = mouseX >= px0 && mouseX < px1 && mouseY >= py0 && mouseY < py1;
        if (mouseDown && !mouseWasDown && inPreview) {
            dragging = true;
            lastDragX = mouseX;
        }
        if (!mouseDown) {
            dragging = false;
        }
        if (dragging) {
            int dx = mouseX - lastDragX;
            if (dx != 0) {
                angle += dx * 0.6f;
                lastDragX = mouseX;
            }
        }
        mouseWasDown = mouseDown;

        long now = System.currentTimeMillis();
        if (spin) {
            angle += (now - lastTime) * 0.03f;
        }
        lastTime = now;

        rect(0, 0, width, height, 0xC0101010);
        text("ReisenMatica - schematics (" + files.length + ")", 10, 10, 0xFFFFFF);
        if (!status.isEmpty()) {
            text(status, 10 + listW() + 10, 10, 0xFFFF55);
        }

        int lx0 = 10, lx1 = 10 + listW();
        rect(lx0, listTop(), lx1, listBottom(), 0x80000000);
        File cur = ReisenMaticaMod.getCurrentFile();
        int rows = visibleRows();
        if (selected >= 0) {
            if (selected < scroll) {
                scroll = selected;
            }
            if (selected >= scroll + rows) {
                scroll = selected - rows + 1;
            }
        }
        for (int r = 0; r < rows; r++) {
            int i = scroll + r;
            if (i >= files.length) {
                break;
            }
            int y = listTop() + r * ROW_H;
            if (i == selected) {
                rect(lx0, y, lx1, y + ROW_H, 0x80559955);
            }
            String name = files[i].getName().replace(".reisenmatica", "");
            boolean active = cur != null && cur.getName().equals(files[i].getName());
            text((active ? "> " : "") + name + " " + dims[i], lx0 + 3, y + 2, active ? 0x55FF55 : 0xFFFFFF);
        }
        if (files.length == 0) {
            text("No schematics yet.", lx0 + 3, listTop() + 4, 0xAAAAAA);
            text("Use Num1/Num2/Num3 in-game.", lx0 + 3, listTop() + 16, 0xAAAAAA);
        }

        drawPreview(px0, py0, px1, py1);

        int mx0 = width - matsW() - 10, mx1 = width - 10;
        rect(mx0, listTop(), mx1, listBottom(), 0x80000000);
        if (preview != null) {
            text("Blocks: " + totalBlocks + " (" + mats.size() + " types)", mx0 + 3, listTop() + 3, 0xFFFF55);
            int maxLines = (listBottom() - listTop() - 20) / ROW_H;
            for (int i = 0; i < mats.size() && i < maxLines; i++) {
                Map.Entry<Integer, Integer> e = mats.get(i);
                int stacks = (e.getValue() + 63) / 64;
                text(BlockNames.name(e.getKey()) + " x" + e.getValue() + " (" + stacks + " st)",
                        mx0 + 3, listTop() + 18 + i * ROW_H, 0xFFFFFF);
            }
            if (mats.size() > maxLines) {
                text("... +" + (mats.size() - maxLines) + " more", mx0 + 3, listBottom() - 11, 0xAAAAAA);
            }
        }

        for (int i = 0; i < BTN.length; i++) {
            boolean hover = mouseX >= bx[i] && mouseX < bx[i] + bw && mouseY >= by && mouseY < by + bh;
            String label = BTN[i];
            if (i == 2) {
                label = ReisenMaticaMod.textured ? "Textures: ON" : "Textures: OFF";
            }
            drawVanillaButton(bx[i], by, bx[i] + bw, by + bh, hover, label);
        }
        text("Drag the preview with left mouse to rotate. Opacity: Num+ / Num* in-game.",
                10, by - 12, 0x999999);
    }

    private void drawVanillaButton(int x0, int y0, int x1, int y1, boolean hover, String label) {
        int fill = hover ? 0xFF8B8B9A : 0xFF6B6B78;
        rect(x0, y0, x1, y1, 0xFF000000);
        rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
        rect(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x60FFFFFF);
        rect(x0 + 1, y0 + 1, x0 + 2, y1 - 1, 0x60FFFFFF);
        rect(x0 + 1, y1 - 2, x1 - 1, y1 - 1, 0x60000000);
        rect(x1 - 2, y0 + 1, x1 - 1, y1 - 1, 0x60000000);
        int tw = textRenderer.getWidth(label);
        text(label, x0 + (x1 - x0 - tw) / 2, y0 + (y1 - y0 - 8) / 2, hover ? 0xFFFFA0 : 0xE0E0E0);
    }

    private void drawPreview(int x0, int y0, int x1, int y1) {
        rect(x0, y0, x1, y1, 0x80000000);
        if (preview == null) {
            return;
        }
        Schematic s = preview;
        if (totalBlocks > MAX_PREVIEW_BLOCKS) {
            text("Too large for preview (" + totalBlocks + " blocks)", x0 + 6, y0 + 6, 0xFF8888);
            return;
        }
        float diag = (float) Math.sqrt(s.width * s.width + s.height * s.height + s.length * s.length);
        float scale = 0.9f * Math.min(x1 - x0, y1 - y0) / Math.max(1f, diag);

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glTranslatef((x0 + x1) / 2f, (y0 + y1) / 2f, 0f);
        GL11.glScalef(scale, -scale, scale);
        GL11.glRotatef(25f, 1f, 0f, 0f);
        GL11.glRotatef(angle, 0f, 1f, 0f);
        GL11.glTranslatef(-s.width / 2f, -s.height / 2f, -s.length / 2f);

        GL11.glBegin(GL11.GL_QUADS);
        for (int y = 0; y < s.height; y++) {
            for (int z = 0; z < s.length; z++) {
                for (int x = 0; x < s.width; x++) {
                    int id = s.idAt(x, y, z);
                    if (id == 0) {
                        continue;
                    }
                    int rgb = Projection.colorOf(id);
                    float r = ((rgb >> 16) & 0xFF) / 255f;
                    float g = ((rgb >> 8) & 0xFF) / 255f;
                    float b = (rgb & 0xFF) / 255f;
                    float x0f = x, y0f = y, z0f = z, x1f = x + 1, y1f = y + 1, z1f = z + 1;
                    if (s.idAt(x, y + 1, z) == 0) {
                        GL11.glColor4f(r, g, b, 1f);
                        GL11.glVertex3f(x0f, y1f, z0f); GL11.glVertex3f(x1f, y1f, z0f);
                        GL11.glVertex3f(x1f, y1f, z1f); GL11.glVertex3f(x0f, y1f, z1f);
                    }
                    if (s.idAt(x, y - 1, z) == 0) {
                        GL11.glColor4f(r * 0.5f, g * 0.5f, b * 0.5f, 1f);
                        GL11.glVertex3f(x0f, y0f, z0f); GL11.glVertex3f(x1f, y0f, z0f);
                        GL11.glVertex3f(x1f, y0f, z1f); GL11.glVertex3f(x0f, y0f, z1f);
                    }
                    if (s.idAt(x, y, z - 1) == 0) {
                        GL11.glColor4f(r * 0.8f, g * 0.8f, b * 0.8f, 1f);
                        GL11.glVertex3f(x0f, y0f, z0f); GL11.glVertex3f(x1f, y0f, z0f);
                        GL11.glVertex3f(x1f, y1f, z0f); GL11.glVertex3f(x0f, y1f, z0f);
                    }
                    if (s.idAt(x, y, z + 1) == 0) {
                        GL11.glColor4f(r * 0.8f, g * 0.8f, b * 0.8f, 1f);
                        GL11.glVertex3f(x0f, y0f, z1f); GL11.glVertex3f(x1f, y0f, z1f);
                        GL11.glVertex3f(x1f, y1f, z1f); GL11.glVertex3f(x0f, y1f, z1f);
                    }
                    if (s.idAt(x - 1, y, z) == 0) {
                        GL11.glColor4f(r * 0.65f, g * 0.65f, b * 0.65f, 1f);
                        GL11.glVertex3f(x0f, y0f, z0f); GL11.glVertex3f(x0f, y0f, z1f);
                        GL11.glVertex3f(x0f, y1f, z1f); GL11.glVertex3f(x0f, y1f, z0f);
                    }
                    if (s.idAt(x + 1, y, z) == 0) {
                        GL11.glColor4f(r * 0.65f, g * 0.65f, b * 0.65f, 1f);
                        GL11.glVertex3f(x1f, y0f, z0f); GL11.glVertex3f(x1f, y0f, z1f);
                        GL11.glVertex3f(x1f, y1f, z1f); GL11.glVertex3f(x1f, y1f, z0f);
                    }
                }
            }
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();

        text(s.width + " x " + s.height + " x " + s.length, x0 + 6, y1 - 12, 0xAAAAAA);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) {
            return;
        }
        for (int i = 0; i < BTN.length; i++) {
            if (mouseX >= bx[i] && mouseX < bx[i] + bw && mouseY >= by && mouseY < by + bh) {
                pressButton(i);
                return;
            }
        }
        int lx0 = 10, lx1 = 10 + listW();
        if (mouseX >= lx0 && mouseX < lx1 && mouseY >= listTop() && mouseY < listBottom()) {
            int i = scroll + (mouseY - listTop()) / ROW_H;
            if (i >= 0 && i < files.length) {
                selected = i;
                pendingDelete = -1;
                status = "";
                loadPreview();
            }
        }
    }

    @Override
    public void keyPressed(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_NUMPADENTER) {
            close();
        } else if (key == Keyboard.KEY_UP) {
            move(-1);
        } else if (key == Keyboard.KEY_DOWN) {
            move(1);
        } else if (key == Keyboard.KEY_LEFT) {
            angle -= 15f;
        } else if (key == Keyboard.KEY_RIGHT) {
            angle += 15f;
        } else if (key == Keyboard.KEY_SPACE) {
            spin = !spin;
        } else if (key == Keyboard.KEY_R) {
            pressButton(1);
        } else if (key == Keyboard.KEY_RETURN) {
            pressButton(0);
        } else if (key == Keyboard.KEY_DELETE) {
            pressButton(3);
        }
    }

    private void move(int d) {
        if (files.length == 0) {
            return;
        }
        selected = Math.max(0, Math.min(files.length - 1, (selected < 0 ? 0 : selected) + d));
        pendingDelete = -1;
        status = "";
        loadPreview();
    }

    private void pressButton(int i) {
        switch (i) {
            case 0: {
                try {
                    if (preview != null && selected >= 0 && selected < files.length) {
                        ReisenMaticaMod.setCurrent(preview, files[selected]);
                    }
                } catch (Exception ex) {
                    status = "Error: " + ex.getMessage();
                    ex.printStackTrace();
                }
                close();
                break;
            }
            case 1:
                if (preview != null) {
                    preview.rotate90();
                    status = "Rotated (press Use/Show to apply)";
                }
                break;
            case 2:
                ReisenMaticaMod.textured = !ReisenMaticaMod.textured;
                break;
            case 3:
                deleteSelected();
                break;
            case 4:
                close();
                break;
            default:
                break;
        }
    }

    private void deleteSelected() {
        if (selected < 0 || selected >= files.length) {
            return;
        }
        if (pendingDelete != selected) {
            pendingDelete = selected;
            status = "Press Delete again to confirm: " + files[selected].getName();
            return;
        }
        File f = files[selected];
        if (f.delete()) {
            ReisenMaticaMod.onDeleted(f);
            status = "Deleted " + f.getName();
            pendingDelete = -1;
            int keep = selected;
            refresh();
            selected = files.length == 0 ? -1 : Math.min(keep, files.length - 1);
            loadPreview();
        } else {
            status = "Could not delete " + f.getName();
        }
    }

    private void close() {
        this.minecraft.setScreen(null);
    }

    private void text(String s, int x, int y, int color) {
        this.textRenderer.drawWithShadow(s, x, y, color);
    }

    private static void rect(int x0, int y0, int x1, int y1, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x0, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y0);
        GL11.glVertex2f(x0, y0);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}