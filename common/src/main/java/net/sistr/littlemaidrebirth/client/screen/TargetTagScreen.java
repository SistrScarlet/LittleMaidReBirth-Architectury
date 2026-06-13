package net.sistr.littlemaidrebirth.client.screen;

import java.util.*;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.client.screen.component.*;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;
import net.sistr.littlemaidrebirth.network.C2SSetTargetTagsPacket;
import org.lwjgl.glfw.GLFW;

/** TargetTagを設定するためのスクリーン 閉じたときにパケットで結果を返す */
@Environment(EnvType.CLIENT)
public class TargetTagScreen extends Screen {
  private final Entity entity;
  private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags;
  private FilterableListGUI<TargetTagGUIElement> targetTagGui;

  public TargetTagScreen(
      Entity entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
    super(Text.empty());
    this.entity = entity;
    this.targetTags = new HashMap<>(targetTags);
  }

  @Override
  protected void init() {
    assert this.client != null;

    int searchInputHeight = 20;
    int elementWidth = textRenderer.fontHeight * 15;
    int elementHeight = textRenderer.fontHeight + 40;
    int widthStack = MathHelper.floor(this.width * 0.8f / elementWidth);
    int totalWidth = elementWidth * widthStack;

    // TargetTag用のFilterPredicate（エンティティタイプのキーと翻訳名で検索）
    FilterPredicate<TargetTagGUIElement> targetTagFilter =
        (targetTagGUIElement, filterText) -> {
          var type = targetTagGUIElement.getTargetIdentifier().getEntityType();
          String searchStr =
              (targetTagGUIElement.getTargetIdentifier().toString()
                      + ","
                      + type.getName().getString())
                  .toLowerCase();
          return searchStr.contains(filterText.toLowerCase());
        };

    // すべてのターゲットタグをリストアップ
    List<TargetTagGUIElement> elements =
        targetTags.entrySet().stream()
            .map(
                entry ->
                    new TargetTagGUIElement(
                        this.client.textRenderer, entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(e -> e.targetIdentifier.toString()))
            .collect(Collectors.toList());

    this.targetTagGui =
        FilterableListGUI.<TargetTagGUIElement>builder()
            .position(MathHelper.floor((this.width - totalWidth) / 2f), 0)
            .size(totalWidth, this.height)
            .elementSize(elementWidth, elementHeight)
            .items(elements)
            .filterBy(targetTagFilter)
            .withScrollBar()
            .searchInputHeight(searchInputHeight)
            .withPlaceholder("Search entities...")
            .build();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    context.fill(0, 0, this.width, this.height, 0x40000000);

    targetTagGui.render(context, mouseX, mouseY, delta);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    return targetTagGui.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(
      double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    return targetTagGui.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    return targetTagGui.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    return targetTagGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (super.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }
    return targetTagGui.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int modifiers) {
    if (targetTagGui.charTyped(chr, modifiers)) {
      return true;
    }
    return super.charTyped(chr, modifiers);
  }

  @Override
  public void removed() {
    super.removed();
    // 全てのtargetTagsを収集
    Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> updatedTargetTags = new HashMap<>();
    for (TargetTagGUIElement element : targetTagGui.getListGUI().getAllElements()) {
      updatedTargetTags.put(element.getTargetIdentifier(), element.getTags());
    }
    send(updatedTargetTags);
  }

  public <T extends Entity & TargetTagManager> void send(
      Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> updatedTargetTags) {
    //noinspection unchecked
    C2SSetTargetTagsPacket.sendC2SPacket((T) entity, updatedTargetTags);
  }

  public static class TargetTagGUIElement extends GUIElement implements ListGUIElement {
    // 攻撃カテゴリの状態
    private enum AttackState {
      ATTACK_PROHIBITED("attack_prohibited", Items.BARRIER.getDefaultStack()),
      PREEMPTIVE_ATTACK_PROHIBITED("preemptive_attack_prohibited", Items.SHIELD.getDefaultStack()),
      PREEMPTIVE_ATTACK_ALLOWED("preemptive_attack_allowed", Items.IRON_SWORD.getDefaultStack());

      private final String translationKey;
      private final ItemStack icon;

      AttackState(String translationKey, ItemStack icon) {
        this.translationKey = translationKey;
        this.icon = icon;
      }

      public AttackState next() {
        return values()[(ordinal() + 1) % values().length];
      }
    }

    // 武器カテゴリの状態
    private enum WeaponState {
      NO_WEAPON_RESTRICTION("no_weapon_restriction", Items.AIR.getDefaultStack()),
      MELEE_WEAPON_PROHIBITED("melee_weapon_prohibited", Items.IRON_SWORD.getDefaultStack()),
      RANGED_WEAPON_PROHIBITED("ranged_weapon_prohibited", Items.BOW.getDefaultStack());

      private final String translationKey;
      private final ItemStack icon;

      WeaponState(String translationKey, ItemStack icon) {
        this.translationKey = translationKey;
        this.icon = icon;
      }

      public WeaponState next() {
        return values()[(ordinal() + 1) % values().length];
      }
    }

    // 接近カテゴリの状態
    private enum ApproachState {
      APPROACH_ALLOWED("approach_allowed", Items.AIR.getDefaultStack()),
      APPROACH_PROHIBITED("approach_prohibited", Items.CREEPER_HEAD.getDefaultStack());

      private final String translationKey;
      private final ItemStack icon;

      ApproachState(String translationKey, ItemStack icon) {
        this.translationKey = translationKey;
        this.icon = icon;
      }

      public ApproachState next() {
        return values()[(ordinal() + 1) % values().length];
      }
    }

    private final TargetIdentifier targetIdentifier;
    private final MarginedClickable clickable = new MarginedClickable(4);
    private final List<ButtonWidget> buttons;
    private AttackState attackState;
    private WeaponState weaponState;
    private ApproachState approachState;

    public TargetTagGUIElement(
        TextRenderer textRenderer,
        TargetIdentifier targetIdentifier,
        Set<TargetingSystem.TargetTag> tags) {
      super(textRenderer.fontHeight * 15, textRenderer.fontHeight + 40);
      this.targetIdentifier = targetIdentifier;

      // 現在のタグセットから状態を決定
      this.attackState = determineAttackState(tags);
      this.weaponState = determineWeaponState(tags);
      this.approachState = determineApproachState(tags);

      this.buttons = new ArrayList<>(3);

      // 攻撃ボタン
      this.buttons.add(
          new LittleMaidScreen.IconButtonWidget(
              0,
              0,
              this.attackState.icon,
              Text.translatable(
                  "gui.littlemaidrebirth.target_tag.tags." + this.attackState.translationKey),
              (b) -> this.attackState = this.attackState.next()));

      // 武器ボタン
      this.buttons.add(
          new LittleMaidScreen.IconButtonWidget(
              0,
              0,
              this.weaponState.icon,
              Text.translatable(
                  "gui.littlemaidrebirth.target_tag.tags." + this.weaponState.translationKey),
              (b) -> this.weaponState = this.weaponState.next()));

      // 接近ボタン
      this.buttons.add(
          new LittleMaidScreen.IconButtonWidget(
              0,
              0,
              this.approachState.icon,
              Text.translatable(
                  "gui.littlemaidrebirth.target_tag.tags." + this.approachState.translationKey),
              (b) -> this.approachState = this.approachState.next()));
    }

    private AttackState determineAttackState(Set<TargetingSystem.TargetTag> tags) {
      if (tags.contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED)) {
        return AttackState.ATTACK_PROHIBITED;
      } else if (tags.contains(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED)) {
        return AttackState.PREEMPTIVE_ATTACK_PROHIBITED;
      } else {
        return AttackState.PREEMPTIVE_ATTACK_ALLOWED;
      }
    }

    private WeaponState determineWeaponState(Set<TargetingSystem.TargetTag> tags) {
      boolean meleeProhibited = tags.contains(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
      boolean rangedProhibited = tags.contains(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);

      if (meleeProhibited && !rangedProhibited) {
        return WeaponState.MELEE_WEAPON_PROHIBITED;
      } else if (!meleeProhibited && rangedProhibited) {
        return WeaponState.RANGED_WEAPON_PROHIBITED;
      } else {
        return WeaponState.NO_WEAPON_RESTRICTION;
      }
    }

    private ApproachState determineApproachState(Set<TargetingSystem.TargetTag> tags) {
      return tags.contains(TargetingSystem.TargetTag.APPROACH_PROHIBITED)
          ? ApproachState.APPROACH_PROHIBITED
          : ApproachState.APPROACH_ALLOWED;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

      // エンティティタイプ名を表示
      var entityTypeName =
          Text.translatable(this.targetIdentifier.getEntityType().getTranslationKey());
      int textWidth = textRenderer.getWidth(entityTypeName);
      if (textWidth <= this.width) {
        context.drawText(textRenderer, entityTypeName, this.x, this.y, 0xFFFFFFFF, true);
      } else {
        // 長すぎるテキストをスクロール表示
        double seconds = Util.getMeasuringTimeMs() / 1000.0;
        var entityTypeNameStr = entityTypeName.getString();

        // スクロール速度 (ピクセル/秒)
        double scrollSpeed = 20.0;

        // 表示可能な幅（少し余裕を持たせる）
        int displayWidth = this.width - 8;

        // スクロールが必要な距離
        int scrollDistance = textWidth - displayWidth;

        // 一往復にかかる時間を計算（テキスト幅 + 表示幅分だけスクロール）
        double cycleTime = (scrollDistance + displayWidth) / scrollSpeed;

        // 現在のサイクル内での位置
        double cyclePosition = (seconds % cycleTime) / cycleTime;

        // スクロールオフセットを計算（左→右→左のパターン）
        int scrollOffset;
        if (cyclePosition < 0.8) {
          // 80%の時間で左から右へスクロール
          scrollOffset = (int) (cyclePosition / 0.8 * scrollDistance);
        } else {
          // 20%の時間で一時停止
          scrollOffset = scrollDistance;
        }

        // クリップ領域を設定してテキストを描画
        context.enableScissor(
            this.x, this.y, this.x + displayWidth, this.y + textRenderer.fontHeight);
        context.drawText(
            textRenderer, entityTypeNameStr, this.x - scrollOffset, this.y, 0xFFFFFFFF, true);
        context.disableScissor();
      }

      // ボタンの位置を設定してレンダリング（エンティティタイプ名の下）
      int buttonY = this.y + textRenderer.fontHeight;
      int buttonX = this.x;

      // 攻撃ボタンの状態を更新
      LittleMaidScreen.IconButtonWidget attackButton =
          (LittleMaidScreen.IconButtonWidget) buttons.get(0);
      attackButton.setIconItem(attackState.icon);
      attackButton.setTooltip(
          Tooltip.of(
              Text.translatable(
                  "gui.littlemaidrebirth.target_tag.tags." + attackState.translationKey)));
      attackButton.setPosition(buttonX, buttonY);
      attackButton.render(context, mouseX, mouseY, delta);
      buttonX += attackButton.getWidth();

      // 武器ボタンの状態を更新
      LittleMaidScreen.IconButtonWidget weaponButton =
          (LittleMaidScreen.IconButtonWidget) buttons.get(1);
      weaponButton.setIconItem(weaponState.icon);
      weaponButton.setTooltip(
          Tooltip.of(
              Text.translatable(
                  "gui.littlemaidrebirth.target_tag.tags." + weaponState.translationKey)));
      weaponButton.setPosition(buttonX, buttonY);
      weaponButton.render(context, mouseX, mouseY, delta);
      buttonX += weaponButton.getWidth();

      // 接近ボタンの状態を更新
      LittleMaidScreen.IconButtonWidget approachButton =
          (LittleMaidScreen.IconButtonWidget) buttons.get(2);
      approachButton.setIconItem(approachState.icon);
      approachButton.setTooltip(
          Tooltip.of(
              Text.translatable(
                  "gui.littlemaidrebirth.target_tag.tags." + approachState.translationKey)));
      approachButton.setPosition(buttonX, buttonY);
      approachButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // ボタンのクリック処理を先にチェック
      for (ButtonWidget buttonWidget : this.buttons) {
        if (buttonWidget.mouseClicked(mouseX, mouseY, button)) {
          return true;
        }
      }

      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
        clickable.click(mouseX, mouseY);
      }
      return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
      // ボタンのクリック処理を先にチェック
      for (ButtonWidget buttonWidget : this.buttons) {
        if (buttonWidget.mouseReleased(mouseX, mouseY, button)) {
          return true;
        }
      }
      return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void setSelected(boolean b) {
      // 必要に応じて実装
    }

    @Override
    public boolean isSelected() {
      return false;
    }

    public TargetIdentifier getTargetIdentifier() {
      return targetIdentifier;
    }

    public Set<TargetingSystem.TargetTag> getTags() {
      Set<TargetingSystem.TargetTag> tags = new HashSet<>();

      // 攻撃状態から対応するタグを追加
      switch (attackState) {
        case ATTACK_PROHIBITED -> tags.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
        case PREEMPTIVE_ATTACK_PROHIBITED -> tags.add(
            TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
        case PREEMPTIVE_ATTACK_ALLOWED -> {
          // 先制攻撃許可の場合、何も追加しない
        }
        default -> {}
      }

      // 武器状態から対応するタグを追加
      switch (weaponState) {
        case MELEE_WEAPON_PROHIBITED -> tags.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
        case RANGED_WEAPON_PROHIBITED -> tags.add(
            TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
        case NO_WEAPON_RESTRICTION -> {
          // 武器制限なしの場合、何も追加しない
        }
        default -> {}
      }

      // 接近状態から対応するタグを追加
      switch (approachState) {
        case APPROACH_PROHIBITED -> tags.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
        case APPROACH_ALLOWED -> {
          // 接近許可の場合、何も追加しない
        }
        default -> {}
      }

      return tags;
    }
  }
}
