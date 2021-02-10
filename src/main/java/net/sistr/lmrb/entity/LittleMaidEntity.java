package net.sistr.lmrb.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
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
import net.sistr.lmrb.entity.iff.*;
import net.sistr.lmrb.entity.mode.*;
import net.sistr.lmrb.item.IFFCopyBookItem;
import net.sistr.lmrb.tags.LMTags;
import net.sistr.lmrb.util.LivingAccessor;

import java.util.*;
import java.util.stream.Collectors;

import static net.sistr.lmrb.entity.Tameable.MovingState.*;

//メイドさん本体
//todo このクラスの行数を500まで減らす、処理の整理、攻撃できない地点の敵に攻撃しない、遠すぎる場合は水中だろうとTP、フリー状態で延々遠出、ランダムテクスチャ
public class LittleMaidEntity extends TameableEntity implements CustomPacketEntity, InventorySupplier, Tameable,
        NeedSalary, ModeSupplier, HasIFF, AimingPoseable, FakePlayerSupplier, IHasMultiModel, SoundPlayable {
    //変数群。カオス
    private static final TrackedData<Byte> MOVING_STATE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> MODE_NAME =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> AIMING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEGGING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final LMFakePlayerSupplier fakePlayer = new LMFakePlayerSupplier(this);
    private final LMInventorySupplier littleMaidInventory = new LMInventorySupplier(this, this);
    //todo お給料機能とテイム機能一緒にした方がよさげ
    private final TickTimeBaseNeedSalary needSalary =
            new TickTimeBaseNeedSalary(this, this, 7, LMTags.Items.MAIDS_SALARY.values());
    private final ModeController modeController = new ModeController(this, this, new HashSet<>());
    private final MultiModelCompound multiModel;
    private final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final HasIFF iff;
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private BlockPos freedomPos;
    private LivingEntity prevTarget;
    @Environment(EnvType.CLIENT)
    private float interestedAngle;
    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;

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
        iff = new IFFImpl(IFFTypeManager.getINSTANCE().getIFFTypes(world).stream()
                .map(IFFType::createIFF).collect(Collectors.toList()));
    }

    //スタティックなメソッド

    public static DefaultAttributeContainer.Builder createLittleMaidAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED)
                .add(EntityAttributes.GENERIC_LUCK)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16D)
                .add(ReachEntityAttributes.REACH)
                .add(ReachEntityAttributes.ATTACK_RANGE);
    }

    public static boolean isValidNaturalSpawn(WorldAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                && world.getBaseLightLevel(pos, 0) > 8;
    }

    //登録メソッドたち

    public void addDefaultModes(LittleMaidEntity maid) {
        maid.addMode(new FencerMode(maid, maid, 1D, true));
        maid.addMode(new ArcherMode<>(maid, 15F,
                entity -> entity instanceof LivingEntity && isFriend((LivingEntity) entity)));
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
                Sets.newHashSet(LMTags.Items.MAIDS_SALARY.values()), 2, 1));
        this.goalSelector.add(10, new WaitGoal(this, this));
        //todo 挙動が怪しい
        /*this.goalSelector.add(12, new WaitWhenOpenGUIGoal<>(this, this,
                LittleMaidScreenHandler.class));*/
        this.goalSelector.add(13, new EscortGoal(this, this,
                16F, 20F, 24F, 1.5D));
        this.goalSelector.add(15, new ModeWrapperGoal(this));
        this.goalSelector.add(16, new FollowAtHeldItemGoal(this, this, true,
                Sets.newHashSet(LMTags.Items.MAIDS_SALARY.values())));
        this.goalSelector.add(17, new LMStareAtHeldItemGoal(this, this, false
                , Sets.newHashSet(LMTags.Items.MAIDS_EMPLOYABLE.values())));
        this.goalSelector.add(17, new LMStareAtHeldItemGoal(this, this, true,
                Sets.newHashSet(LMTags.Items.MAIDS_SALARY.values())));
        this.goalSelector.add(18, new LMMoveToDropItemGoal(this, 8, 1D));
        this.goalSelector.add(19, new EscortGoal(this, this,
                6F, 8F, 12F, 1.5D));
        this.goalSelector.add(20, new EscortGoal(this, this,
                4F, 6F, 12F, 1.0D));
        this.goalSelector.add(20, new FreedomGoal(this, this, 0.8D, 16D));
        this.goalSelector.add(30, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(30, new LookAroundGoal(this));

        this.targetSelector.add(3, new PredicateRevengeGoal(this, this::isFriend));
        this.targetSelector.add(4, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(5, new AttackWithOwnerGoal(this));
        this.targetSelector.add(6, new FollowTargetGoal<>(
                this, LivingEntity.class, 5, true, false,
                entity -> identify(entity) == IFFTag.ENEMY));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MOVING_STATE, (byte) 2);
        this.dataTracker.startTracking(MODE_NAME, "");
        this.dataTracker.startTracking(AIMING, false);
        this.dataTracker.startTracking(BEGGING, false);
    }

    //読み書き系

    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);

        writeInventory(tag);

        tag.putInt("MovingState", getMovingState().getId());

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

        writeIFF(tag);

    }

    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        readInventory(tag);

        setMovingState(MovingState.fromId(tag.getInt("MovingState")));

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

        readIFF(tag);
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

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return super.canImmediatelyDespawn(distanceSquared);
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
        dimensions = EntityDimensions.changing(width, height);
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
        //todo ここ拡張可能にする
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
    public boolean damage(DamageSource source, float amount) {
        if (!world.isClient) {
            //味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getSource() instanceof SnowballEntity) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        Entity attacker = source.getAttacker();
        //Friendからの攻撃を除外
        if (attacker instanceof LivingEntity && isFriend((LivingEntity) attacker)) {
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

    //todo 以下数メソッドにはもうちと整理が必要か

    //trueでアイテムが使用された、falseでされなかった
    //trueならItemStack.interactWithEntity()が起こらず、またアイテム使用が必ずキャンセルされる
    //継承元のコードは無視
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking() || stack.getItem() instanceof IFFCopyBookItem) {
            return ActionResult.PASS;
        }
        if (!hasTameOwner()) {
            if (LMTags.Items.MAIDS_EMPLOYABLE.contains(stack.getItem())) {
                return contract(player, stack, false);
            }
            return ActionResult.PASS;
        }
        if (!player.getUuid().equals(this.getOwnerUuid())) {
            return ActionResult.PASS;
        }
        if (isStrike()) {
            if (LMTags.Items.MAIDS_EMPLOYABLE.contains(stack.getItem())) {
                return contract(player, stack, true);
            } else if (world instanceof ServerWorld) {
                ((ServerWorld) world).spawnParticles(ParticleTypes.SMOKE,
                        this.getX() + (0.5F - random.nextFloat()) * 0.2F,
                        this.getEyeY() + (0.5F - random.nextFloat()) * 0.2F,
                        this.getZ() + (0.5F - random.nextFloat()) * 0.2F,
                        5,
                        0, 1, 0, 0.1);
            }
            return ActionResult.PASS;
        }
        if (LMTags.Items.MAIDS_SALARY.contains(stack.getItem())) {
            return changeState(player, stack);
        }
        if (!player.world.isClient) {
            openContainer(player);
        }
        return ActionResult.success(world.isClient);
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
        MovingState state = this.getMovingState();
        if (state == WAIT) {
            setMovingState(ESCORT);
        } else {
            setMovingState(WAIT);
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
        while (receiveSalary(1)) ;//ここに給料処理が混じってるのがちょっとムカつく
        getNavigation().stop();
        this.setOwnerUuid(player.getUuid());
        setMovingState(ESCORT);
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
    public void writeInventory(CompoundTag tag) {
        this.littleMaidInventory.writeInventory(tag);
    }

    @Override
    public void readInventory(CompoundTag tag) {
        this.littleMaidInventory.readInventory(tag);
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

    //防具の更新およびオフハンドの位置ズラし
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            Inventory inv = getInventory();
            inv.setStack(1 + 18 + slot.getEntitySlotId(), stack);
            multiModel.updateArmor();
        } else if (slot == EquipmentSlot.MAINHAND) {
            getInventory().setStack(0, stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            getInventory().setStack(18 + 4 + 1, stack);
        }
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            return getInventory().getStack(1 + 18 + slot.getEntitySlotId());
        } else if (slot == EquipmentSlot.MAINHAND) {
            return getInventory().getStack(0);
        } else if (slot == EquipmentSlot.OFFHAND) {
            return getInventory().getStack(18 + 4 + 1);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Iterable<ItemStack> getItemsHand() {
        return () -> Lists.newArrayList(getMainHandStack(), getOffHandStack()).iterator();
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Lists.newArrayList(
                getEquippedStack(EquipmentSlot.FEET),
                getEquippedStack(EquipmentSlot.LEGS),
                getEquippedStack(EquipmentSlot.CHEST),
                getEquippedStack(EquipmentSlot.HEAD));
    }

    @Override
    protected void dropInventory() {
        //鯖側でしか動かないが一応チェック
        Inventory inv = this.getInventory();
        if (inv instanceof PlayerInventory)
            ((LMInventorySupplier.LMInventory) inv).dropAll();
    }

    @Override
    protected void damageArmor(DamageSource source, float amount) {
        super.damageArmor(source, amount);
        ((PlayerInventory) getInventory()).damageArmor(source, amount);
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
    public MovingState getMovingState() {
        int num = this.dataTracker.get(MOVING_STATE);
        return MovingState.fromId(num);
    }

    public void setMovingState(MovingState movingState) {
        int num = movingState.getId();
        this.dataTracker.set(MOVING_STATE, (byte) num);
    }

    @Override
    public void setFreedomPos(BlockPos freedomPos) {
        this.freedomPos = freedomPos;
    }

    @Override
    public BlockPos getFreedomPos() {
        if (freedomPos == null) freedomPos = getBlockPos();
        return freedomPos;
    }

    @Override
    public void setInSittingPose(boolean inSittingPose) {

    }

    @Override
    public boolean isInSittingPose() {
        return false;
    }

    @Override
    public void setSitting(boolean sitting) {
        setMovingState(sitting ? WAIT : ESCORT);
    }

    @Override
    public boolean isSitting() {
        return getMovingState() == WAIT;
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
            this.playSound(SoundEvents.ENTITY_ITEM_PICKUP,
                    1.0F, this.getRandom().nextFloat() * 0.1F + 1.0F);
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
    public IFFTag identify(LivingEntity target) {
        return iff.identify(target);
    }

    @Override
    public void setIFFs(List<IFF> iffs) {
        iff.setIFFs(iffs);
    }

    @Override
    public List<IFF> getIFFs() {
        return iff.getIFFs();
    }

    @Override
    public void writeIFF(CompoundTag tag) {
        iff.writeIFF(tag);
    }

    @Override
    public void readIFF(CompoundTag tag) {
        iff.readIFF(tag);
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return identify(target) != IFFTag.FRIEND;
    }

    public boolean isFriend(LivingEntity entity) {
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
        return identify(entity) == IFFTag.FRIEND;
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
