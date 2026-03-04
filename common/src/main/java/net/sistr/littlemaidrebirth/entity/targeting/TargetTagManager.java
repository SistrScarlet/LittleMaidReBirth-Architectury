package net.sistr.littlemaidrebirth.entity.targeting;

import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;

public interface TargetTagManager {
  Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id);

  void writeTargetTags(NbtCompound nbt);

  void readTargetTags(NbtCompound nbt);

  Sync getTargetTagsSync();

  interface Sync {
    int hash();

    Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> getData();

    void syncFrom(Sync source);
  }
}
