package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;

import java.util.*;

public class MaidManagerImpl implements MaidManager {
    private final Map<UUID, LMInfo> maidMap = new HashMap<>();

    @Override
    public void registerMaid(LittleMaidEntity maid) {
        maidMap.put(maid.getUuid(), MaidLMInfo.create(maid, true));
    }

    @Override
    public void registerMaid(MaidSoulEntity soul) {
        maidMap.put(soul.getSoul().getUuid(), SoulEntityLMInfo.create(soul, true));
    }

    @Override
    public void registerMaid(LittleMaidEntity.MaidSoul soul) {
        maidMap.put(soul.getUuid(), SoulLMInfo.create(soul));
    }

    @Override
    public List<LMInfo> getMaidList() {
        return List.copyOf(maidMap.values());
    }

    @Override
    public void writeMaidManager(NbtCompound nbt) {
        write(nbt, this.maidMap.values().stream().toList());
    }

    @Override
    public void readMaidManager(NbtCompound nbt) {
        this.maidMap.clear();
        var list = new ArrayList<LMInfo>();
        read(nbt, list);
        list.forEach(lminfo -> maidMap.put(lminfo.id(), lminfo));
    }

    public static void write(NbtCompound nbt, List<LMInfo> list) {
        var listNbt = new NbtList();
        for (LMInfo info : list) {
            NbtCompound infoNbt = new NbtCompound();
            info.write(infoNbt);
            listNbt.add(infoNbt);
        }
        nbt.put("maidList", listNbt);
    }

    public static void read(NbtCompound nbt, List<LMInfo> list) {
        var listNbt = nbt.getList("maidList", NbtElement.COMPOUND_TYPE);
        for (var element : listNbt) {
            NbtCompound infoNbt = (NbtCompound) element;
            LMInfo info = LMInfo.read(infoNbt);
            list.add(info);
        }
    }

    @Override
    public List<LittleMaidEntity.MaidSoul> getMaidSouls() {
        return this.maidMap.values().stream()
                .filter(lmInfo -> lmInfo.status() == Status.SOUL_WITHIN)
                .map(lmInfo -> ((SoulLMInfo) lmInfo).soul())
                .toList();
    }

    @Override
    public void clearMaidSouls() {
        this.maidMap.values().removeIf(lmInfo -> lmInfo.status() == Status.SOUL_WITHIN);
    }

    @Override
    public void checkMaidUnload() {
        Map<UUID, LMInfo> updates = new HashMap<>();
        
        this.maidMap.values().stream()
                .filter(lmInfo -> lmInfo.status() == Status.ALIVE || lmInfo.status() == Status.SOUL_ENTITY)
                .map(info -> info.getEntity())
                .filter(Optional::isPresent)
                .forEach(o -> {
                    var entity = o.get();
                    // エンティティが死亡 or ワールドが読み込まれていない
                    if (!entity.isAlive() || entity.getServer().getWorld(entity.getWorld().getRegistryKey()) == null) {
                        if (entity instanceof LittleMaidEntity maid) {
                            updates.put(maid.getUuid(), MaidLMInfo.create(maid, false));
                        } else if (entity instanceof MaidSoulEntity soul) {
                            updates.put(soul.getUuid(), SoulEntityLMInfo.create(soul, false));
                        }
                    }
                });
        
        // ストリーム処理が完了してから一括で更新
        this.maidMap.putAll(updates);
    }
}
