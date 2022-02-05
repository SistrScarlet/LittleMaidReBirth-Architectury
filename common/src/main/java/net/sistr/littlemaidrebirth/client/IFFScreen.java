package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmodelloader.LittleMaidModelLoader;
import net.sistr.littlemaidmodelloader.client.screen.*;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.network.SyncIFFPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

//IFFの受け渡しをしてオンオフ切り替えるスクリーン
//閉じたときにパケットで結果を返す
//todo リセットボタン
@Environment(EnvType.CLIENT)
public class IFFScreen extends Screen {
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 196;
    public static final Identifier MODEL_SELECT_GUI_TEXTURE =
            new Identifier(LittleMaidModelLoader.MODID, "textures/gui/model_select.png");
    private final Entity entity;
    private final ImmutableList<IFF> iffs;
    private ScrollBar scrollBar;
    private ListGUI<IFFGUIElement> iffGui;

    public IFFScreen(Entity entity, List<IFF> iffs) {
        super(new LiteralText(""));
        this.entity = entity;
        this.iffs = ImmutableList.copyOf(iffs);
    }

    @Override
    protected void init() {
        assert this.client != null;
        int scale = 15;
        int widthRatio = 16;
        int heightRatio = 3;
        int heightStack = 4;
        int x = (this.width - scale * widthRatio) / 2;
        int y = (this.height - scale * heightRatio * heightStack) / 2;
        this.iffGui = new ListGUI<>(x, y, 1, heightStack,
                scale * widthRatio, scale * heightRatio,
                this.iffs.stream().map(IFFGUIElement::new).collect(Collectors.toList()));
        this.scrollBar = new ScrollBar(
                (width + GUI_WIDTH) / 2 + 4, (height - GUI_HEIGHT) / 2,
                8, GUI_HEIGHT, this.iffGui.size(),
                new TextureAddress(0, 200, 8, 8, 256, 256),
                new TextureAddress(0, 208, 8, 8, 256, 256),
                new TextureAddress(0, 216, 8, 8, 256, 256),
                new TextureAddress(0, 224, 10, 6, 256, 256),
                MODEL_SELECT_GUI_TEXTURE);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        this.client.getTextureManager().bindTexture(MODEL_SELECT_GUI_TEXTURE);
        int relX = (this.width - GUI_WIDTH) / 2;
        int relY = (this.height - GUI_HEIGHT) / 2;
        this.drawTexture(matrices, relX, relY, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        iffGui.render(matrices, mouseX, mouseY, delta);
        scrollBar.render(matrices, mouseX, mouseY, delta);

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
        if (scrollBar.mouseClicked(mouseX, mouseY, button)) {
            iffGui.setScroll(scrollBar.getPoint());
            return true;
        } else {
            return iffGui.mouseClicked(mouseX, mouseY, button);
        }

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrollBar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            iffGui.setScroll(scrollBar.getPoint());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollBar.mouseReleased(mouseX, mouseY, button);
        return iffGui.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (scrollBar.mouseScrolled(mouseX, mouseY, amount)) {
            iffGui.setScroll(scrollBar.getPoint());
            return true;
        } else {
            if (iffGui.mouseScrolled(mouseX, mouseY, amount)) {
                scrollBar.setPoint(iffGui.getScroll());
                return true;
            }
            return false;
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        SyncIFFPacket.sendC2SPacket(entity, iffs);
    }

    public static class IFFGUIElement extends GUIElement implements ListGUIElement {
        private final IFF iff;
        private final MarginedClickable clickable = new MarginedClickable(4);
        private int x;
        private int y;
        private boolean renderClashed;

        public IFFGUIElement(IFF iff) {
            this.iff = iff;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.iff.getIFFType().getEntity().ifPresent(entity -> {
                EntityType<?> entityType = entity.getType();
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                textRenderer.drawWithShadow(matrices, new TranslatableText(entityType.getTranslationKey()),
                        0, 0, 0xFFFFFFFF);
                int color;
                switch (iff.getIFFTag()) {
                    case FRIEND:
                        color = 0xFF40FF40;
                        break;
                    case ENEMY:
                        color = 0xFFFF4040;
                        break;
                    default:
                        color = 0xFFFFFF40;
                }
                textRenderer.drawWithShadow(matrices, iff.getIFFTag().getName(),
                        0, textRenderer.fontHeight * 2, color);
                if (renderClashed) return;
                try {
                    InventoryScreen.drawEntity(x + 15 * 12 + 15 / 2, y + 15 * 3, 15,
                            15 * 12 + 15 / 2f - mouseX,
                            15 * 3 - entity.getEyeHeight(EntityPose.STANDING) * 15 - mouseY, entity);
                } catch (Exception e) {
                    LittleMaidReBirthMod.LOGGER.warn("描画処理がクラッシュしました。" + entityType + ":" + entity);
                    e.printStackTrace();
                    renderClashed = true;
                    //行われない終了処理を行う
                    //ちょっと強引
                    VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                    immediate.draw();
                    EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
                    entityRenderDispatcher.setRenderShadows(true);
                    RenderSystem.getModelViewStack().pop();
                    DiffuseLighting.enableGuiDepthLighting();
                }
            });
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                clickable.click(mouseX, mouseY);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (clickable.release(mouseX, mouseY)) {
                    IFFTag tag = this.iff.getIFFTag();
                    switch (tag) {
                        case ENEMY:
                            this.iff.setTag(IFFTag.FRIEND);
                            break;
                        case FRIEND:
                            this.iff.setTag(IFFTag.UNKNOWN);
                            break;
                        default:
                            this.iff.setTag(IFFTag.ENEMY);
                            break;
                    }
                    MinecraftClient.getInstance().getSoundManager()
                            .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void setSelected(boolean b) {

        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void setPos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}
