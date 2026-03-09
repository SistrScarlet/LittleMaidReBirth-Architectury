package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SaddleItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SaddleItem.class)
public class MixinSaddleItem extends Item {

    public MixinSaddleItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.getPassengerList().stream().anyMatch(e -> e instanceof LittleMaidEntity)) {
            user.removeAllPassengers();
        }
        return super.use(world, user, hand);
    }
}
