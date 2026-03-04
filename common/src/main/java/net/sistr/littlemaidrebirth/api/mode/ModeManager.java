package net.sistr.littlemaidrebirth.api.mode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

/** モードタイプを管理するクラス メイド専用 */
public class ModeManager {
  public static final ModeManager INSTANCE = new ModeManager();
  private final BiMap<Identifier, ModeType<? extends Mode>> MODE_TYPES = HashBiMap.create();

  public void register(Identifier id, ModeType<? extends Mode> type) {
    MODE_TYPES.put(id, type);
  }

  public Optional<Identifier> getId(Mode mode) {
    return getId(mode.getModeType());
  }

  public Optional<Identifier> getId(ModeType<?> modeType) {
    return Optional.ofNullable(MODE_TYPES.inverse().get(modeType));
  }

  public Optional<ModeType<? extends Mode>> getType(Identifier id) {
    return Optional.ofNullable(MODE_TYPES.get(id));
  }

  /** メイドのモードを新規作成 */
  public Collection<Mode> createModes(LittleMaidEntity maid) {
    return MODE_TYPES.values().stream().map(type -> type.create(maid)).collect(Collectors.toList());
  }
}
