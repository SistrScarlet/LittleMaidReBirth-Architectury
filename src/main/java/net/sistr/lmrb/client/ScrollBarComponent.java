package net.sistr.lmrb.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Optional;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ScrollBarComponent extends DrawableHelper {
    protected final Supplier<Float> left;
    protected final Supplier<Float> top;
    protected final Supplier<Float> width;
    protected final Supplier<Float> height;

    public ScrollBarComponent(Supplier<Float> left, Supplier<Float> top, Supplier<Float> width, Supplier<Float> height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public void renderScrollBar(MatrixStack matrices, float percent) {
        fill(matrices, left.get(), top.get(), left.get() + width.get(), top.get() + height.get(), 0xFF000000);
        fill(matrices, left.get(), top.get(), left.get() + width.get(), top.get() + height.get() * percent, 0xFFFFFFFF);
    }

    public Optional<Float> click(double x, double y) {
        if (left.get() < x && x < left.get() + width.get()) {
            float percent = (float) ((y - top.get()) / height.get());
            if (0 <= percent && percent <= 1F) return Optional.of(percent);
        }
        return Optional.empty();
    }

    public static void fill(MatrixStack matrices, float x1, float y1, float x2, float y2, int color) {
        fill(matrices, (int) x1, (int) y1, (int) x2, (int) y2, color);
    }

}
