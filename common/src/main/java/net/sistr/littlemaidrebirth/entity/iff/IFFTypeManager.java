package net.sistr.littlemaidrebirth.entity.iff;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.LMRBMod;

import java.util.Optional;
import java.util.Set;

//IFFを実装するエンティティクラスごとにIFFTypeManagerを増やしても良い

/**
 * IFFタイプを管理するクラス
 */
public class IFFTypeManager {
    private static final IFFTypeManager INSTANCE = new IFFTypeManager();
    private final BiMap<Identifier, IFFType> iffTypes = HashBiMap.create();
    private boolean setup;

    public static IFFTypeManager getINSTANCE() {
        return INSTANCE;
    }

    /**
     * IFFTypeを登録する
     * idが重複している場合、IllegalArgumentExceptionを吐く
     * */
    public void register(Identifier id, IFFType type) {
        if (iffTypes.containsKey(id)) {
            throw new IllegalArgumentException("idが重複しています。：" + id);
        }
        iffTypes.put(id, type);
    }

    private void setup(World world) {
        setup = true;
        Set<IFFType> set = Sets.newHashSet(iffTypes.values());
        for (IFFType type : set) {
            try {
                if (!type.init(world))
                    iffTypes.remove(iffTypes.inverse().get(type));
            } catch (Exception e) {
                LMRBMod.LOGGER.warn("IFFの初期化中に例外が発生しました。:" + type.entityType);
                e.printStackTrace();
                iffTypes.remove(iffTypes.inverse().get(type));
            }
        }
    }

    /**
     * 登録済みのすべてのIFFTypeを得る
     */
    public Set<IFFType> getIFFTypes(World world) {
        if (!setup) setup(world);
        return iffTypes.values();
    }

    /**
     * IFFTypeからIDを返す
     */
    public Optional<Identifier> getId(IFFType iffType) {
        return Optional.ofNullable(iffTypes.inverse().get(iffType));
    }

    /**
     * IDからIFFタイプを返す
     */
    public Optional<IFFType> getIFFType(Identifier id) {
        return Optional.ofNullable(iffTypes.get(id));
    }

    /**
     * NBTからIFFを読み込む
     */
    public Optional<IFF> loadIFF(NbtCompound nbt) {
        return loadIFFType(nbt)
                .map(IFFType::createIFF)
                .map(iff -> iff.readTag(nbt));
    }

    /**
     * NBTからIFFTypeを読み込む
     */
    public Optional<IFFType> loadIFFType(NbtCompound nbt) {
        Identifier id = new Identifier(nbt.getString("IFFType"));
        return getIFFType(id);
    }

}
