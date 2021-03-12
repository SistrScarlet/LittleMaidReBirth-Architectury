package net.sistr.littlemaidrebirth.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
import net.sistr.lmml.client.ModelSelectScreen;
import net.sistr.lmml.resource.manager.LMConfigManager;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.entity.Tameable;
import net.sistr.littlemaidrebirth.network.OpenIFFScreenPacket;
import net.sistr.littlemaidrebirth.network.SyncMovingStatePacket;
import net.sistr.littlemaidrebirth.network.SyncSoundConfigPacket;

//todo 体力/防御力表示、モード名表示/移動状態をアイコンで表記
@Environment(EnvType.CLIENT)
public class LittleMaidScreen extends HandledScreen<LittleMaidScreenHandler> {
    private static final Identifier GUI =
            new Identifier("lmreengaged", "textures/gui/container/littlemaidinventory2.png");
    private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    private static final ItemStack ARMOR = Items.LEATHER_CHESTPLATE.getDefaultStack();
    private static final ItemStack BOOK = Items.BOOK.getDefaultStack();
    private static final ItemStack NOTE = Items.NOTE_BLOCK.getDefaultStack();
    private static final ItemStack FEATHER = Items.FEATHER.getDefaultStack();
    private final LittleMaidEntity openAt;
    private Text stateText;

    public LittleMaidScreen(LittleMaidScreenHandler screenContainer, PlayerInventory inv, Text titleIn) {
        super(screenContainer, inv, titleIn);
        this.backgroundHeight = 208;
        openAt = screenContainer.getGuiEntity();
    }

    @Override
    protected void init() {
        super.init();
        if (openAt == null) {
            client.openScreen(null);
            return;
        }
        int left = (int) ((this.width - backgroundWidth) / 2F) - 5;
        int top = (int) ((this.height - backgroundHeight) / 2F);
        int size = 20;
        int layer = -1;
        this.addButton(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> OpenIFFScreenPacket.sendC2SPacket(openAt)) {
            @Override
            public void renderButton(MatrixStack matrices, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrices, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderGuiItemIcon(BOOK, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addButton(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> {
                    openAt.setConfigHolder(LMConfigManager.INSTANCE.getAnyConfig());
                    SyncSoundConfigPacket.sendC2SPacket(openAt, openAt.getConfigHolder().getName());
                }, (button, matrices, x, y) -> {
            String text = openAt.getConfigHolder().getName();
            float renderX = Math.max(0, x - textRenderer.getWidth(text) / 2F);
            textRenderer.drawWithShadow(matrices, text, renderX,
                    y - textRenderer.fontHeight / 2F, 0xFFFFFF);
        }) {
            @Override
            public void renderButton(MatrixStack matrices, int x, int y, float delta) {
                super.renderButton(matrices, x, y, delta);
                itemRenderer.renderGuiItemIcon(NOTE, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addButton(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> client.openScreen(new ModelSelectScreen(title, openAt.world, openAt))) {
            @Override
            public void renderButton(MatrixStack matrices, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrices, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderGuiItemIcon(ARMOR, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addButton(new ButtonWidget(left - size, top + size * ++layer, size, size, new LiteralText(""),
                button -> {
                    openAt.setMovingState(openAt.getMovingState() == Tameable.MovingState.FREEDOM
                            ? Tameable.MovingState.WAIT
                            : Tameable.MovingState.FREEDOM);
                    stateText = getStateText();
                }) {
            @Override
            public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                super.renderButton(matrices, mouseX, mouseY, delta);
                itemRenderer.renderGuiItemIcon(FEATHER, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        stateText = getStateText();
    }

    public Text getStateText() {
        MutableText stateText = new TranslatableText("state." + LittleMaidReBirthMod.MODID + "." + openAt.getMovingState().getName());
        openAt.getModeName().ifPresent(
                modeName -> stateText.append(" : ")
                        .append(new TranslatableText("mode." + LittleMaidReBirthMod.MODID + "." + modeName)));
        return stateText;
    }

    @Override
    public void tick() {
        super.tick();
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
                (this.height - this.backgroundHeight) / 2F + 30 - mouseY, openAt);
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
            String healthStr = MathHelper.ceil(openAt.getHealth()) + " / " + MathHelper.ceil(openAt.getMaxHealth());
            this.textRenderer.draw(matrices, healthStr,
                    98F + (5F * 9F - textRenderer.getWidth(healthStr)) / 2F,
                    16F - textRenderer.fontHeight / 2F, 0x404040);
        } else {
            float health = (openAt.getHealth() / openAt.getMaxHealth()) * 20F;
            drawHealth(matrices, 98, 7, MathHelper.clamp(health - 10, 0, 10), 5);
            drawHealth(matrices, 98, 16, MathHelper.clamp(health, 0, 10), 5);
        }
        this.client.getTextureManager().bindTexture(GUI);
    }

    protected void drawArmor(MatrixStack matrices) {
        float armor = openAt.getArmor();
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
        this.client.getTextureManager().bindTexture(ICONS);
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
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        assert this.client != null;
        this.client.getTextureManager().bindTexture(GUI);
        int relX = (this.width - this.backgroundWidth) / 2;
        int relY = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, relX, relY, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void onClose() {
        super.onClose();
        SyncMovingStatePacket.sendC2SPacket(openAt, openAt.getMovingState());
    }
}
