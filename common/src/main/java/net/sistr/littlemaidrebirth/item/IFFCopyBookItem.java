package net.sistr.littlemaidrebirth.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.sistr.littlemaidrebirth.setup.ModSetup;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * IFFをコピーする本。不使用
 * */
public class IFFCopyBookItem extends Item {

    public IFFCopyBookItem() {
        super(new Settings()
                .arch$tab(ModSetup.ITEM_GROUP)
                .maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        NbtCompound tag = stack.getNbt();
        if (tag != null && tag.contains("IFFs")) {
            tooltip.add(Text.translatable("item.littlemaidrebirth.iff_copy_book.tooltip"));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return super.use(world, user, hand);
        }
        ItemStack stack = user.getStackInHand(hand);
        Vec3d start = user.getCameraPosVec(1F);
        Vec3d end = start.add(user.getRotationVector().multiply(4D));
        BlockHitResult bResult = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
        if (bResult.getType() != HitResult.Type.MISS) {
            end = bResult.getPos();
        }
        Box box = new Box(start, end).expand(1);
        EntityHitResult eResult = ProjectileUtil.getEntityCollision(world, user, start, end, box,
                entity -> entity instanceof HasIFF);
        if (eResult == null || eResult.getType() == HitResult.Type.MISS)
            return super.use(world, user, hand);

        Entity target = eResult.getEntity();
        if (user.isSneaking()) {
            NbtList list = new NbtList();
            ((HasIFF) target).getIFFs().forEach(iff -> list.add(iff.writeTag()));
            NbtCompound tag = stack.getOrCreateNbt();
            tag.put("IFFs", list);
            user.sendMessage(Text.translatable("item.littlemaidrebirth.iff_copy_book.message_written"), true);
        } else {
            NbtCompound tag = stack.getOrCreateNbt();
            if (!tag.contains("IFFs")) {
                return super.use(world, user, hand);
            }
            NbtList list = tag.getList("IFFs", 10);
            ((HasIFF) target).setIFFs(list.stream()
                    .map(t -> (NbtCompound) t)
                    .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
            user.sendMessage(Text.translatable("item.littlemaidrebirth.iff_copy_book.message_apply"), true);
        }
        user.world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1F, 1F);
        return TypedActionResult.success(stack);
    }

    //こちらだとタグがイマイチうまく保存できない
    /*@Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof HasIFF)) {
            return super.useOnEntity(stack, user, entity, hand);
        }
        if (user.world.isClient) {
            return ActionResult.success(true);
        }
        if (user.isSneaking()) {
            NbtList list = new NbtList();
            ((HasIFF) entity).getIFFs().forEach(iff -> list.add(iff.writeTag()));
            NbtCompound tag = stack.getOrCreateTag();
            tag.put("IFFs", list);
        } else {
            NbtCompound tag = stack.getOrCreateTag();
            if (!tag.contains("IFFs")) {
                return super.useOnEntity(stack, user, entity, hand);
            }
            NbtList list = tag.getList("IFFs", 10);
            ((HasIFF) entity).setIFFs(list.stream()
                    .map(t -> (NbtCompound) t)
                    .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
        }
        return ActionResult.success(false);
    }*/


}
