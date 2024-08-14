package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.client.screen.GUIElement;
import net.sistr.littlemaidmodelloader.client.screen.ModelSelectScreen;
import net.sistr.littlemaidmodelloader.client.screen.SoundPackSelectScreen;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.network.C2SSetBloodSuckPacket;
import net.sistr.littlemaidrebirth.network.C2SSetMovingStatePacket;
import net.sistr.littlemaidrebirth.network.OpenIFFScreenPacket;

import java.util.function.Supplier;

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
    private final MovingMode prevMovingMode;
    private MovingMode movingMode;

    public LittleMaidScreen(LittleMaidScreenHandler screenContainer, PlayerInventory inv, Text titleIn) {
        super(screenContainer, inv, titleIn);
        this.backgroundHeight = 208;
        owner = screenContainer.getGuiEntity();
        unpaidDays = screenContainer.getUnpaidDays();
        prevMovingMode = movingMode = owner.getMovingMode();
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
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, Text.of(""),
                button -> TameableUtil.getTameOwner(owner).ifPresent(OpenIFFScreenPacket::sendC2SPacket), Supplier::get) {
            @Override
            public void renderButton(DrawContext context, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(context, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                context.drawItem(BOOK, this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, Text.of(""),
                button -> client.setScreen(new SoundPackSelectScreen<>(title, owner)), Supplier::get) {
            @Override
            public void renderButton(DrawContext context, int x, int y, float delta) {
                super.renderButton(context, x, y, delta);
                context.drawItem(NOTE, this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, Text.of(""),
                button -> client.setScreen(new ModelSelectScreen<>(title, owner.getWorld(), owner)), Supplier::get) {
            @Override
            public void renderButton(DrawContext context, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(context, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                context.drawItem(ARMOR, this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, Text.of(""),
                button -> {
                    switch (movingMode) {
                        case ESCORT -> movingMode = MovingMode.FREEDOM;
                        case FREEDOM -> movingMode = MovingMode.TRACER;
                        case TRACER -> movingMode = MovingMode.ESCORT;
                    }
                    stateText = getStateText();
                }, Supplier::get) {
            @Override
            public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                super.renderButton(context, mouseX, mouseY, delta);
                context.drawItem(FEATHER, this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
            }
        });
        this.addDrawableChild(new ButtonWidget(left - size, top + size * ++layer, size, size, Text.of(""),
                button -> C2SSetBloodSuckPacket.sendC2SPacket(this.owner, !this.owner.isBloodSuck()), Supplier::get) {
            @Override
            public void renderButton(DrawContext context, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(context, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                context.drawItem(LittleMaidScreen.this.owner.isBloodSuck() ? IRON_AXE : IRON_SWORD,
                        this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
            }
        });
        this.salaryWindow = new WindowGUIComponent(
                this.width / 2 - 40, this.height / 2 - 40, 80, 80,
                ImmutableList.<GUIElement>builder()
                        .add(new SalaryGUI(80, 80, this.width / 2 - 40, this.height / 2 - 40,
                                this.textRenderer, 7, unpaidDays))
                        .build()) {
            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                context.drawTexture(SALARY_WINDOW_TEXTURE, this.x, this.y, 0, 0, 80, 80, 128, 128);
            }
        };
        this.addDrawableChild(new ButtonWidget(left - size, top + size * (layer += 2), size, size, Text.of(""),
                button -> {//ウィンドウを出す
                    showSalaryWindow = true;
                }, Supplier::get) {
            @Override
            public void renderButton(DrawContext context, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(context, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                context.drawItem(SUGAR, this.getX() - 8 + this.width / 2, this.getY() - 8 + this.height / 2);
            }
        });
        stateText = getStateText();
    }

    public Text getStateText() {
        if (owner.isStrike()) {
            return Text.translatable("state." + LMRBMod.MODID + ".Strike");
        }
        MutableText stateText = Text.translatable("state." + LMRBMod.MODID + "." + movingMode.getName());
        owner.getModeName().ifPresent(
                modeName -> stateText.append(" : ")
                        .append(Text.translatable("mode." + LMRBMod.MODID + "." + modeName)));
        return stateText;
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        //少し重たいかもしれないが、screenを開く直前にsetModeNameした場合に取得がズレるので毎tickやる
        stateText = getStateText();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.render(context, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        InventoryScreen.drawEntity(context,
                (this.width - this.backgroundWidth) / 2 + 52,
                (this.height - this.backgroundHeight) / 2 + 59,
                20,
                (this.width - this.backgroundWidth) / 2F + 52 - mouseX,
                (this.height - this.backgroundHeight) / 2F + 30 - mouseY, owner);

        if (showSalaryWindow) {
            salaryWindow.render(context, mouseX, mouseY, partialTicks);
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
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        context.drawText(textRenderer, this.stateText.getString(), 8, 65, 0x404040, false);
        String insideSkirt = Text.translatable("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getString();
        context.drawText(textRenderer, insideSkirt, 168 - textRenderer.getWidth(insideSkirt), 65, 0x404040, false);
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        if (left + 7 <= mouseX && mouseX < left + 96 && top + 7 <= mouseY && mouseY < top + 60) {
            drawArmor(context);
        } else {
            drawHealth(context, mouseX, mouseY);
        }
    }

    protected void drawHealth(DrawContext context, int mouseX, int mouseY) {
        float left = (width - backgroundWidth) / 2F;
        float top = (height - backgroundHeight) / 2F;
        if (left + 98 <= mouseX && mouseX < left + 98 + 5 * 9 && top + 7 <= mouseY && mouseY < top + 7 + 2 * 9) {
            String healthStr = MathHelper.ceil(owner.getHealth()) + " / " + MathHelper.ceil(owner.getMaxHealth());
            context.drawText(textRenderer, healthStr,
                    98 + (int) ((5 * 9 - textRenderer.getWidth(healthStr)) / 2F),
                    16 - (int) (textRenderer.fontHeight / 2F), 0x404040, false);
        } else {
            float health = (owner.getHealth() / owner.getMaxHealth()) * 20F;
            drawHealth(context, 98, 7, MathHelper.clamp(health - 10, 0, 10), 5);
            drawHealth(context, 98, 16, MathHelper.clamp(health, 0, 10), 5);
        }
        RenderSystem.setShaderTexture(0, GUI);
    }

    protected void drawArmor(DrawContext context) {
        float armor = owner.getArmor();
        drawArmor(context, 98, 7, MathHelper.clamp(armor - 10, 0, 10), 5);
        drawArmor(context, 98, 16, MathHelper.clamp(armor, 0, 10), 5);
    }

    protected void drawHealth(DrawContext context, int x, int y, float health, int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 0, 52, 0, 61, 0);
    }

    protected void drawArmor(DrawContext context, int x, int y, float health, int rowHeart) {
        drawIcon(context, x, y, health, rowHeart, 16, 9, 34, 9, 25, 9);
    }

    protected void drawIcon(DrawContext context, int x, int y, float num, int row,
                            int baseU, int baseV, int overU, int overV, int halfU, int halfV) {
        for (int i = 0; i < row; i++) {
            context.drawTexture(ICONS, x + i * 9, y, baseU, baseV, 9, 9);
            if (1 < num) {
                context.drawTexture(ICONS, x + i * 9, y, overU, overV, 9, 9);
            } else if (0 < num) {
                context.drawTexture(ICONS, x + i * 9, y, halfU, halfV, 9, 9);
            }
            num -= 2;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int relX = (this.width - this.backgroundWidth) / 2;
        int relY = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(GUI, relX, relY, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void close() {
        super.close();
        if (prevMovingMode != movingMode) {
            C2SSetMovingStatePacket.sendC2SPacket(owner, movingMode);
        }
    }

    public static class SalaryGUI extends GUIElement {
        private final TextRenderer textRenderer;
        private final int maxUnpaidDays;
        private final int unpaidDays;

        protected SalaryGUI(int width, int height, int x, int y, TextRenderer textRenderer, int maxUnpaidDays, int unpaidDays) {
            super(width, height);
            this.x = x;
            this.y = y;
            this.textRenderer = textRenderer;
            this.maxUnpaidDays = maxUnpaidDays;
            this.unpaidDays = unpaidDays;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            var matrices = context.getMatrices();
            matrices.push();
            RenderSystem.enableDepthTest();
            String unpaid = (maxUnpaidDays - unpaidDays) + " / " + maxUnpaidDays;
            int textWidth = textRenderer.getWidth(unpaid);
            matrices.translate(0, 0, 300);
            context.drawText(textRenderer, unpaid,
                    (int) (this.x + this.width / 2f - textWidth / 2f),
                    (int) (this.y + this.height / 2f - textRenderer.fontHeight / 2f), 0x0, false);
            matrices.pop();
        }

    }

}
