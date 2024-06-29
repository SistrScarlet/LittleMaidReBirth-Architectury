package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmodelloader.LMMLMod;
import net.sistr.littlemaidmodelloader.client.screen.*;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.network.C2SSetIFFPacket;
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
            Identifier.of(LMMLMod.MODID, "textures/gui/model_select.png");
    private final Entity entity;
    private final ImmutableList<IFF> iffs;
    private ScrollBar scrollBar;
    private ListGUI<IFFGUIElement> iffGui;

    public IFFScreen(Entity entity, List<IFF> iffs) {
        super(Text.empty());
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, MODEL_SELECT_GUI_TEXTURE);
        int relX = (this.width - GUI_WIDTH) / 2;
        int relY = (this.height - GUI_HEIGHT) / 2;
        context.drawTexture(MODEL_SELECT_GUI_TEXTURE, relX, relY, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        iffGui.render(context, mouseX, mouseY, delta);
        scrollBar.render(context, mouseX, mouseY, delta);

    }

    @Override
    public void tick() {
        super.tick();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollBar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            iffGui.setScroll(scrollBar.getPoint());
            return true;
        } else {
            if (iffGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                scrollBar.setPoint(iffGui.getScroll());
                return true;
            }
            return false;
        }
    }

    @Override
    public void close() {
        super.close();
        C2SSetIFFPacket.sendC2SPacket(entity, iffs, this.entity.getRegistryManager());
    }

    public static class IFFGUIElement extends GUIElement implements ListGUIElement {
        private final IFF iff;
        private final MarginedClickable clickable = new MarginedClickable(4);
        private boolean renderClashed;

        public IFFGUIElement(IFF iff) {
            super(15 * 16, 15 * 3);
            this.iff = iff;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.iff.getIFFType().getTargetEntityExample().ifPresent(entity -> {
                EntityType<?> entityType = entity.getType();
                if (renderClashed) return;
                try {
                    int scale = 15;
                    InventoryScreen.drawEntity(context,
                            x, y,
                            x + width, y + height,
                            scale,
                            -entity.getHeight() / 2.0F + height / 2.0F / scale,
                            mouseX, mouseY, entity);
                } catch (Exception e) {
                    LMRBMod.LOGGER.warn("描画処理がクラッシュしました。" + entityType + ":" + entity);
                    e.printStackTrace();
                    renderClashed = true;
                    //行われない終了処理を行う
                    //ちょっと強引
                    context.draw();
                    EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
                    entityRenderDispatcher.setRenderShadows(true);
                    context.getMatrices().pop();
                    DiffuseLighting.enableGuiDepthLighting();
                    context.disableScissor();
                }
            });
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            context.drawText(textRenderer, Text.translatable(this.iff.getEntityType().getTranslationKey()),
                    this.x, this.y, 0xFFFFFFFF, true);
            int color = switch (iff.getIFFTag()) {
                case FRIEND -> 0xFF40FF40;
                case ENEMY -> 0xFFFF4040;
                default -> 0xFFFFFF40;
            };
            context.drawText(textRenderer, iff.getIFFTag().getName(),
                    this.x, this.y + textRenderer.fontHeight * 2, color, true);
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
                        case ENEMY -> this.iff.setTag(IFFTag.FRIEND);
                        case FRIEND -> this.iff.setTag(IFFTag.UNKNOWN);
                        default -> this.iff.setTag(IFFTag.ENEMY);
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

    }

}
