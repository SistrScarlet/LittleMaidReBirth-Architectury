package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.client.screen.GUIElement;
import net.sistr.littlemaidmodelloader.client.screen.ModelSelectScreen;
import net.sistr.littlemaidmodelloader.client.screen.SoundPackSelectScreen;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.entity.MovingMode;
import net.sistr.littlemaidrebirth.network.OpenIFFScreenPacket;
import net.sistr.littlemaidrebirth.network.SyncBloodSuckPacket;
import net.sistr.littlemaidrebirth.network.SyncMovingStatePacket;

//todo モード名表示/移動状態をアイコンで表記
@Environment(EnvType.CLIENT)
public class LittleMaidScreen extends HandledScreen<LittleMaidScreenHandler> {
    private static final Identifier GUI =
            new Identifier("lmreengaged", "textures/gui/container/littlemaidinventory2.png");
    private static final Identifier SALARY_WINDOW_TEXTURE =
            new Identifier("littlemaidrebirth", "textures/gui/salary_window.png");
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final ItemStack ARMOR = Items.LEATHER_CHESTPLATE.getDefaultStack();
    private static final ItemStack BOOK = Items.BOOK.getDefaultStack();
    private static final ItemStack NOTE = Items.NOTE_BLOCK.getDefaultStack();
    private static final ItemStack FEATHER = Items.FEATHER.getDefaultStack();
    private static final ItemStack IRON_SWORD = Items.IRON_SWORD.getDefaultStack();
    private static final ItemStack IRON_AXE = Items.IRON_AXE.getDefaultStack();
    private static final ItemStack SUGAR = Items.SUGAR.getDefaultStack();
    private final LittleMaidEntity owner;
    private final int unpaidDays;
    private WindowGUIComponent salaryWindow;
    private boolean showSalaryWindow;
    private Text stateText;
    private MovingMode movingMode;

    public LittleMaidScreen(LittleMaidScreenHandler screenContainer, PlayerInventory inv, Text titleIn) {
        super(screenContainer, inv, titleIn);
        this.backgroundHeight = 208;
        owner = screenContainer.getGuiEntity();
        unpaidDays = screenContainer.getUnpaidDays();
        movingMode = owner.getMovingMode();
    }

    @Override
    protected void init() {
        super.init();
        if (owner == null) {
            client.setScreen(null);
            return;
        }
        int left = (int) ((this.width - backgroundWidth) / 2F) - 5;
        int top = (int) ((this.height - backgroundHeight) / 2F);
        int size = 20;
        int layer = -1;
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> owner.getTameOwner().ifPresent(OpenIFFScreenPacket::sendC2SPacket)) {
            @Override
            public void renderButton(MatrixStack matrices, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrices, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderGuiItemIcon(BOOK, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> client.setScreen(new SoundPackSelectScreen<>(title, owner))) {
            @Override
            public void renderButton(MatrixStack matrices, int x, int y, float delta) {
                super.renderButton(matrices, x, y, delta);
                itemRenderer.renderGuiItemIcon(NOTE, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> client.setScreen(new ModelSelectScreen<>(title, owner.world, owner))) {
            @Override
            public void renderButton(MatrixStack matrices, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrices, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderGuiItemIcon(ARMOR, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> {
                    switch (movingMode) {
                        case ESCORT -> movingMode = MovingMode.FREEDOM;
                        case FREEDOM -> movingMode = MovingMode.TRACER;
                        case TRACER -> movingMode = MovingMode.ESCORT;
                    }
                    stateText = getStateText();
                }) {
            @Override
            public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                super.renderButton(matrices, mouseX, mouseY, delta);
                itemRenderer.renderGuiItemIcon(FEATHER, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> SyncBloodSuckPacket.sendC2SPacket(this.owner, !this.owner.isBloodSuck())) {
            @Override
            public void renderButton(MatrixStack matrices, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrices, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderGuiItemIcon(LittleMaidScreen.this.owner.isBloodSuck() ? IRON_AXE : IRON_SWORD,
                        this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.salaryWindow = new WindowGUIComponent(
                this.width / 2 - 40, this.height / 2 - 40, 80, 80,
                ImmutableList.<GUIElement>builder()
                        .add(new SalaryGUI(80, 80, this.width / 2 - 40, this.height / 2 - 40,
                                this.itemRenderer, this.textRenderer, 7, unpaidDays))
                        .build()) {
            @Override
            public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShaderTexture(0, SALARY_WINDOW_TEXTURE);
                drawTexture(matrices, this.x, this.y, 0, 0, 80, 80, 128, 128);
                super.render(matrices, mouseX, mouseY, delta);
            }
        };
        this.addDrawableChild(new ButtonWidget(left - size, top + size * (layer += 2), size, size, new LiteralText(""),
                button -> {//ウィンドウを出す
                    showSalaryWindow = true;
                }) {
            @Override
            public void renderButton(MatrixStack matrices, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrices, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderGuiItemIcon(SUGAR, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        stateText = getStateText();
    }

    public Text getStateText() {
        MutableText stateText = new TranslatableText("state." + LMRBMod.MODID + "." + movingMode.getName());
        owner.getModeName().ifPresent(
                modeName -> stateText.append(" : ")
                        .append(new TranslatableText("mode." + LMRBMod.MODID + "." + modeName)));
        return stateText;
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        //少し重たいかもしれないが、screenを開く直前にsetModeNameした場合に取得がズレるので毎tickやる
        stateText = getStateText();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        super.render(matrices, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
        InventoryScreen.drawEntity(
                (this.width - this.backgroundWidth) / 2 + 52,
                (this.height - this.backgroundHeight) / 2 + 59,
                20,
                (this.width - this.backgroundWidth) / 2F + 52 - mouseX,
                (this.height - this.backgroundHeight) / 2F + 30 - mouseY, owner);

        if (showSalaryWindow) {
            salaryWindow.render(matrices, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showSalaryWindow) {
            if (!salaryWindow.mouseClicked(mouseX, mouseY, button)) {
                showSalaryWindow = false;
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (showSalaryWindow && salaryWindow.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (showSalaryWindow && salaryWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        this.textRenderer.draw(matrices, this.stateText.getString(), 8F, 65F, 0x404040);
        String insideSkirt = new TranslatableText("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getString();
        this.textRenderer.draw(matrices, insideSkirt, 168F - textRenderer.getWidth(insideSkirt), 65F, 0x404040);
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        if (left + 7 <= mouseX && mouseX < left + 96 && top + 7 <= mouseY && mouseY < top + 60) {
            drawArmor(matrices);
        } else {
            drawHealth(matrices, mouseX, mouseY);
        }
    }

    protected void drawHealth(MatrixStack matrices, int mouseX, int mouseY) {
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        if (left + 98 <= mouseX && mouseX < left + 98 + 5 * 9 && top + 7 <= mouseY && mouseY < top + 7 + 2 * 9) {
            String healthStr = MathHelper.ceil(owner.getHealth()) + " / " + MathHelper.ceil(owner.getMaxHealth());
            this.textRenderer.draw(matrices, healthStr,
                    98F + (5F * 9F - textRenderer.getWidth(healthStr)) / 2F,
                    16F - textRenderer.fontHeight / 2F, 0x404040);
        } else {
            float health = (owner.getHealth() / owner.getMaxHealth()) * 20F;
            drawHealth(matrices, 98, 7, MathHelper.clamp(health - 10, 0, 10), 5);
            drawHealth(matrices, 98, 16, MathHelper.clamp(health, 0, 10), 5);
        }
        RenderSystem.setShaderTexture(0, GUI);
    }

    protected void drawArmor(MatrixStack matrices) {
        float armor = owner.getArmor();
        drawArmor(matrices, 98, 7, MathHelper.clamp(armor - 10, 0, 10), 5);
        drawArmor(matrices, 98, 16, MathHelper.clamp(armor, 0, 10), 5);
    }

    protected void drawHealth(MatrixStack matrices, int x, int y, float health, int rowHeart) {
        drawIcon(matrices, x, y, health, rowHeart, 16, 0, 52, 0, 61, 0);
    }

    protected void drawArmor(MatrixStack matrices, int x, int y, float health, int rowHeart) {
        drawIcon(matrices, x, y, health, rowHeart, 16, 9, 34, 9, 25, 9);
    }

    protected void drawIcon(MatrixStack matrices, int x, int y, float num, int row,
                            int baseU, int baseV, int overU, int overV, int halfU, int halfV) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, ICONS);
        for (int i = 0; i < row; i++) {
            this.drawTexture(matrices, x + i * 9, y, baseU, baseV, 9, 9);
            if (1 < num) {
                this.drawTexture(matrices, x + i * 9, y, overU, overV, 9, 9);
            } else if (0 < num) {
                this.drawTexture(matrices, x + i * 9, y, halfU, halfV, 9, 9);
            }
            num -= 2;
        }
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.backgroundWidth) / 2;
        int relY = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, relX, relY, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void close() {
        super.close();
        SyncMovingStatePacket.sendC2SPacket(owner, movingMode);
    }

    public static class SalaryGUI extends GUIElement {
        private final ItemRenderer itemRenderer;
        private final TextRenderer textRenderer;
        private final int maxUnpaidDays;
        private final int unpaidDays;

        protected SalaryGUI(int width, int height, int x, int y, ItemRenderer itemRenderer, TextRenderer textRenderer, int maxUnpaidDays, int unpaidDays) {
            super(width, height);
            this.x = x;
            this.y = y;
            this.itemRenderer = itemRenderer;
            this.textRenderer = textRenderer;
            this.maxUnpaidDays = maxUnpaidDays;
            this.unpaidDays = unpaidDays;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            matrices.push();
            RenderSystem.enableDepthTest();
            String unpaid = (maxUnpaidDays - unpaidDays) + " / " + maxUnpaidDays;
            int textWidth = textRenderer.getWidth(unpaid);
            matrices.translate(0, 0, 300);
            textRenderer.draw(matrices, unpaid,
                    this.x + this.width / 2f - textWidth / 2f,
                    this.y + this.height / 2f - textRenderer.fontHeight / 2f, 0x0);
            matrices.pop();
        }

    }

}
