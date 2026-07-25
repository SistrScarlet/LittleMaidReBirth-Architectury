package net.sistr.littlemaidrebirth.entity.targeting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Npc;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.village.Merchant;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.LMRBMod;

public class TargetTagManagerImpl implements TargetTagManager {
    private static final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> TARGET_TAG_MAP =
            new HashMap<>();
    private static boolean staticInitialized;
    private final World world;
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap =
            new HashMap<>();
    private boolean isInitialized;
    private int hash = -1;

    public TargetTagManagerImpl(World world) {
        this.world = world;
    }

    // コンストラクタで実行するとスタックオーバーフローになるので分離
    private void init() {
        if (!staticInitialized) {
            Registries.ENTITY_TYPE.stream()
                    .filter(type -> type.isSummonable() && type.isSaveable())
                    .map(type -> type.create(world))
                    .filter(e -> e != null)
                    .forEach(
                            e -> {
                                if (!(e instanceof LivingEntity)) {
                                    return;
                                }
                                Set<TargetingSystem.TargetTag> set = new HashSet<>();
                                // 非モンスター系と一部中立モブは先制攻撃禁止
                                if (!(e instanceof Monster)
                                        || e instanceof PiglinEntity
                                        || e instanceof ZombifiedPiglinEntity
                                        || e instanceof EndermanEntity) {
                                    set.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                                }
                                // クリーパー, ウォーデンは接近禁止
                                if (e instanceof CreeperEntity || e instanceof WardenEntity) {
                                    set.add(TargetingSystem.TargetTag.APPROACH_PROHIBITED);
                                    set.add(TargetingSystem.TargetTag.MELEE_WEAPON_PROHIBITED);
                                }
                                // エンダーマンは遠距離攻撃禁止
                                if (e instanceof EndermanEntity) {
                                    set.add(TargetingSystem.TargetTag.RANGED_WEAPON_PROHIBITED);
                                }
                                // ペット系、NPC系は攻撃禁止
                                if (e instanceof TameableEntity
                                        || e instanceof Npc
                                        || e instanceof Merchant
                                        || e instanceof ArmorStandEntity) {
                                    set.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                                    set.add(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
                                }
                                // 先制攻撃禁止かつ非モンスターなら攻撃禁止
                                if (set.contains(
                                                TargetingSystem.TargetTag
                                                        .PREEMPTIVE_ATTACK_PROHIBITED)
                                        && !(e instanceof Monster)) {
                                    set.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                                }
                                // 家畜系は攻撃可
                                if (e instanceof CowEntity
                                        || e instanceof ChickenEntity
                                        || e instanceof SheepEntity
                                        || e instanceof PigEntity
                                        || e instanceof PolarBearEntity
                                        || e instanceof RabbitEntity) {
                                    set.remove(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                                }
                                // ウォーデンは攻撃禁止
                                if (e instanceof WardenEntity) {
                                    set.add(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
                                }
                                TARGET_TAG_MAP.put(new TargetIdentifier(e), set);
                            });
            staticInitialized = true;
            LMRBMod.LOGGER.info("TargetTagMap Count: {}", TARGET_TAG_MAP.size());
        }
        var tmp = new HashMap<>(TARGET_TAG_MAP);
        tmp.putAll(targetTagMap);
        this.targetTagMap.putAll(tmp);
        this.hash = this.targetTagMap.hashCode();
    }

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        if (!this.isInitialized) {
            init();
            this.isInitialized = true;
        }
        if (!this.targetTagMap.containsKey(id)) {
            return Set.of(TargetingSystem.TargetTag.PREEMPTIVE_ATTACK_PROHIBITED);
        }
        return this.targetTagMap.get(id);
    }

    @Override
    public void writeTargetTags(NbtCompound nbt) {
        if (!this.isInitialized) {
            init();
            this.isInitialized = true;
        }
        write(this.targetTagMap, nbt);
    }

    public static void write(
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap, NbtCompound nbt) {
        var list = new NbtList();
        for (Map.Entry<TargetIdentifier, Set<TargetingSystem.TargetTag>> entry :
                targetTagMap.entrySet()) {
            var id = entry.getKey();
            var tags = entry.getValue();
            var listEntry = new NbtCompound();
            listEntry.putString("id", id.toString());
            // NbtList ではなく ByteArray で書く。理由は readTagOrdinals を参照
            var ordinals = new byte[tags.size()];
            int i = 0;
            for (TargetingSystem.TargetTag tag : tags) {
                ordinals[i++] = (byte) tag.ordinal();
            }
            listEntry.putByteArray("tags", ordinals);
            list.add(listEntry);
        }
        nbt.put("targetTagMap", list);
    }

    @Override
    public void readTargetTags(NbtCompound nbt) {
        read(this.targetTagMap, nbt);
        this.hash = this.targetTagMap.hashCode();
    }

    public static void read(
            Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTagMap, NbtCompound nbt) {
        targetTagMap.clear();
        if (!nbt.contains("targetTagMap")) {
            return;
        }
        var list = nbt.getList("targetTagMap", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            var listEntry = list.getCompound(i);
            var id = TargetIdentifier.tryParse(listEntry.getString("id"));
            if (id.isEmpty()) continue;
            var tags = new HashSet<TargetingSystem.TargetTag>();
            for (byte ordinal : readTagOrdinals(listEntry)) {
                if (ordinal >= 0 && ordinal < TargetingSystem.TargetTag.values().length) {
                    tags.add(TargetingSystem.TargetTag.values()[ordinal]);
                }
            }
            targetTagMap.put(id.get(), tags);
        }
    }

    /**
     * tags の ordinal 列を読み取る。ByteArray と NbtList の両形式を受け付ける。
     *
     * <p>{@code NbtOps.createList} は全要素が NbtByte の NbtList を NbtByteArray へ変換する。 シングルプレイのホストは
     * playerdata ではなく level.dat の Player タグから読み込まれ、その経路が Dynamic を通るため型が入れ替わる。{@code
     * getList(BYTE_TYPE)} は型が LIST でないと空を返すので、 NbtList で保存すると再ログイン時に全タグが失われていた。書き込みは ByteArray
     * に統一しつつ、 旧形式で保存された既存データも読めるようにしておく。
     */
    private static byte[] readTagOrdinals(NbtCompound listEntry) {
        if (listEntry.contains("tags", NbtElement.BYTE_ARRAY_TYPE)) {
            return listEntry.getByteArray("tags");
        }
        var tagsList = listEntry.getList("tags", NbtElement.BYTE_TYPE);
        var ordinals = new byte[tagsList.size()];
        for (int i = 0; i < tagsList.size(); i++) {
            ordinals[i] = ((NbtByte) tagsList.get(i)).byteValue();
        }
        return ordinals;
    }

    @Override
    public Sync getTargetTagsSync() {
        return new Sync() {
            @Override
            public int hash() {
                return TargetTagManagerImpl.this.hash;
            }

            @Override
            public Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> getData() {
                return Map.copyOf(TargetTagManagerImpl.this.targetTagMap);
            }

            @Override
            public void syncFrom(Sync source) {
                TargetTagManagerImpl.this.targetTagMap.clear();
                TargetTagManagerImpl.this.targetTagMap.putAll(source.getData());
                TargetTagManagerImpl.this.hash = source.hash();
            }
        };
    }
}
