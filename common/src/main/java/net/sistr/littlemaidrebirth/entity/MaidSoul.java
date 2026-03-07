package net.sistr.littlemaidrebirth.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;

// todo このクラス置く場所ここで正しい？
public class MaidSoul {
  private final NbtCompound nbt;
  private final UUID uuid;
  private final String name;

  public MaidSoul(LittleMaidEntity maid) {
    this.nbt = new NbtCompound();
    maid.writeNbt(this.nbt);
    this.nbt.putString("Name", maid.getName().getString());
    this.name = maid.getName().getString();
    this.uuid = maid.getUuid();
  }

  private MaidSoul(NbtCompound nbt, UUID uuid, String name) {
    this.nbt = nbt;
    this.uuid = uuid;
    this.name = name;
  }

  public static MaidSoul fromNbt(NbtCompound nbt) {
    return new MaidSoul(nbt, nbt.getUuid("UUID"), nbt.getString("Name"));
  }

  public NbtCompound getNbt() {
    return nbt;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public Optional<UUID> getOwnerUUID() {
    return Optional.ofNullable(nbt.getUuid("Owner"));
  }

  public String getName() {
    return this.name;
  }
}
