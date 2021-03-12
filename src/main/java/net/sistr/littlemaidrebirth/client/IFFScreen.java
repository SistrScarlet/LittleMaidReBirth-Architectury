package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.network.SyncIFFPacket;

import java.util.List;
import java.util.Optional;

//IFFの受け渡しをしてオンオフ切り替えるスクリーン
//閉じたときにパケットで結果を返す
//todo リセットボタン
@Environment(EnvType.CLIENT)
public class IFFScreen extends Screen {
    private final Entity entity;
    private final ImmutableList<IFF> iffs;
    private int scrollAmount = 2;
    private int selectLine = -1;
    private long time;
    private static final int layerSize = 45;
    private final ScrollBarComponent scrollBar =
            new ScrollBarComponent(() -> this.width * 0.8F, () -> this.height * 0.2F,
                    () -> 20F, () -> this.height * 0.6F);

    public IFFScreen(Entity entity, List<IFF> iffs) {
        super(new LiteralText(""));
        this.entity = entity;
        this.iffs = ImmutableList.copyOf(iffs);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        renderAllMobs(matrices, mouseX, mouseY);
        scrollBar.renderScrollBar(matrices, (-scrollAmount + 2F) / iffs.size());
    }

    public void renderAllMobs(MatrixStack matrices, int mouseX, int mouseY) {
        int count = -1;
        int x = width / 2 - width / 4 + layerSize;
        int y = scrollAmount;
        for (IFF iff : iffs) {
            count++;
            if (y++ < 0) continue;
            if (height < y * layerSize) break;
            iff.getIFFType().render(matrices, x, y * layerSize, -mouseX + x, -mouseY + y * layerSize);
            if (count == selectLine)
                fill(matrices, width / 4, y * layerSize,
                        width - width / 4, (y - 1) * layerSize, 0x40FFFFFF);
            int color;
            switch (iff.getIFFTag()) {
                case FRIEND:
                    color = 0x40FF40;
                    break;
                case ENEMY:
                    color = 0xFF4040;
                    break;
                default:
                    color = 0xFFFF40;
            }
            drawStringWithShadow(matrices, textRenderer, iff.getIFFTag().getName(),
                    x + layerSize * 2, y * layerSize - layerSize / 2, color);
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        super.renderBackground(matrices);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Optional<Float> optional = scrollBar.click(mouseX, mouseY);
        if (optional.isPresent()) {
            scrollAmount = (int) ((-iffs.size() + 2) * optional.get());
            return false;
        }
        if (width / 4F < mouseX && mouseX < width - width / 4F) {
            this.selectLine = -scrollAmount + (int) (mouseY / layerSize);
            if (this.selectLine < 0 || iffs.size() <= selectLine) {
                return false;
            }
            if (Util.getMeasuringTimeMs() - this.time < 250L) {
                IFF iff = iffs.get(selectLine);
                IFFTag tag = iff.getIFFTag();
                switch (tag) {
                    case FRIEND:
                        iff.setTag(IFFTag.ENEMY);
                        break;
                    case ENEMY:
                        iff.setTag(IFFTag.UNKNOWN);
                        break;
                    default:
                        iff.setTag(IFFTag.FRIEND);
                }
                MinecraftClient.getInstance().getSoundManager()
                        .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        this.time = Util.getMeasuringTimeMs();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scrollAmount += 0 < amount ? 1 : -1;
        scrollAmount = MathHelper.clamp(scrollAmount, -iffs.size() + 3, 2);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void onClose() {
        super.onClose();
        SyncIFFPacket.sendC2SPacket(entity, iffs);
    }

}
