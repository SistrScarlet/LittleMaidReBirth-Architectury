package net.sistr.littlemaidrebirth.client.screen;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.client.screen.component.FilterPredicate;
import net.sistr.littlemaidmodelloader.client.screen.component.FilterableListGUI;
import net.sistr.littlemaidmodelloader.client.screen.component.GUIElement;
import net.sistr.littlemaidmodelloader.client.screen.component.ListGUIElement;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.network.C2SCallWaitPacket;
import net.sistr.littlemaidrebirth.network.C2SOpenInventoryPacket;

/** メイドさん管理情報を表示するためのスクリーン */
@Environment(EnvType.CLIENT)
public class MaidManagerScreen extends Screen {
    private final List<MaidManager.LMInfo> lmInfoList;
    private FilterableListGUI<LMInfoGUIElement> lmInfoGui;

    public MaidManagerScreen(List<MaidManager.LMInfo> lmInfoList) {
        super(Text.translatable("gui.littlemaidrebirth.maidmanager.title"));
        this.lmInfoList = lmInfoList;
    }

    @Override
    protected void init() {
        if (this.client == null || this.client.world == null) {
            return;
        }

        int searchInputHeight = 20;
        int elementWidth = textRenderer.fontHeight * 18;
        int elementHeight = textRenderer.fontHeight * 6 + 20;
        int widthStack = MathHelper.floor(this.width * 0.8f / elementWidth);
        int totalWidth = elementWidth * widthStack;
        int totalHeight = MathHelper.floor(this.height * 0.9f);

        // LMInfo用のFilterPredicate（名前とステータスで検索）
        FilterPredicate<LMInfoGUIElement> lmInfoFilter =
                (lmInfoGUIElement, filterText) -> {
                    String searchStr =
                            (lmInfoGUIElement.getLMInfo().name()
                                            + ","
                                            + lmInfoGUIElement.getLMInfo().status().name())
                                    .toLowerCase();
                    return searchStr.contains(filterText.toLowerCase());
                };

        // LMInfoのリストをGUI要素に変換
        String currentWorldId = this.client.world.getRegistryKey().getValue().toString();

        List<LMInfoGUIElement> elements =
                lmInfoList.stream()
                        .map(info -> new LMInfoGUIElement(this.client.textRenderer, info))
                        .sorted(createSortComparator(currentWorldId))
                        .collect(Collectors.toList());

        this.lmInfoGui =
                FilterableListGUI.<LMInfoGUIElement>builder()
                        .position(
                                MathHelper.floor((this.width - totalWidth) / 2f),
                                MathHelper.floor((this.height - totalHeight) / 2f))
                        .size(totalWidth, totalHeight)
                        .elementSize(elementWidth, elementHeight)
                        .items(elements)
                        .filterBy(lmInfoFilter)
                        .withScrollBar()
                        .searchInputHeight(searchInputHeight)
                        .withPlaceholder("Search maids...")
                        .build();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x40000000);

        lmInfoGui.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return lmInfoGui.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return lmInfoGui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return lmInfoGui.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return lmInfoGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return lmInfoGui.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (lmInfoGui.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private static Comparator<LMInfoGUIElement> createSortComparator(String currentWorldId) {
        return Comparator
                // 1. ステータス（最優先）
                .comparing((LMInfoGUIElement e) -> e.getLMInfo().status().name())
                // 2. ワールド優先度（同一ワールドを優先）
                .thenComparing(
                        e -> {
                            String worldId = e.getLMInfo().getWorldId();
                            return worldId.equals(currentWorldId) ? 0 : 1;
                        })
                // 3. ワールド名（異なるワールド間でのソート）
                .thenComparing(e -> e.getLMInfo().getWorldId())
                // 4. 名前
                .thenComparing(e -> e.getLMInfo().name())
                // 5. 距離（同一ワールドの場合のみ）
                .thenComparing(
                        e -> {
                            String worldId = e.getLMInfo().getWorldId();
                            if (!worldId.equals(currentWorldId)) {
                                return Double.MAX_VALUE; // 異なるワールドは最後
                            }

                            var client = MinecraftClient.getInstance();
                            if (client == null || client.player == null) {
                                return Double.MAX_VALUE;
                            }

                            return e.getLMInfo()
                                    .getEntityClient(client.world)
                                    .map(
                                            entity ->
                                                    client.player.squaredDistanceTo(
                                                            entity.getX(),
                                                            entity.getY(),
                                                            entity.getZ()))
                                    .orElse(Double.MAX_VALUE);
                        });
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static class LMInfoGUIElement extends GUIElement implements ListGUIElement {
        private final MaidManager.LMInfo lmInfo;
        private final LittleMaidScreen.IconButtonWidget inventoryButton;
        private final ButtonWidget callWaitButton;

        public LMInfoGUIElement(TextRenderer textRenderer, MaidManager.LMInfo lmInfo) {
            super(textRenderer.fontHeight * 18, textRenderer.fontHeight * 6 + 20);
            this.lmInfo = lmInfo;

            // インベントリボタンを作成
            this.inventoryButton =
                    new LittleMaidScreen.IconButtonWidget(
                            0,
                            0,
                            new ItemStack(Items.CHEST),
                            Text.translatable("gui.littlemaidrebirth.maidmanager.open_inventory"),
                            (button) -> openInventory());
            this.callWaitButton =
                    new ButtonWidget.Builder(
                                    Text.literal("call"),
                                    onPress -> {
                                        lmInfo.getEntityClient(MinecraftClient.getInstance().world)
                                                .filter(e -> e instanceof LittleMaidEntity)
                                                .map(e -> (LittleMaidEntity) e)
                                                .ifPresent(
                                                        e ->
                                                                C2SCallWaitPacket.sendC2SPacket(
                                                                        e,
                                                                        TameableUtil.isWait(e)
                                                                                ? C2SCallWaitPacket
                                                                                        .State.CALL
                                                                                : C2SCallWaitPacket
                                                                                        .State
                                                                                        .WAIT));
                                    })
                            .size(30, 20)
                            .build();
        }

        private boolean canInteractWithMaid() {
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) {
                return false;
            }

            // ワールドの同一性をチェック
            String worldId = lmInfo.getWorldId();
            if (worldId.isEmpty()
                    || !worldId.equals(client.world.getRegistryKey().getValue().toString())) {
                return false;
            }

            return lmInfo.getEntityClient(client.world)
                    .map(
                            entity -> {
                                // 8ブロック以内かチェック
                                double squaredDistance =
                                        client.player.squaredDistanceTo(
                                                entity.getX(), entity.getY(), entity.getZ());
                                return squaredDistance < 64.0; // 8 * 8
                            })
                    .orElse(false);
        }

        private void openInventory() {
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null) {
                return;
            }

            // エンティティが存在し、8ブロック以内の場合のみインベントリを開く
            if (canInteractWithMaid()) {
                lmInfo.getEntityClient(client.world)
                        .ifPresent(
                                entity -> {
                                    C2SOpenInventoryPacket.sendC2SPacket(entity);
                                });
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            var client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) {
                return;
            }
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            // 1. リトルメイド名を表示
            context.fill(
                    this.x,
                    this.y,
                    this.x + this.width - textRenderer.fontHeight,
                    this.y + textRenderer.fontHeight,
                    0xFF000000);
            String name =
                    lmInfo.getEntityClient(client.world)
                            .map(entity -> entity.getName().getString())
                            .orElse(lmInfo.name());
            drawScrollingText(
                    context,
                    textRenderer,
                    name,
                    this.x,
                    this.y,
                    this.width - textRenderer.fontHeight,
                    0xFFFFFFFF,
                    false);

            // 2. ステータス / ロード状態を表示
            var statusText = lmInfo.status().getText().copy();

            // SOUL_WITHIN以外の場合はロード状態も表示
            if (lmInfo.status() != MaidManager.Status.SOUL_WITHIN) {
                var loadedText =
                        lmInfo.isLoaded()
                                ? Text.literal("Loaded").formatted(Formatting.GRAY)
                                : Text.literal("Unloaded").formatted(Formatting.GRAY);
                statusText =
                        statusText
                                .append(Text.literal(" / ").formatted(Formatting.GRAY))
                                .append(loadedText);
            }

            context.drawText(
                    textRenderer,
                    statusText,
                    this.x,
                    this.y + textRenderer.fontHeight,
                    0xFFCCCCCC,
                    true);

            // 3. ワールド名を表示
            String worldId = lmInfo.getWorldId();
            if (!worldId.isEmpty()) {
                drawScrollingText(
                        context,
                        textRenderer,
                        worldId,
                        this.x,
                        this.y + textRenderer.fontHeight * 2,
                        this.width - textRenderer.fontHeight * 3,
                        0xFFAAAAAA,
                        true);
            }

            // 4. XYZ座標と距離を表示（worldIdが空でない場合のみ）
            if (!worldId.isEmpty()) {
                BlockPos pos =
                        lmInfo.getEntityClient(client.world)
                                .map(entity -> entity.getBlockPos())
                                .orElse(lmInfo.getLastPos());

                var coordText =
                        Text.literal(
                                        String.format(
                                                "XYZ: %d, %d, %d",
                                                pos.getX(), pos.getY(), pos.getZ()))
                                .copy();

                // 距離計算（同一ワールドの場合のみ）
                if (worldId.equals(client.world.getRegistryKey().getValue().toString())) {
                    double squaredDistance =
                            client.player.squaredDistanceTo(
                                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    double distance = Math.sqrt(squaredDistance);

                    // 8ブロック以内なら白、そうでないならグレー
                    Formatting distanceColor = distance <= 8.0 ? Formatting.WHITE : Formatting.GRAY;
                    Text distanceText =
                            Text.literal(String.format(" (%.0fm)", distance))
                                    .formatted(distanceColor);
                    coordText.append(distanceText);
                }

                drawScrollingText(
                        context,
                        textRenderer,
                        coordText,
                        this.x,
                        this.y + textRenderer.fontHeight * 3,
                        this.width - textRenderer.fontHeight * 3,
                        0xFFAAAAAA,
                        true);
            }

            lmInfo.getEntityClient(client.world)
                    .filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e)
                    .ifPresent(
                            e -> {
                                // モード名を表示
                                if (e instanceof LittleMaidEntity littleMaid) {
                                    littleMaid
                                            .getModeName()
                                            .ifPresent(
                                                    modeName ->
                                                            context.drawText(
                                                                    textRenderer,
                                                                    modeName,
                                                                    this.x,
                                                                    this.y
                                                                            + textRenderer
                                                                                            .fontHeight
                                                                                    * 4,
                                                                    0xFFFFFFFF,
                                                                    true));
                                }

                                // エンティティを描画（右側）
                                int entityX = this.x + this.width - 20;
                                int entityY = this.y + this.height - textRenderer.fontHeight;
                                int entitySize = 20;
                                InventoryScreen.drawEntity(
                                        context,
                                        entityX - 25,
                                        entityY - 50,
                                        entityX + 25,
                                        entityY + 8,
                                        entitySize,
                                        0.0625f,
                                        (float) (entityX - 20),
                                        (float) (entityY - 21),
                                        e);
                            });

            if (canInteractWithMaid()) {
                // インベントリボタン、コールウェイトボタンを左下に配置
                int buttonX = this.x;
                int buttonY = this.y + this.height - textRenderer.fontHeight - 20;
                inventoryButton.setPosition(buttonX, buttonY);
                inventoryButton.render(context, mouseX, mouseY, delta);
                lmInfo.getEntityClient(client.world)
                        .filter(e -> e instanceof LittleMaidEntity)
                        .map(e -> (LittleMaidEntity) e)
                        .ifPresent(
                                e -> {
                                    if (TameableUtil.isWait(e)) {
                                        callWaitButton.setMessage(Text.literal("call"));
                                    } else {
                                        callWaitButton.setMessage(Text.literal("wait"));
                                    }
                                });
                buttonX += inventoryButton.getWidth();
                callWaitButton.setPosition(buttonX, buttonY);
                callWaitButton.render(context, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // インベントリボタンのクリック処理
            if (canInteractWithMaid()
                    && (inventoryButton.mouseClicked(mouseX, mouseY, button)
                            || callWaitButton.mouseClicked(mouseX, mouseY, button))) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            // インベントリボタンのリリース処理
            if (canInteractWithMaid()
                    && (inventoryButton.mouseReleased(mouseX, mouseY, button)
                            || callWaitButton.mouseReleased(mouseX, mouseY, button))) {
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public void setSelected(boolean selected) {
            // 必要に応じて実装
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        public MaidManager.LMInfo getLMInfo() {
            return lmInfo;
        }

        /** 長いテキストをスクロール表示するヘルパーメソッド */
        private void drawScrollingText(
                DrawContext context,
                TextRenderer textRenderer,
                String text,
                int x,
                int y,
                int availableWidth,
                int color,
                boolean shadow) {
            drawScrollingText(
                    context, textRenderer, Text.of(text), x, y, availableWidth, color, shadow);
        }

        private void drawScrollingText(
                DrawContext context,
                TextRenderer textRenderer,
                Text text,
                int x,
                int y,
                int availableWidth,
                int color,
                boolean shadow) {
            int textWidth = textRenderer.getWidth(text);
            if (textWidth <= availableWidth) {
                context.drawText(textRenderer, text, x, y, color, shadow);
            } else {
                // 長すぎるテキストをスクロール表示
                double seconds = Util.getMeasuringTimeMs() / 1000.0;
                double scrollSpeed = 20.0;
                int displayWidth = availableWidth - 8;
                int scrollDistance = textWidth - displayWidth;
                double cycleTime = (scrollDistance + displayWidth) / scrollSpeed;
                double cyclePosition = (seconds % cycleTime) / cycleTime;

                int scrollOffset;
                if (cyclePosition < 0.8) {
                    scrollOffset = (int) (cyclePosition / 0.8 * scrollDistance);
                } else {
                    scrollOffset = scrollDistance;
                }

                context.enableScissor(x, y, x + displayWidth, y + textRenderer.fontHeight);
                context.drawText(textRenderer, text, x - scrollOffset, y, color, shadow);
                context.disableScissor();
            }
        }
    }
}
