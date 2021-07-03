package net.sistr.littlemaidrebirth.api.mode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModeManager {
    public static ModeManager INSTANCE = new ModeManager();
    private final BiMap<Identifier, ModeType<? extends Mode>> MODE_TYPES = HashBiMap.create();

    public void register(Identifier id, ModeType<? extends Mode> type) {
        MODE_TYPES.put(id, type);
    }

    public Optional<Identifier> getId(Mode mode) {
        return Optional.ofNullable(MODE_TYPES.inverse().get(mode.getModeType()));
    }

    public Collection<Mode> getModes(LittleMaidEntity maid) {
        return MODE_TYPES.values().stream().map(type -> type.create(maid)).collect(Collectors.toList());
    }

}
