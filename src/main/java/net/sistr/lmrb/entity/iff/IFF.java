package net.sistr.lmrb.entity.iff;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

public class IFF {
    protected IFFTag iffTag;
    protected IFFType iffType;
    protected EntityType<?> entityType;

    public IFF(IFFTag iffTag, IFFType iffType, EntityType<?> entityType) {
        this.iffTag = iffTag;
        this.iffType = iffType;
        this.entityType = entityType;
    }

    public boolean identify(LivingEntity entity) {
        return entity.getType() == entityType;
    }

    public CompoundTag writeTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("IFFTag", iffTag.getId());
        tag.putString("IFFType", IFFTypeManager.getINSTANCE().getId(iffType)
                .orElseThrow(() -> new RuntimeException("存在しないIFFTypeです。")).toString());
        tag.putString("EntityType", EntityType.getId(entityType).toString());
        return tag;
    }

    public IFF readTag(CompoundTag tag) {
        iffTag = IFFTag.getTagFromId(tag.getInt("IFFTag"));
        iffType = IFFTypeManager.getINSTANCE().getIFFType(new Identifier(tag.getString("IFFType")))
                .orElseThrow(() -> new RuntimeException("存在しないIFFTypeです。"));
        entityType = EntityType.get(tag.getString("EntityType"))
                .orElseThrow(() -> new RuntimeException("存在しないEntityTypeです。"));
        return this;
    }

    public IFFTag getIFFTag() {
        return iffTag;
    }

    public IFFType getIFFType() {
        return iffType;
    }

    public IFF setTag(IFFTag iffTag) {
        this.iffTag = iffTag;
        return this;
    }
}
