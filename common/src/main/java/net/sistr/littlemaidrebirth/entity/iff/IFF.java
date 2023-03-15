package net.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * エンティティを識別するクラス
 */
public class IFF {
    protected IFFTag iffTag;
    protected IFFType iffType;
    protected EntityType<?> entityType;

    public IFF(IFFTag iffTag, IFFType iffType, EntityType<?> entityType) {
        this.iffTag = iffTag;
        this.iffType = iffType;
        this.entityType = entityType;
    }

    /**
     * エンティティが対象かチェックする
     */
    public boolean identify(LivingEntity entity) {
        return entity.getType() == entityType;
    }

    /**
     * IFFの内容を書き出す
     */
    public NbtCompound writeTag() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("IFFTag", iffTag.getId());
        nbt.putString("IFFType", IFFTypeManager.getINSTANCE().getId(iffType)
                .orElseThrow(() -> new RuntimeException("存在しないIFFTypeです。")).toString());
        nbt.putString("EntityType", EntityType.getId(entityType).toString());
        return nbt;
    }

    /**
     * IFFの内容を上書きする
     */
    public IFF readTag(NbtCompound nbt) {
        iffTag = IFFTag.getTagFromId(nbt.getInt("IFFTag"));
        iffType = IFFTypeManager.getINSTANCE().getIFFType(new Identifier(nbt.getString("IFFType")))
                .orElseThrow(() -> new RuntimeException("存在しないIFFTypeです。"));
        entityType = EntityType.get(nbt.getString("EntityType"))
                .orElseThrow(() -> new RuntimeException("存在しないEntityTypeです。"));
        return this;
    }

    public IFFTag getIFFTag() {
        return iffTag;
    }

    public IFFType getIFFType() {
        return iffType;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public IFF setTag(IFFTag iffTag) {
        this.iffTag = iffTag;
        return this;
    }
}
