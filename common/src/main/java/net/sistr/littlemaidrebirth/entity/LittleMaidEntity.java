package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import dev.architectury.registry.menu.MenuRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.sistr.littlemaidmodelloader.client.resource.manager.LMSoundManager;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.MultiModelCompound;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayableCompound;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.multimodel.IMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMPose;
import net.sistr.littlemaidmodelloader.network.CustomMobSpawnPacket;
import net.sistr.littlemaidmodelloader.network.util.CustomPacketEntity;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.goal.*;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.mode.ModeController;
import net.sistr.littlemaidrebirth.entity.mode.ModeSupplier;
import net.sistr.littlemaidrebirth.entity.mode.ModeWrapperGoal;
import net.sistr.littlemaidrebirth.item.IFFCopyBookItem;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.sistr.littlemaidrebirth.entity.Tameable.MovingState.ESCORT;
import static net.sistr.littlemaidrebirth.entity.Tameable.MovingState.WAIT;

//メイドさん本体
public class LittleMaidEntity extends TameableEntity implements CustomPacketEntity, InventorySupplier, Tameable,
        Contractable, ModeSupplier, HasIFF, AimingPoseable, FakePlayerSupplier, IHasMultiModel, SoundPlayable {
    //変数群。カオス
    private static final TrackedData<Byte> MOVING_STATE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> MODE_NAME =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> AIMING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEGGING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BLOOD_SUCK =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final LMFakePlayerSupplier fakePlayer = new LMFakePlayerSupplier(this);
    private final LMInventorySupplier littleMaidInventory = new LMInventorySupplier(this, this);
    private final ItemContractable<LittleMaidEntity> itemContractable =
            new ItemContractable<>(this,
                    LMRBMod.getConfig().getConsumeSalaryInterval(),
                    LMRBMod.getConfig().getUnpaidCountLimit(),
                    stack -> stack.isIn(LMTags.Items.MAIDS_SALARY));
    private final ModeController modeController = new ModeController(this, this, new HashSet<>());
    private final MultiModelCompound multiModel;
    private final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private BlockPos freedomPos;
    @Environment(EnvType.CLIENT)
    private float interestedAngle;
    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;
    private int playSoundCool;

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
        setRandomTexture();
        soundPlayer = new SoundPlayableCompound(this, () ->
                multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
    }

    //基本使わない
    public LittleMaidEntity(World world) {
        this(Registration.LITTLE_MAID_MOB.get(), world);
    }

    //スタティックなメソッド

    public static DefaultAttributeContainer.Builder createLittleMaidAttributes() {
        DefaultAttributeContainer.Builder builder = TameableEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED)
                .add(EntityAttributes.GENERIC_LUCK)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16D);
        ReachAttributeUtil.addAttribute(builder);
        return builder;
    }

    public static boolean isValidNaturalSpawn(WorldAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                && world.getBaseLightLevel(pos, 0) > 8;
    }

    //登録メソッドたち

    @Override
    protected void initGoals() {
        super.initGoals();
        int priority = 0;

        LMRBConfig config = LMRBMod.getConfig();

        Predicate<Goal> healthPredicate =
                g -> config.getEmergencyTeleportHealthThreshold()
                        < MathHelper.clamp(
                        LittleMaidEntity.this.getHealth() / LittleMaidEntity.this.getMaxHealth(),
                        0, 1);

        this.goalSelector.add(0, new TeleportTameOwnerGoal<>(this, config.getTeleportStartRange()));
        //緊急テレポート
        this.goalSelector.add(0, new StartPredicateGoalWrapper<>(
                new TeleportTameOwnerGoal<>(this, config.getEmergencyTeleportStartRange()), healthPredicate.negate()));

        this.goalSelector.add(++priority, new SwimGoal(this));
        this.goalSelector.add(++priority, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(++priority, new HealMyselfGoal<>(this, config.getHealInterval(), config.getHealAmount(),
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));
        this.goalSelector.add(++priority, new WaitGoal<>(this));
        this.goalSelector.add(++priority, new StartPredicateGoalWrapper<>(
                new ModeWrapperGoal<>(this), healthPredicate));
        this.goalSelector.add(++priority,
                new FollowTameOwnerGoal<>(this, 1.5f, config.getSprintStartRange(), config.getSprintEndRange()));
        this.goalSelector.add(++priority, new FollowAtHeldItemGoal(this, this, true,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));
        this.goalSelector.add(++priority, new LMStareAtHeldItemGoal(this, this, false,
                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)));
        this.goalSelector.add(priority, new LMStareAtHeldItemGoal(this, this, true,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));
        this.goalSelector.add(++priority, new StartPredicateGoalWrapper<>(
                new LMMoveToDropItemGoal(this, 8, 1D), healthPredicate));
        this.goalSelector.add(++priority,
                new FollowTameOwnerGoal<>(this, 1.0f, config.getFollowStartRange(), config.getFollowEndRange()));
        this.goalSelector.add(++priority, new FreedomGoal<>(this, 0.8D, config.getFreedomRange()));
        this.goalSelector.add(++priority, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(priority, new LookAroundGoal(this));

        this.targetSelector.add(3, new PredicateRevengeGoal(this, entity -> !isFriend(entity)));
        this.targetSelector.add(4, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(5, new AttackWithOwnerGoal(this));
        this.targetSelector.add(6, new ActiveTargetGoal<>(
                this, LivingEntity.class, 5, true, false,
                this::isEnemy));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MOVING_STATE, (byte) 0);
        this.dataTracker.startTracking(AIMING, false);
        this.dataTracker.startTracking(BEGGING, false);
        this.dataTracker.startTracking(MODE_NAME, "");
        this.dataTracker.startTracking(BLOOD_SUCK, false);
    }

    public void addDefaultModes(LittleMaidEntity maid) {
        ModeManager.INSTANCE.getModes(maid).forEach(maid::addMode);
    }

    public void setRandomTexture() {
        LMTextureManager.INSTANCE.getAllTextures().stream()
                .filter(h -> h.hasSkinTexture(false))//野生テクスチャがある
                .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))//モデルがある
                .min(Comparator.comparingInt(h -> ThreadLocalRandom.current().nextInt()))//ランダム抽出
                .ifPresent(h -> Arrays.stream(TextureColors.values())
                        .filter(c -> h.getTexture(c, false, false).isPresent())
                        .min(Comparator.comparingInt(c -> ThreadLocalRandom.current().nextInt()))
                        .ifPresent(c -> {
                            this.setColor(c);
                            this.setTextureHolder(h, Layer.SKIN, Part.HEAD);
                            if (h.hasArmorTexture()) {
                                setTextureHolder(h, Layer.INNER, Part.HEAD);
                                setTextureHolder(h, Layer.INNER, Part.BODY);
                                setTextureHolder(h, Layer.INNER, Part.LEGS);
                                setTextureHolder(h, Layer.INNER, Part.FEET);
                                setTextureHolder(h, Layer.OUTER, Part.HEAD);
                                setTextureHolder(h, Layer.OUTER, Part.BODY);
                                setTextureHolder(h, Layer.OUTER, Part.LEGS);
                                setTextureHolder(h, Layer.OUTER, Part.FEET);
                            }
                        }));
    }

    //読み書き系

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        writeInventory(nbt);

        nbt.putInt("MovingState", getMovingState().getId());
        //後で消す
        String old = nbt.getString("MovingState");
        if (!old.isEmpty()) {
            setMovingState(MovingState.fromName(old));
        }

        if (freedomPos != null)
            nbt.put("FreedomPos", NbtHelper.fromBlockPos(freedomPos));

        writeContractable(nbt);

        writeModeData(nbt);

        nbt.putByte("SkinColor", (byte) getColor().getIndex());
        nbt.putBoolean("IsContract", isContract());
        nbt.putString("SkinTexture", getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            nbt.putString("ArmorTextureInner" + part.getPartName(),
                    getTextureHolder(Layer.INNER, part).getTextureName());
            nbt.putString("ArmorTextureOuter" + part.getPartName(),
                    getTextureHolder(Layer.OUTER, part).getTextureName());
        }

        nbt.putString("SoundConfigName", getConfigHolder().getName());

        writeIFF(nbt);

        setBloodSuck(nbt.getBoolean("isBloodSuck"));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        readInventory(nbt);

        setMovingState(MovingState.fromId(nbt.getInt("MovingState")));

        if (nbt.contains("FreedomPos"))
            freedomPos = NbtHelper.toBlockPos(nbt.getCompound("FreedomPos"));

        readContractable(nbt);

        readModeData(nbt);

        if (nbt.contains("SkinColor"))
            setColor(TextureColors.getColor(nbt.getByte("SkinColor")));
        setContract(nbt.getBoolean("IsContract"));
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        if (nbt.contains("SkinTexture")) {
            textureManager.getTexture(nbt.getString("SkinTexture"))
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        }
        for (Part part : Part.values()) {
            String inner = "ArmorTextureInner" + part.getPartName();
            String outer = "ArmorTextureOuter" + part.getPartName();
            if (nbt.contains(inner)) {
                textureManager.getTexture(nbt.getString(inner))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            }
            if (nbt.contains(outer)) {
                textureManager.getTexture(nbt.getString(outer))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
            }
        }

        if (nbt.contains("SoundConfigName"))
            LMConfigManager.INSTANCE.getConfig(nbt.getString("SoundConfigName"))
                    .ifPresent(this::setConfigHolder);

        readIFF(nbt);

        nbt.putBoolean("isBloodSuck", isBloodSuck());
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
        itemContractable.tick();
        if (world.isClient) {
            tickInterestedAngle();
        }
        playSoundCool = Math.max(0, playSoundCool - 1);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        modeController.tick();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return LMRBMod.getConfig().isCanDespawn() && !getTameOwnerUuid().isPresent();
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
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /**
     * 騎乗時のオフセット
     */
    @Override
    public double getHeightOffset() {
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getyOffset(getCaps()) - getHeight();
    }

    //このままだとEntityDimensionsが作っては捨てられてを繰り返すのでパフォーマンスはよろしくない
    //…が、そもそもそんなにたくさん呼ばれるメソッドでもない
    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions dimensions;
        IMultiModel model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps(), MMPose.convertPose(pose));
        float width = model.getWidth(getCaps(), MMPose.convertPose(pose));
        dimensions = EntityDimensions.changing(width, height);
        return dimensions.scaled(getScaleFactor());
    }

    @Nullable
    @Override
    public Entity moveToWorld(ServerWorld destination) {
        //ディメンション移動の時に、テレポ先を新たな自由行動地点とする
        Entity entity = super.moveToWorld(destination);
        if (entity == null) return null;
        if (entity instanceof LittleMaidEntity) {
            ((LittleMaidEntity) entity).setFreedomPos(((LittleMaidEntity) entity).getFreedomPos());
        }
        return entity;
    }

    @Override
    public boolean isInWalkTargetRange(BlockPos pos) {
        //自身または主人から16ブロック以内
        if (pos.isWithinDistance(pos, 16)
                || getTameOwner().filter(owner -> owner.getBlockPos().isWithinDistance(pos, 16)).isPresent()) {
            return super.isInWalkTargetRange(pos);
        }
        return false;
    }

    @Override
    public void playAmbientSound() {
        if (world.isClient || this.dead || getConfigHolder()
                .getParameter("LivingVoiceRate")
                .map(s -> {
                    try {
                        return Float.parseFloat(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(0.2f) < random.nextFloat()) {
            return;
        }
        if (getHealth() / getMaxHealth() < 0.3F) {
            play(LMSounds.LIVING_WHINE);
        } else {
            if (age % 4 == 0 && this.world.isSkyVisible(this.getBlockPos())) {
                Biome biome = this.world.getBiome(getBlockPos()).value();
                if (biome.isCold(getBlockPos())) {
                    play(LMSounds.LIVING_COLD);
                } else if (biome.isHot(getBlockPos())) {
                    play(LMSounds.LIVING_HOT);
                }
            } else if (age % 4 == 1 && world.isRaining()) {
                Biome biome = this.world.getBiome(getBlockPos()).value();
                if (biome.getPrecipitation() == Biome.Precipitation.RAIN)
                    play(LMSounds.LIVING_RAIN);
                else if (biome.getPrecipitation() == Biome.Precipitation.SNOW)
                    play(LMSounds.LIVING_SNOW);
            } else {
                if (this.getMainHandStack().getItem() == Items.CLOCK
                        || this.getOffHandStack().getItem() == Items.CLOCK) {
                    int time = (int) (world.getTimeOfDay() % 24000);
                    //時間約23500-1500はse_living_morning
                    //時間約12500-23500はse_living_night
                    if (time < 1500 || 23500 <= time) {
                        play(LMSounds.LIVING_MORNING);
                    } else if (12500 <= time) {
                        play(LMSounds.LIVING_NIGHT);
                    } else {
                        play(LMSounds.LIVING_DAYTIME);
                    }
                } else {
                    play(LMSounds.LIVING_DAYTIME);
                }
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        if (LMRBMod.getConfig().isCanResurrection()) {
            this.unsetRemoved();
            this.dead = false;
            this.deathTime = 0;
            this.setHealth(this.getMaxHealth());
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 60, 5));
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 60, 5));
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20 * 60, 5));
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 60, 5));
            return;
        }
        super.onDeath(source);
        play(LMSounds.DEATH);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.dead) {
            return super.damage(source, amount);
        }
        if (!world.isClient) {
            //味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getSource() instanceof SnowballEntity) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        LMRBConfig config = LMRBMod.getConfig();
        if (config.isNonMobDamageImmunity() && source.getAttacker() == null) {
            return false;
        }
        if (config.isImmortal() && source != DamageSource.OUT_OF_WORLD && !source.isSourceCreativePlayer()) {
            return false;
        }
        if (config.isFallImmunity() && source == DamageSource.FALL) {
            return false;
        }
        Entity attacker = source.getAttacker();
        //Friendからの攻撃を除外
        if (!config.isFriendlyFire() && attacker instanceof LivingEntity && isFriend((LivingEntity) attacker)) {
            return false;
        }
        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.damage(source, amount);
        if (!world.isClient && !isHurtTime) {
            if (getTameOwnerUuid().isPresent() && this.getMovingState() == WAIT && result && 0 < amount) {
                this.setMovingState(ESCORT);
            }
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
    public void onKilledOther(ServerWorld world, LivingEntity other) {
        if (isBloodSuck()) play(LMSounds.LAUGHTER);

        super.onKilledOther(world, other);
    }

    @Override
    protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
        if (type != MovementType.SELF && type != MovementType.PLAYER) {
            return movement;
        }

        LMRBConfig config = LMRBMod.getConfig();

        if (!config.isCanMoveToDanger()) {

            //危険物に絶対触れない
            if (!LMRBMod.getConfig().isNonMobDamageImmunity() && isDamageSourceEmpty(this.getBoundingBox())
                    && !this.isDamageSourceEmpty(this.getBoundingBox().offset(movement.x, 0, movement.z))) {
                movement = pushBack(movement, (x, z) ->
                        !this.isDamageSourceEmpty(this.getBoundingBox().offset(x, 0, z)));
            }

            //絶対に飛び降りない
            if (!config.isFallImmunity()
                    && this.canClipAtLedge()
                    && !isSafeFallHeight(this.getPos().add(movement.x, 0, movement.z))) {
                movement = pushBack(movement, (x, z) ->
                        //着地までにダメージを受けない高さに足場がない
                        this.world.isSpaceEmpty(this, this.getBoundingBox()
                                .offset(x, 0, z)
                                .stretch(0, -(getDangerHeightThreshold() - fallDistance), 0)));
            }
        }

        return movement;
    }

    private Vec3d pushBack(Vec3d movement, BiPredicate<Double, Double> pushBackPredicate) {
        double dot = 0.05;
        double mX = movement.x;
        double mZ = movement.z;
        while (mX != 0.0 && pushBackPredicate.test(mX, 0d)) {
            if (mX < dot && mX >= -dot) {
                mX = 0.0;
                continue;
            }
            if (mX > 0.0) {
                mX -= dot;
                continue;
            }
            mX += dot;
        }
        while (mZ != 0.0 && pushBackPredicate.test(0d, mZ)) {
            if (mZ < dot && mZ >= -dot) {
                mZ = 0.0;
                continue;
            }
            if (mZ > 0.0) {
                mZ -= dot;
                continue;
            }
            mZ += dot;
        }
        while (mX != 0.0 && mZ != 0.0 && pushBackPredicate.test(mX, mZ)) {
            mX = mX < dot && mX >= -dot ? 0.0 : (mX > 0.0 ? (mX -= dot) : (mX += dot));
            if (mZ < dot && mZ >= -dot) {
                mZ = 0.0;
                continue;
            }
            if (mZ > 0.0) {
                mZ -= dot;
                continue;
            }
            mZ += dot;
        }
        return new Vec3d(mX, movement.y, mZ);
    }

    private boolean isDamageSourceEmpty(Box box) {
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);

        for (int x = 0; x < maxX - minX + 1; x++) {
            for (int y = 0; y < maxY - minY + 1; y++) {
                for (int z = 0; z < maxZ - minZ + 1; z++) {
                    PathNodeType pathNodeType = this.getNavigation().getNodeMaker()
                            .getDefaultNodeType(this.world, minX + x, minY + y, minZ + z);
                    if (pathNodeType == PathNodeType.DAMAGE_FIRE
                            || pathNodeType == PathNodeType.DAMAGE_CACTUS
                            || pathNodeType == PathNodeType.DAMAGE_OTHER
                            || pathNodeType == PathNodeType.LAVA) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isSafeFallHeight(Vec3d pos) {
        BlockHitResult result = this.world.raycast(new RaycastContext(
                pos,
                pos.subtract(0, getDangerHeightThreshold() + 0.1, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (result.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3d hitPos = result.getPos();
        if (getDangerHeightThreshold() < pos.y - hitPos.y) {
            return false;
        }
        BlockPos checkPos = new BlockPos(pos.x, pos.y - 1, pos.z);
        for (int i = 0; i < pos.y - hitPos.y + 1; i++) {
            PathNodeType pathNodeType = this.getNavigation().getNodeMaker()
                    .getDefaultNodeType(this.world, checkPos.getX(), checkPos.getY(), checkPos.getZ());
            if (pathNodeType == PathNodeType.WALKABLE || pathNodeType == PathNodeType.BLOCKED) {
                return true;
            }
            if (pathNodeType == PathNodeType.DAMAGE_FIRE
                    || pathNodeType == PathNodeType.DAMAGE_CACTUS
                    || pathNodeType == PathNodeType.DAMAGE_OTHER
                    || pathNodeType == PathNodeType.LAVA) {
                return false;
            }
            checkPos = checkPos.down();
        }
        return false;
    }

    private boolean canClipAtLedge() {
        float canClipHeight = getDangerHeightThreshold();
        //着地しているか、落下距離が危険高度未満かつ下に足場があるとき
        return this.onGround || this.fallDistance < canClipHeight
                && !this.world.isSpaceEmpty(this, this.getBoundingBox()
                .stretch(0.0, this.fallDistance - canClipHeight, 0.0));
    }

    private float getDangerHeightThreshold() {
        //マイナスの値も返すことを利用しているため、バージョンアップ/mixinでの仕様変更に注意が必要
        int fallDamage = computeFallDamage(0, 1);
        return -fallDamage;
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
            if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(player, stack, false);
            }
            return ActionResult.PASS;
        }
        if (!player.getUuid().equals(this.getOwnerUuid())) {
            return ActionResult.PASS;
        }
        if (isStrike()) {
            if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
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
        if (stack.isIn(LMTags.Items.MAIDS_SALARY)) {
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
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.getInventory().removeOne(stack);
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
        setStrike(false);
        itemContractable.setUnpaidTimes(0);
        getNavigation().stop();
        this.setOwnerUuid(player.getUuid());
        setMovingState(ESCORT);
        setContract(true);
        itemContractable.setContract(true);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.getInventory().removeOne(stack);
            }
        }
        return ActionResult.success(world.isClient);
    }

    //GUI開くやつ
    public void openContainer(PlayerEntity player) {
        setAttacker(null);
        getNavigation().stop();
        setModeName(getMode().map(Mode::getName).orElse(""));
        MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, screenFactory);
    }

    //インベントリ関連
    //todo PlayerEntityでinventoryに対してアクセスしてるメソッドをすべて実装すべき

    @Override
    public Inventory getInventory() {
        return this.littleMaidInventory.getInventory();
    }

    @Override
    public void writeInventory(NbtCompound tag) {
        this.littleMaidInventory.writeInventory(tag);
    }

    @Override
    public void readInventory(NbtCompound tag) {
        this.littleMaidInventory.readInventory(tag);
    }

    @Override
    public Iterable<ItemStack> getItemsHand() {
        return Lists.newArrayList(getMainHandStack(), getOffHandStack());
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
    protected void damageArmor(DamageSource source, float amount) {
        ((PlayerInventory) getInventory()).damageArmor(source, amount, PlayerInventory.ARMOR_SLOTS);
    }

    @Override
    protected void damageHelmet(DamageSource source, float amount) {
        ((PlayerInventory) getInventory()).damageArmor(source, amount, PlayerInventory.HELMET_SLOTS);
    }

    //メイドさんはガードしないので要らないかも
    @Override
    protected void damageShield(float amount) {
        if (this.activeItemStack.isOf(Items.SHIELD)) {

            if (amount >= 3.0F) {
                int i = 1 + MathHelper.floor(amount);
                Hand hand = this.getActiveHand();
                this.activeItemStack.damage(i, (LivingEntity) this, (playerEntity) -> playerEntity.sendToolBreakStatus(hand));
                if (this.activeItemStack.isEmpty()) {
                    if (hand == Hand.MAIN_HAND) {
                        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }

                    this.activeItemStack = ItemStack.EMPTY;
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8F, 0.8F + this.world.random.nextFloat() * 0.4F);
                }
            }

        }
    }

    @Override
    public StackReference getStackReference(int mappedIndex) {
        if (mappedIndex >= 0 && mappedIndex < 36) {
            return StackReference.of(this.getInventory(), mappedIndex);
        }
        return super.getStackReference(mappedIndex);
    }

    //要る？
    @Override
    public ItemStack getArrowType(ItemStack stack) {
        if (!(stack.getItem() instanceof RangedWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((RangedWeaponItem) stack.getItem()).getHeldProjectiles();
            ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
            if (!itemStack.isEmpty()) {
                return itemStack;
            } else {
                predicate = ((RangedWeaponItem) stack.getItem()).getProjectiles();

                for (int i = 0; i < this.getInventory().size(); ++i) {
                    ItemStack itemStack2 = this.getInventory().getStack(i);
                    if (predicate.test(itemStack2)) {
                        return itemStack2;
                    }
                }

                return ItemStack.EMPTY;
            }
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
    protected void dropInventory() {
        //鯖側でしか動かないが一応チェック
        Inventory inv = this.getInventory();
        if (inv instanceof PlayerInventory)
            ((LMInventorySupplier.LMInventory) inv).dropAll();
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

    @Override
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
        if (freedomPos == null) return getBlockPos();
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

    public boolean isBloodSuck() {
        return this.dataTracker.get(BLOOD_SUCK);
    }

    public void setBloodSuck(boolean isBloodSuck) {
        this.dataTracker.set(BLOOD_SUCK, isBloodSuck);
    }

    @Environment(EnvType.CLIENT)
    public float getInterestedAngle(float tickDelta) {
        return (prevInterestedAngle + (interestedAngle - prevInterestedAngle) * tickDelta) *
                ((getId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI);
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
    public boolean isStrike() {
        return itemContractable.isStrike();
    }

    @Override
    public void setStrike(boolean strike) {
        itemContractable.setStrike(strike);
    }

    @Override
    public void writeContractable(NbtCompound nbt) {
        itemContractable.writeContractable(nbt);
    }

    @Override
    public void readContractable(NbtCompound nbt) {
        itemContractable.readContractable(nbt);
    }

    public int getUnpaidDays() {
        return itemContractable.getUnpaidTimes();
    }

    //モード機能

    @Override
    public Optional<Mode> getMode() {
        return modeController.getMode();
    }

    @Override
    public void writeModeData(NbtCompound tag) {
        modeController.writeModeData(tag);
    }

    @Override
    public void readModeData(NbtCompound tag) {
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
    public Optional<IFFTag> identify(LivingEntity target) {
        UUID ownerId = this.getOwnerUuid();
        if (ownerId != null) {
            //主はフレンド
            if (ownerId.equals(target.getUuid())) {
                return Optional.of(IFFTag.FRIEND);
            }
            //同じ主を持つ者はフレンド
            if (target instanceof Tameable && ownerId.equals(((Tameable) target).getTameOwnerUuid().orElse(null))
                    || target instanceof TameableEntity && ownerId.equals(((TameableEntity) target).getOwnerUuid())) {
                return Optional.of(IFFTag.FRIEND);
            }
        }
        return getTameOwner()
                .filter(owner -> owner instanceof HasIFF)
                .map(owner -> (HasIFF) owner)
                .flatMap(t -> t.identify(target));
    }

    @Override
    public void setIFFs(List<IFF> iffs) {
    }

    @Override
    public List<IFF> getIFFs() {
        return Lists.newArrayList();
    }

    @Override
    public void writeIFF(NbtCompound nbt) {
    }

    @Override
    public void readIFF(NbtCompound nbt) {
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return !isFriend(target);
    }

    public boolean isFriend(LivingEntity entity) {
        return identify(entity).orElse(null) == IFFTag.FRIEND;
    }

    public boolean isEnemy(LivingEntity entity) {
        return isBloodSuck() ? !isFriend(entity) : identify(entity).orElse(null) == IFFTag.ENEMY;
    }

    //構え

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
        itemContractable.setContract(isContract);
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
    public Optional<IMultiModel> getModel(Layer layer, Part part) {
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
        if (0 < this.playSoundCool) {
            return;
        }
        this.playSoundCool = 5;
        if (isBloodSuck()) {
            if (soundName.equals(LMSounds.FIND_TARGET_N)) {
                soundName = LMSounds.FIND_TARGET_B;
            } else if (soundName.equals(LMSounds.ATTACK)) {
                soundName = LMSounds.ATTACK_BLOOD_SUCK;
            }
        }
        if (this.world.isClient) {
            this.getConfigHolder().getSoundFileName(soundName.toLowerCase())
                    .ifPresent((soundFileName) ->
                            LMSoundManager.INSTANCE.play(soundFileName, this.getSoundCategory(),
                                    LMRBMod.getConfig().getVoiceVolume(), 1.0F,
                                    this.getX(), this.getEyeY(), this.getZ()));
            return;
        }
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
        protected final LittleMaidEntity maid;

        public LMMoveToDropItemGoal(LittleMaidEntity maid, int range, double speed) {
            super(maid, range, speed);
            this.maid = maid;
        }

        @Override
        public boolean canStart() {
            return maid.getMovingState() != WAIT && ((PlayerInventory) maid.getInventory()).getEmptySlot() != -1 && super.canStart();
        }

        @Override
        public List<ItemEntity> findAroundDropItem() {
            return maid.getTameOwner()
                    .filter(owner -> maid.getMovingState() != WAIT)
                    .map(owner -> {
                        return super.findAroundDropItem().stream()
                                .filter(item -> !isOwnerRange(item, owner))
                                .collect(Collectors.toList());
                        //ご主人様が存在しない場合は普通にとる
                    }).orElse(super.findAroundDropItem());
        }

        private boolean isOwnerRange(Entity entity, Entity owner) {
            final Vec3d ownerPos = owner.getPos();
            final Vec3d entityPos = entity.getPos().subtract(ownerPos);
            final Vec3d ownerRot = owner.getRotationVec(1F).multiply(4);
            final double dot = entityPos.dotProduct(ownerRot);
            final double range = 4;
            //プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
            //かつ内積の大きさが4m以下
            return 0 < dot && dot < range * range;
        }
    }

    public static class LMStareAtHeldItemGoal extends TameableStareAtHeldItemGoal {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(LittleMaidEntity maid, Tameable tameable, boolean isTamed, Predicate<ItemStack> targetItem) {
            super(maid, tameable, isTamed, targetItem);
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
