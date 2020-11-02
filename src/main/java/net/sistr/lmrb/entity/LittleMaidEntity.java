package net.sistr.lmrb.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.sistr.lmml.entity.compound.IHasMultiModel;
import net.sistr.lmml.entity.compound.MultiModelCompound;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.entity.compound.SoundPlayableCompound;
import net.sistr.lmml.maidmodel.IModelCaps;
import net.sistr.lmml.maidmodel.ModelMultiBase;
import net.sistr.lmml.network.CustomMobSpawnPacket;
import net.sistr.lmml.network.util.CustomPacketEntity;
import net.sistr.lmml.resource.holder.ConfigHolder;
import net.sistr.lmml.resource.holder.TextureHolder;
import net.sistr.lmml.resource.manager.LMConfigManager;
import net.sistr.lmml.resource.manager.LMModelManager;
import net.sistr.lmml.resource.manager.LMTextureManager;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.lmml.resource.util.TextureColors;
import net.sistr.lmrb.entity.goal.*;
import net.sistr.lmrb.entity.iff.HasIFF;
import net.sistr.lmrb.entity.mode.*;
import net.sistr.lmrb.setup.Registration;
import net.sistr.lmrb.util.LivingAccessor;

import java.util.*;

//メイドさん本体
//todo 啼くように、このクラスの行数を500まで減らす
public class LittleMaidEntity extends TameableEntity implements CustomPacketEntity, InventorySupplier, Tameable,
        NeedSalary, ModeSupplier, HasIFF, AimingPoseable, FakePlayerSupplier, IHasMultiModel, SoundPlayable {
    //変数群。カオス
    private static final TrackedData<Byte> MOVING_STATE = DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> MODE_NAME = DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> AIMING = DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEGGING = DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final LMFakePlayerSupplier fakePlayer = new LMFakePlayerSupplier(this);
    private final LMInventorySupplier littleMaidInventory = new LMInventorySupplier(this, this);
    //todo お給料機能とテイム機能一緒にした方がよさげ
    private final TickTimeBaseNeedSalary needSalary =
            new TickTimeBaseNeedSalary(this, this, 7, Lists.newArrayList(Items.SUGAR));
    private final ModeController modeController = new ModeController(this, this, new HashSet<>());
    private final MultiModelCompound multiModel;
    private final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private BlockPos freedomPos;
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    @Environment(EnvType.CLIENT)
    private float interestedAngle;
    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;
    private LivingEntity prevTarget;

    //コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new FixedMoveControl(this);
        ((MobNavigation) getNavigation()).setCanPathThroughDoors(true);
        multiModel = new MultiModelCompound(this,
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
        soundPlayer = new SoundPlayableCompound(this, () ->
                multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
    }

    public LittleMaidEntity(World world) {
        super(Registration.LITTLE_MAID_MOB, world);
        this.moveControl = new FixedMoveControl(this);
        ((MobNavigation) getNavigation()).setCanPathThroughDoors(true);
        multiModel = new MultiModelCompound(this,
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
        soundPlayer = new SoundPlayableCompound(this, () ->
                multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
    }

    public LittleMaidEntity(World world, MultiModelCompound multiModel, SoundPlayableCompound soundPlayer) {
        super(Registration.LITTLE_MAID_MOB, world);
        this.moveControl = new FixedMoveControl(this);
        ((MobNavigation) getNavigation()).setCanPathThroughDoors(true);
        this.multiModel = multiModel;
        this.soundPlayer = soundPlayer;
        addDefaultModes(this);
    }

    //スタティックなメソッド

    public static DefaultAttributeContainer.Builder createLittleMaidAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED)
                .add(EntityAttributes.GENERIC_LUCK);
    }

    public static boolean isValidNaturalSpawn(EntityType<? extends AnimalEntity> type, WorldAccess world,
                                              SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                && world.getBaseLightLevel(pos, 0) > 8;
    }

    //登録メソッドたち

    public void addDefaultModes(LittleMaidEntity maid) {
        maid.addMode(new FencerMode(maid, maid, 1D, true));
        maid.addMode(new ArcherMode(maid, maid, maid,
                0.1F, 10, 24));
        maid.addMode(new CookingMode(maid, maid));
        maid.addMode(new RipperMode(maid, 8));
        maid.addMode(new TorcherMode(maid, maid, maid, 8));
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(5, new HealMyselfGoal(this, this,
                Sets.newHashSet(Items.SUGAR), 2, 1));
        this.goalSelector.add(10, new WaitGoal(this, this));
        //todo 挙動が怪しい
        /*this.goalSelector.add(12, new WaitWhenOpenGUIGoal<>(this, this,
                LittleMaidScreenHandler.class));*/
        this.goalSelector.add(13, new EscortGoal(this, this,
                16F, 20F, 24F, 1.5D));
        this.goalSelector.add(15, new ModeWrapperGoal(this));
        this.goalSelector.add(16, new FollowAtHeldItemGoal(this, this, true,
                Sets.newHashSet(Items.SUGAR)));
        this.goalSelector.add(17, new LMStareAtHeldItemGoal(this, this, false
                , Sets.newHashSet(Items.CAKE)));
        this.goalSelector.add(17, new LMStareAtHeldItemGoal(this, this, true,
                Sets.newHashSet(Items.SUGAR)));
        this.goalSelector.add(18, new LMMoveToDropItemGoal(this, 8, 1D));
        this.goalSelector.add(19, new EscortGoal(this, this,
                6F, 8F, 12F, 1.5D));
        this.goalSelector.add(20, new EscortGoal(this, this,
                4F, 6F, 12F, 1.0D));
        this.goalSelector.add(20, new FreedomGoal(this, this, 0.8D, 16D));
        this.goalSelector.add(30, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(30, new LookAroundGoal(this));

        this.targetSelector.add(3, new RevengeGoal(this));
        this.targetSelector.add(4, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(5, new AttackWithOwnerGoal(this));
        this.targetSelector.add(6, new FollowTargetGoal<>(this, MobEntity.class,
                5, true, false, this::isEnemy));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MOVING_STATE, (byte) 0);
        this.dataTracker.startTracking(MODE_NAME, "");
        this.dataTracker.startTracking(AIMING, false);
        this.dataTracker.startTracking(BEGGING, false);
    }

    //読み書き系

    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);

        littleMaidInventory.writeInventory(tag);

        tag.putString("MovingState", getMovingState());

        if (freedomPos != null)
            tag.put("FreedomPos", NbtHelper.fromBlockPos(freedomPos));

        needSalary.writeSalary(tag);

        writeModeData(tag);

        tag.putByte("SkinColor", (byte) getColor().getIndex());
        tag.putBoolean("IsContract", isContract());
        tag.putString("SkinTexture", getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            tag.putString("ArmorTextureInner" + part.getPartName(),
                    getTextureHolder(Layer.INNER, part).getTextureName());
            tag.putString("ArmorTextureOuter" + part.getPartName(),
                    getTextureHolder(Layer.OUTER, part).getTextureName());
        }

        tag.putString("SoundConfigName", getConfigHolder().getName());

    }

    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        littleMaidInventory.readInventory(tag);

        if (tag.contains("MovingState"))
            setMovingState(tag.getString("MovingState"));

        if (tag.contains("FreedomPos")) {
            freedomPos = NbtHelper.toBlockPos(tag.getCompound("FreedomPos"));
        }

        needSalary.readSalary(tag);

        readModeData(tag);

        if (tag.contains("SkinColor")) {
            setColor(TextureColors.getColor(tag.getByte("SkinColor")));
        }
        setContract(tag.getBoolean("IsContract"));
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        if (tag.contains("SkinTexture")) {
            textureManager.getTexture(tag.getString("SkinTexture"))
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        }
        for (Part part : Part.values()) {
            String inner = "ArmorTextureInner" + part.getPartName();
            String outer = "ArmorTextureOuter" + part.getPartName();
            if (tag.contains(inner)) {
                textureManager.getTexture(tag.getString(inner))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            }
            if (tag.contains(outer)) {
                textureManager.getTexture(tag.getString(outer))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
            }
        }

        if (tag.contains("SoundConfigName")) {
            LMConfigManager.INSTANCE.getConfig(tag.getString("SoundConfigName"))
                    .ifPresent(this::setConfigHolder);
        }
    }

    //鯖
    @Override
    public void writeCustomPacket(PacketByteBuf buf) {
        //モデル
        buf.writeEnumConstant(getColor());
        buf.writeBoolean(isContract());
        buf.writeString(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeString(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeString(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        //サウンド
        buf.writeString(getConfigHolder().getName());
    }

    //蔵
    @Override
    public void readCustomPacket(PacketByteBuf buf) {
        //モデル
        //readString()はクラ処理。このメソッドでは、クラ側なので問題なし
        setColor(buf.readEnumConstant(TextureColors.class));
        setContract(buf.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(buf.readString())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager.getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager.getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
        //サウンド
        LMConfigManager.INSTANCE.getConfig(buf.readString())
                .ifPresent(this::setConfigHolder);
    }

    //バニラメソッズ

    @Override
    public void tick() {
        super.tick();
        fakePlayer.tick();
        tickHandSwing();
        if (hasTameOwner()) needSalary.tick();
        if (world.isClient) {
            tickInterestedAngle();
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        modeController.tick();
        LivingEntity target = getTarget();
        if (target != null && target != prevTarget) {
            play(LMSounds.FIND_TARGET_N);
        }
        prevTarget = target;
    }

    //canSpawnとかでも使われる
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos.down()).isFullCube(world, pos) ? 10.0F : world.getBrightness(pos) - 0.5F;
    }

    @Override
    protected float getDropChance(EquipmentSlot slot) {
        return 0;
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target) && !isFriend(target);
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    /**
     * 上に乗ってるエンティティへのオフセット
     */
    @Override
    public double getMountedHeightOffset() {
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /**
     * 騎乗時のオフセット
     */
    @Override
    public double getHeightOffset() {
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getyOffset(getCaps()) - getHeight();
    }

    //このままだとEntityDimensionsが作っては捨てられてを繰り返すのでパフォーマンスはよろしくない
    //…が、そもそもそんなにたくさん呼ばれるメソッドでもない
    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions dimensions;
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps());
        float width = model.getWidth(getCaps());
        dimensions = EntityDimensions.fixed(width, height);
        return dimensions.scaled(getScaleFactor());
    }

    @Override
    public void playAmbientSound() {
        if (world.isClient) {
            return;
        }
        if (0.2F < random.nextFloat()) {
            return;
        }
        if (getHealth() / getMaxHealth() < 0.3F) {
            play(LMSounds.LIVING_WHINE);
        } else if (this.getMainHandStack().getItem() == Items.CLOCK) {
            long time = world.getTimeOfDay();
            if (0 <= time && time < 1000) {
                if (time % 2 == 0)
                    play(LMSounds.GOOD_MORNING);
                else
                    play(LMSounds.LIVING_MORNING);
            } else if (12542 <= time && time < 13500) {
                if (time % 2 == 0)
                    play(LMSounds.GOOD_NIGHT);
                else
                    play(LMSounds.LIVING_NIGHT);
            }
        } else if (world.isRaining()) {
            Biome biome = this.world.getBiome(getBlockPos());
            if (biome.getPrecipitation() == Biome.Precipitation.RAIN)
                play(LMSounds.LIVING_RAIN);
            else if (biome.getPrecipitation() == Biome.Precipitation.SNOW)
                play(LMSounds.LIVING_SNOW);
        } else {
            Biome biome = this.world.getBiome(getBlockPos());
            float temperature = biome.getTemperature(getBlockPos());
            if (temperature < 0.1F) {
                play(LMSounds.LIVING_COLD);
            } else if (1 < temperature) {
                play(LMSounds.LIVING_HOT);
            } else {
                play(LMSounds.LIVING_DAYTIME);
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        play(LMSounds.DEATH);
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack stack = getEquippedStack(slot);
                this.dropStack(stack);
                this.equipStack(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        this.dropStack(getEquippedStack(EquipmentSlot.OFFHAND));
        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (!world.isClient)
            ((PlayerInventory) this.getInventory()).dropAll();
    }

    //防具の更新およびオフハンドの位置ズラし
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            multiModel.updateArmor();
        } else if (!world.isClient && slot == EquipmentSlot.OFFHAND) {
            ((PlayerInventory) getInventory()).offHand.set(0, stack);
            return;
        }
        super.equipStack(slot, stack);
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (!world.isClient && slot == EquipmentSlot.OFFHAND) {
            return ((PlayerInventory) getInventory()).offHand.get(0);
        }
        return super.getEquippedStack(slot);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!world.isClient) {
            //味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getSource() instanceof SnowballEntity) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        Entity attacker = source.getAttacker();
        //Friend及び、自身と同じUUIDの者(自身のFakePlayer)を除外
        if (attacker != null && (isFriend(attacker) || this.getUuid().equals(attacker.getUuid()))) {
            return false;
        }
        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.damage(source, amount);
        if (!world.isClient && !isHurtTime) {
            if (!result || amount <= 0F) {
                play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && ((LivingAccessor) this).blockedByShield_LM(source)) {
                play(LMSounds.HURT_GUARD);
            } else if (source == DamageSource.FALL) {
                play(LMSounds.HURT_FALL);
            } else if (source.isFire()) {
                play(LMSounds.HURT_FIRE);
            } else {
                play(LMSounds.HURT);
            }
        }
        return result;
    }

    @Override
    protected void damageArmor(DamageSource source, float amount) {
        super.damageArmor(source, amount);
        ((PlayerInventory) getInventory()).damageArmor(source, amount);
    }

    public boolean isFriend(Entity entity) {
        UUID ownerId = this.getOwnerUuid();
        if (ownerId != null) {
            //主はフレンド
            if (ownerId.equals(entity.getUuid())) {
                return true;
            }
            //同じ主を持つ者はフレンド
            if (entity instanceof Tameable && ownerId.equals(((Tameable) entity).getTameOwnerUuid().orElse(null))
                    || entity instanceof TameableEntity && ownerId.equals(((TameableEntity) entity).getOwnerUuid())) {
                return true;
            }
        }
        return false;
    }

    //todo 以下数メソッドにはもうちと整理が必要か

    //trueでアイテムが使用された、falseでされなかった
    //trueならItemStack.interactWithEntity()が起こらず、またアイテム使用が必ずキャンセルされる
    //継承元のコードは無視
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.shouldCancelInteraction()) {
            return ActionResult.PASS;
        }
        //ストライキ中、ケーキじゃないなら不満気にしてリターン
        //クライアント側にはストライキかどうかは判定できない
        if (isStrike() && stack.getItem() != Items.CAKE) {
            if (world instanceof ServerWorld)
                ((ServerWorld) world).spawnParticles(ParticleTypes.SMOKE,
                        this.getX() + (0.5F - random.nextFloat()) * 0.2F,
                        this.getEyeY() + (0.5F - random.nextFloat()) * 0.2F,
                        this.getZ() + (0.5F - random.nextFloat()) * 0.2F,
                        5,
                        0, 1, 0, 0.1);
            return ActionResult.PASS;
        }
        if (hasTameOwner()) {
            if (isStrike()) {
                if (stack.getItem() == Items.CAKE) {
                    return contract(player, stack, true);
                }
                return ActionResult.PASS;
            }
            if (stack.getItem() == Items.SUGAR) {
                return changeState(player, stack);
            }
        } else {
            if (stack.getItem() == Items.CAKE) {
                return contract(player, stack, false);
            }
        }
        if (player.getUuid().equals(this.getOwnerUuid())) {
            if (!player.world.isClient)
                openContainer(player);
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    public ActionResult changeState(PlayerEntity player, ItemStack stack) {
        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.random.nextFloat() * 0.1F + 1.0F);
        this.world.addParticle(ParticleTypes.NOTE, this
                        .getX(), this.getY() + this.getStandingEyeHeight(), this.getZ(),
                0, this.random.nextGaussian() * 0.02D, 0);
        this.getNavigation().stop();
        changeMovingState();
        if (!player.abilities.creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.inventory.removeOne(stack);
            }
        }
        return ActionResult.success(world.isClient);
    }

    public void changeMovingState() {
        String state = this.getMovingState();
        switch (state) {
            case Tameable.WAIT:
                setMovingState(Tameable.ESCORT);
                break;
            case Tameable.ESCORT:
                setMovingState(Tameable.FREEDOM);
                this.freedomPos = getBlockPos();
                break;
            case Tameable.FREEDOM:
                setMovingState(Tameable.WAIT);
                break;
            default:
                setMovingState(Tameable.WAIT);
                break;
        }
    }

    public ActionResult contract(PlayerEntity player, ItemStack stack, boolean isReContract) {
        this.world.addParticle(ParticleTypes.HEART,
                getX(), getY() + getStandingEyeHeight(), getZ(),
                0, this.random.nextGaussian() * 0.02D, 0);
        if (!world.isClient) {
            if (isReContract) {
                play(LMSounds.RECONTRACT);
            } else {
                play(LMSounds.GET_CAKE);
            }
        }
        if (isReContract) {
            setStrike(false);
        }
        while (receiveSalary(1));//ここに給料処理が混じってるのがちょっとムカつく
        getNavigation().stop();
        this.setOwnerUuid(player.getUuid());
        setMovingState(Tameable.ESCORT);
        setContract(true);
        if (!player.abilities.creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.inventory.removeOne(stack);
            }
        }
        return ActionResult.success(world.isClient);
    }

    //GUI開くやつ
    public void openContainer(PlayerEntity player) {
        setAttacker(null);
        getNavigation().stop();
        setModeName(getMode().map(Mode::getName).orElse(""));
        player.openHandledScreen(screenFactory);
    }

    //インベントリ関連

    @Override
    public Inventory getInventory() {
        return this.littleMaidInventory.getInventory();
    }

    @Override
    public boolean equip(int slot, ItemStack item) {
        Inventory inventory = this.getInventory();
        if (0 <= slot && slot < inventory.size()) {
            inventory.setStack(slot, item);
            return true;
        } else {
            return super.equip(slot, item);
        }
    }

    //テイム関連

    @Override
    public Optional<LivingEntity> getTameOwner() {
        return Optional.ofNullable(getOwner());
    }

    @Override
    public void setTameOwnerUuid(UUID id) {
        setOwnerUuid(id);
    }

    @Override
    public Optional<UUID> getTameOwnerUuid() {
        return Optional.ofNullable(getOwnerUuid());
    }

    @Override
    public boolean hasTameOwner() {
        return getTameOwnerUuid().isPresent();
    }

    @Override
    public String getMovingState() {
        int num = this.dataTracker.get(MOVING_STATE);
        if (num <= 0) {
            return FREEDOM;
        } else if (num == 1) {
            return ESCORT;
        }
        return WAIT;
    }

    public void setMovingState(String movingState) {
        int num;
        switch (movingState) {
            case ESCORT:
                num = 1;
                break;
            case WAIT:
                num = 2;
                break;
            default:
                num = 0;
                break;
        }
        this.dataTracker.set(MOVING_STATE, (byte) num);
    }

    @Override
    public Optional<BlockPos> getFollowPos() {
        String state = getMovingState();
        switch (state) {
            case WAIT:
                return Optional.of(this.getBlockPos());
            case ESCORT:
                return getTameOwner().map(Entity::getBlockPos);
            case FREEDOM:
                return Optional.of(freedomPos == null ? getBlockPos() : freedomPos);
        }
        return Optional.empty();
    }

    @Override
    public boolean isTamed() {
        return hasTameOwner();
    }

    public boolean isBegging() {
        return this.dataTracker.get(BEGGING);
    }

    public void setBegging(boolean begging) {
        this.dataTracker.set(BEGGING, begging);
    }

    @Environment(EnvType.CLIENT)
    public float getInterestedAngle(float tickDelta) {
        return (prevInterestedAngle + (interestedAngle - prevInterestedAngle) * tickDelta) *
                ((getEntityId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI);
    }

    @Environment(EnvType.CLIENT)
    private void tickInterestedAngle() {
        prevInterestedAngle = interestedAngle;
        if (isBegging()) {
            interestedAngle = interestedAngle + (1.0F - interestedAngle) * 0.4F;
        } else {
            interestedAngle = interestedAngle + (0.0F - interestedAngle) * 0.4F;
        }
    }

    //お給料

    @Override
    public boolean receiveSalary(int num) {
        return needSalary.receiveSalary(num);
    }

    @Override
    public boolean consumeSalary(int num) {
        boolean result = needSalary.consumeSalary(num);
        if (result) {
            this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.getRandom().nextFloat() * 0.1F + 1.0F);
            this.swingHand(Hand.MAIN_HAND);
        }
        return result;
    }

    @Override
    public int getSalary() {
        return needSalary.getSalary();
    }

    @Override
    public boolean isSalary(ItemStack stack) {
        return needSalary.isSalary(stack);
    }

    @Override
    public boolean isStrike() {
        return needSalary.isStrike();
    }

    @Override
    public void setStrike(boolean strike) {
        needSalary.setStrike(strike);
    }

    //モード機能

    @Override
    public Optional<Mode> getMode() {
        return modeController.getMode();
    }

    @Override
    public void writeModeData(CompoundTag tag) {
        modeController.writeModeData(tag);
    }

    @Override
    public void readModeData(CompoundTag tag) {
        modeController.readModeData(tag);
    }

    public void addMode(Mode mode) {
        modeController.addMode(mode);
    }

    public void setModeName(String modeName) {
        this.dataTracker.set(MODE_NAME, modeName);
    }

    @Environment(EnvType.CLIENT)
    public Optional<String> getModeName() {
        String modeName = this.dataTracker.get(MODE_NAME);
        if (modeName.isEmpty()) return Optional.empty();
        return Optional.of(modeName);
    }

    //IFF

    @Override
    public boolean isEnemy(Entity entity) {
        return entity instanceof Monster
                && !(entity instanceof CreeperEntity)
                && !(entity instanceof EndermanEntity);
    }

    //エイム

    @Override
    public boolean isAimingBow() {
        return this.dataTracker.get(AIMING);
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.dataTracker.set(AIMING, aiming);
    }

    //Fake関連、クライアントで実行するとクラッシュする

    @Override
    public FakePlayer getFakePlayer() {
        return fakePlayer.getFakePlayer();
    }

    //マルチモデル関連

    @Override
    public boolean isAllowChangeTexture(Entity entity, TextureHolder textureHolder, Layer layer, Part part) {
        return multiModel.isAllowChangeTexture(entity, textureHolder, layer, part);
    }

    @Override
    public void setTextureHolder(TextureHolder textureHolder, Layer layer, Part part) {
        multiModel.setTextureHolder(textureHolder, layer, part);
        if (layer == Layer.SKIN) {
            calculateDimensions();
        }
    }

    @Override
    public TextureHolder getTextureHolder(Layer layer, Part part) {
        return multiModel.getTextureHolder(layer, part);
    }

    @Override
    public void setColor(TextureColors textureColor) {
        multiModel.setColor(textureColor);
    }

    @Override
    public TextureColors getColor() {
        return multiModel.getColor();
    }

    @Override
    public void setContract(boolean isContract) {
        multiModel.setContract(isContract);
    }

    /**
     * マルチモデルの使用テクスチャが契約時のものかどうか
     * ※実際に契約状態かどうかをチェックする場合、
     * {@link #hasTameOwner()}か、
     * {@link #getTameOwnerUuid()}の返り値が存在するかでチェックすること
     */
    @Override
    public boolean isContract() {
        return multiModel.isContract();
    }

    @Override
    public Optional<ModelMultiBase> getModel(Layer layer, Part part) {
        return multiModel.getModel(layer, part);
    }

    @Override
    public Optional<Identifier> getTexture(Layer layer, Part part, boolean isLight) {
        return multiModel.getTexture(layer, part, isLight);
    }

    @Override
    public IModelCaps getCaps() {
        return caps;
    }

    @Override
    public boolean isArmorVisible(Part part) {
        return multiModel.isArmorVisible(part);
    }

    @Override
    public boolean isArmorGlint(Part part) {
        return multiModel.isArmorGlint(part);
    }

    //音声関係

    @Override
    public void play(String soundName) {
        soundPlayer.play(soundName);
    }

    @Override
    public void setConfigHolder(ConfigHolder configHolder) {
        soundPlayer.setConfigHolder(configHolder);
    }

    @Override
    public ConfigHolder getConfigHolder() {
        return soundPlayer.getConfigHolder();
    }

    //オーバーライドしなくても動くが、CustomPacketEntityが機能しない
    @Override
    public Packet<?> createSpawnPacket() {
        return CustomMobSpawnPacket.createPacket(this);
    }

    public static class LMMoveToDropItemGoal extends MoveToDropItemGoal {
        private final LittleMaidEntity maid;

        public LMMoveToDropItemGoal(LittleMaidEntity maid, int range, double speed) {
            super(maid, range, speed);
            this.maid = maid;
        }

        @Override
        public boolean canStart() {
            return ((PlayerInventory) maid.getInventory()).getEmptySlot() != -1 && super.canStart();
        }
    }

    public static class LMStareAtHeldItemGoal extends TameableStareAtHeldItemGoal {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(LittleMaidEntity maid, Tameable tameable, boolean isTamed, Set<Item> items) {
            super(maid, tameable, isTamed, items);
            this.maid = maid;
        }

        @Override
        public void start() {
            super.start();
            maid.setBegging(true);
        }

        @Override
        public void stop() {
            super.stop();
            maid.setBegging(false);
        }
    }

}
