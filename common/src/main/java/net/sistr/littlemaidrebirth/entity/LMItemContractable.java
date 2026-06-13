package net.sistr.littlemaidrebirth.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.SalaryBoxPosListener;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class LMItemContractable<T extends LittleMaidEntity>
        extends ItemContractable<LittleMaidEntity> implements SalaryBoxPosListener {
    protected final ObjectArraySet<BlockPos> salaryBoxPosSet =
            new ObjectArraySet<>(getMaxMemorySalaryBoxPos());

    public LMItemContractable(
            T mob,
            Supplier<Integer> maxConsumeInterval,
            Supplier<Integer> maxUnpaidTimes,
            Predicate<ItemStack> salaryItems) {
        super(mob, maxConsumeInterval, maxUnpaidTimes, salaryItems);
    }

    @Override
    protected void postReceive() {
        super.postReceive();
        var maid = this.mob;
        maid.swingHand(Hand.MAIN_HAND);
        maid.playSound(
                SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, maid.getRandom().nextFloat() * 0.1F + 1.0F);
        maid.play(LMSounds.EAT_SUGAR);
    }

    @Override
    protected void onStrike() {
        super.onStrike();

        mob.setStrike(true);
        TameableUtil.setWait(mob, false);
        if (mob.getMovingMode() != MovingMode.FREEDOM) {
            mob.setMovingMode(MovingMode.FREEDOM);
            mob.setFreedomPos(mob.getBlockPos());
        }
    }

    @Override
    public void listenSalaryBoxPos(BlockPos pos) {
        if (!TameableUtil.hasTameOwner(this.mob) || salaryBoxPosSet.contains(pos)) {
            return;
        }
        if (salaryBoxPosSet.size() < getMaxMemorySalaryBoxPos()) {
            salaryBoxPosSet.add(pos);
        } else {
            checkAndFixSalaryBoxPosSize();
            var farthestPos = salaryBoxPosSet.stream().max(this::compareDistance).orElseThrow();
            if (pos.getSquaredDistance(mob.getPos())
                    < farthestPos.getSquaredDistance(mob.getPos())) {
                salaryBoxPosSet.remove(farthestPos);
                salaryBoxPosSet.add(pos);
                if (this.mob.getEntityWorld() instanceof ServerWorld serverWorld) {
                    var particlePos = pos.toCenterPos();

                    serverWorld.spawnParticles(
                            ParticleTypes.FIREWORK,
                            particlePos.x,
                            particlePos.y,
                            particlePos.z,
                            10,
                            0,
                            0,
                            0,
                            0.2);
                }
            }
        }
    }

    public List<BlockPos> getSalaryBoxPositions() {
        return new ObjectArrayList<>(salaryBoxPosSet);
    }

    public boolean hasSalaryBoxPositions() {
        return !salaryBoxPosSet.isEmpty();
    }

    public void setSalaryBoxPositions(Collection<BlockPos> salaryBoxPosSet) {
        this.salaryBoxPosSet.clear();
        this.salaryBoxPosSet.addAll(salaryBoxPosSet);
        checkAndFixSalaryBoxPosSize();
    }

    @Override
    public void readContractable(NbtCompound nbt) {
        super.readContractable(nbt);

        salaryBoxPosSet.clear();
        if (nbt.contains("salaryBoxPosList")) {
            var boxPosList = nbt.getList("salaryBoxPosList", NbtElement.COMPOUND_TYPE);
            boxPosList.forEach(
                    posTag -> {
                        if (posTag instanceof NbtCompound posTagCompound) {
                            NbtHelper.toBlockPos(posTagCompound, "pos")
                                    .ifPresent(salaryBoxPosSet::add);
                        }
                    });
            checkAndFixSalaryBoxPosSize();
        }
    }

    @Override
    public void writeContractable(NbtCompound nbt) {
        super.writeContractable(nbt);

        if (!salaryBoxPosSet.isEmpty()) {
            checkAndFixSalaryBoxPosSize();
            NbtList salaryBoxPosList = new NbtList();
            for (BlockPos pos : salaryBoxPosSet) {
                var posTag = new NbtCompound();
                posTag.put("pos", NbtHelper.fromBlockPos(pos));
                salaryBoxPosList.add(posTag);
            }
            nbt.put("salaryBoxPosList", salaryBoxPosList);
        }
    }

    protected int getMaxMemorySalaryBoxPos() {
        return LMRBMod.getConfig().contract.maxMemorySalaryBoxPos;
    }

    protected void checkAndFixSalaryBoxPosSize() {
        if (salaryBoxPosSet.size() > getMaxMemorySalaryBoxPos()) {
            var tmp =
                    salaryBoxPosSet.stream()
                            .sorted(this::compareDistance)
                            .limit(getMaxMemorySalaryBoxPos())
                            .toList();
            salaryBoxPosSet.clear();
            salaryBoxPosSet.addAll(tmp);
        }
    }

    protected int compareDistance(BlockPos pos1, BlockPos pos2) {
        return Double.compare(
                pos1.getSquaredDistance(mob.getPos()), pos2.getSquaredDistance(mob.getPos()));
    }
}
