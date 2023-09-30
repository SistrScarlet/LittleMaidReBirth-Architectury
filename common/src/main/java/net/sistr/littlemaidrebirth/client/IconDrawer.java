package net.sistr.littlemaidrebirth.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class IconDrawer {
    private final Identifier[] TEXTURES;
    private final int width;
    private final int height;

    public IconDrawer(int width, int height, Identifier... textures) {
        this.width = width;
        this.height = height;
        TEXTURES = textures;
    }

    public void draw(DrawContext context, int x, int y, int type) {
        context.drawTexture(TEXTURES[type], x, y, 0, 0, 0, width, height, width, height);
    }

    public void drawSafe(DrawContext context, int x, int y, int type) {
        if (type < TEXTURES.length && TEXTURES[type] != null) {
            context.drawTexture(TEXTURES[type], x, y, 0, 0, 0, width, height, width, height);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Full/Half/Emptyの3状態あるアイコンを並べて描画する。
     * TEXTURESは 0 = Full, 1 = Half, 2 = Empty, 3 = Containerである。
     * TEXTURESの 3 = Container があるとき、アイコンの背景として描画される。
     * なおTEXTURESがnull、あるいはTEXTURESの長さが足りない場合は、そのテクスチャだけ描画されない。
     * @param row 行の数。row = 2 のときタテは2。
     * @param column 列の数。column = 2 のときヨコは2。
     * @param value 表現したい値。Full = 2, Half = 1, Empty = 0で示される。
     */
    public void drawIconMatrix(DrawContext context, int x, int y, int row, int column, int value) {
        int iconNum = row * column;
        for (int i = 0; i < iconNum; i++) {
            int drawX = x + i % column * this.getWidth();
            int drawY = y + i / column * this.getHeight();
            this.drawSafe(context, drawX, drawY, 3);
            int type = MathHelper.clamp(value - i * 2, 0, 2);
            this.drawSafe(context, drawX, drawY, type);
        }
    }

}
