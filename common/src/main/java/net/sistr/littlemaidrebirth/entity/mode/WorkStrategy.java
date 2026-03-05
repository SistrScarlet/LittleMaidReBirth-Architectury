package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WorkStrategy<T extends BlockEntity> {

  Optional<T> getBlockEntity(World world, BlockPos pos);

  boolean hasRequiredItems(Inventory inventory);

  boolean isUsableTarget(T blockEntity, Inventory inventory, World world);

  boolean hasRemainingWork(T blockEntity);

  boolean shouldContinueWork(T blockEntity, Inventory inventory, World world);

  void doWork(T blockEntity, Inventory inventory, World world, WorkActions actions);

  void extractAll(T blockEntity, Inventory inventory, WorkActions actions);

  String blockPosNbtKey();
}
