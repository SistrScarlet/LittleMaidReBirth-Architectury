package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import dev.architectury.extensions.network.EntitySpawnExtension;
import dev.architectury.networking.NetworkManager;
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
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.particle.DustParticleEffect;
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
import net.sistr.littlemaidmodelloader.network.SyncMultiModelPacket;
import net.sistr.littlemaidmodelloader.resource.holder.ConfigHolder;
import net.sistr.littlemaidmodelloader.resource.holder.TextureHolder;
import net.sistr.littlemaidmodelloader.resource.manager.LMConfigManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMModelManager;
import net.sistr.littlemaidmodelloader.resource.manager.LMTextureManager;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidmodelloader.resource.util.TextureColors;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.advancement.criterion.LMRBCriteria;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.goal.*;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.mode.HasMode;
import net.sistr.littlemaidrebirth.entity.mode.HasModeImpl;
import net.sistr.littlemaidrebirth.entity.mode.ModeWrapperGoal;
import net.sistr.littlemaidrebirth.entity.util.Tameable;
import net.sistr.littlemaidrebirth.entity.util.*;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//メイドさん本体
//todo 声タイミング調整
//todo ドロップアイテム
//todo 契約期間の残りは砂糖をあげた時の音符の色で判断してください。 - 簡単
//todo 雪合戦 日が暮れると遊びは終わり - 優先
//todo モードトリガーアイテム指定
//todo 署名済みではない書き込み可能な本にパラメータを記述して、メイドさんに右クリックで使用すると値が反映されます。
//todo メイドさんも金リンゴや牛乳を飲めるようになりました。 - 簡単
//todo つまみ食い - 優先
//todo ダメージ/水没待機解除 - 実装済み？
//todo トランザム
//todo 経験値
//todo 座ったメイドでも追従時に立つように
//todo スト時砂糖ドカ食い - 簡単
//todo GUIを開いている時に動きを止める - 優先
//todo リスポ
//todo 死亡メッセ追加 - 優先
//todo はしご
//todo おさわり厳禁：他人のメイドに触ると殴られる
//todo 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる
public class LittleMaidEntity extends TameableEntity implements EntitySpawnExtension, HasInventory, net.sistr.littlemaidrebirth.entity.util.Tameable,
        Contractable, HasMode, HasIFF, AimingPoseable, HasFakePlayer, IHasMultiModel, SoundPlayable, HasMovingMode {
    //LMM_FLAGSのindex
    private static final int WAIT_INDEX = 0;
    private static final int AIMING_INDEX = 1;
    private static final int BEGGING_INDEX = 2;
    private static final int BLOOD_SUCK_INDEX = 3;
    private static final int STRIKE_INDEX = 4;
    private static final TrackedData<Byte> LMM_FLAGS =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> MOVING_MODE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> MODE_NAME =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    //移譲s
    private final LMHasFakePlayer fakePlayer = new LMHasFakePlayer(this);
    private final LMHasInventory littleMaidInventory = new LMHasInventory(this, this);
    private final ItemContractable<LittleMaidEntity> itemContractable =
            new ItemContractable<>(this,
                    LMRBMod.getConfig().getConsumeSalaryInterval(),
                    LMRBMod.getConfig().getUnpaidCountLimit(),
                    stack -> stack.isIn(LMTags.Items.MAIDS_SALARY),
                    mob -> {
                        mob.setStrike(true);
                        mob.setWait(false);
                        if (mob.getMovingMode() != MovingMode.FREEDOM) {
                            mob.setMovingMode(MovingMode.FREEDOM);
                            mob.freedomPos = mob.getBlockPos();
                        }
                    });
    private final HasModeImpl hasModeImpl = new HasModeImpl(this, this, new HashSet<>());
    private final MultiModelCompound multiModel;
    private final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final IModelCaps caps = new LittleMaidModelCaps(this);

    @Nullable
    private BlockPos freedomPos;
    //首傾げのやつ
    @Environment(EnvType.CLIENT)
    private float interestedAngle;
    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;
    private int playSoundCool;
    private int idFactor;

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
        initIdFactor();

        //野良の子らの初期設定
        setRandomTexture();
        if (LMRBMod.getConfig().isSilentDefaultVoice()) {
            soundPlayer.setConfigHolder(LMConfigManager.EMPTY_CONFIG);
        } else {
            List<ConfigHolder> configs = LMConfigManager.INSTANCE.getAllConfig();
            soundPlayer.setConfigHolder(configs.get(idFactor % configs.size()));
        }
        String defaultSoundPackName = LMRBMod.getConfig().getDefaultSoundPackName();
        if (!defaultSoundPackName.isEmpty()) {
            LMConfigManager.INSTANCE.getAllConfig().stream()
                    .filter(c -> c.getPackName().equalsIgnoreCase(defaultSoundPackName))
                    .findAny()
                    .ifPresent(soundPlayer::setConfigHolder);
        }
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

    //todo 速度をconfig化
    @Override
    protected void initGoals() {
        int priority = -1;
        LMRBConfig config = LMRBMod.getConfig();

        Predicate<Goal> healthPredicate =
                g -> config.getEmergencyTeleportHealthThreshold()
                        < MathHelper.clamp(
                        LittleMaidEntity.this.getHealth() / LittleMaidEntity.this.getMaxHealth(),
                        0, 1);

        this.goalSelector.add(++priority, new HasMMTeleportTameOwnerGoal<>(this,
                config.getTeleportStartRange()));
        //緊急テレポート
        this.goalSelector.add(priority, new StartPredicateGoalWrapper<>(
                new HasMMTeleportTameOwnerGoal<>(this,
                        config.getEmergencyTeleportStartRange()),
                healthPredicate.negate()));

        this.goalSelector.add(++priority, new SwimGoal(this));
        this.goalSelector.add(++priority, new LongDoorInteractGoal(this, true));

        this.goalSelector.add(++priority, new HealMyselfGoal<>(this,
                config.getHealInterval(),
                config.getHealAmount(),
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));

        this.goalSelector.add(++priority, new WaitGoal<>(this));

        this.goalSelector.add(++priority, new StartPredicateGoalWrapper<>(
                new ModeWrapperGoal<>(this), healthPredicate));

        this.goalSelector.add(++priority,
                new HasMMFollowTameOwnerGoal<>(
                        this,
                        1.0f,
                        config.getSprintStartRange(),
                        config.getSprintEndRange()) {
                    @Override
                    public void start() {
                        super.start();
                        this.tameable.setSprinting(true);
                    }

                    @Override
                    public void stop() {
                        super.stop();
                        this.tameable.setSprinting(false);
                    }
                });

        this.goalSelector.add(++priority, new FollowAtHeldItemGoal<>(this,
                true,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));
        this.goalSelector.add(priority, new LMStareAtHeldItemGoal<>(this,
                true,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)));

        this.goalSelector.add(++priority,
                new HasMMFollowTameOwnerGoal<>(
                        this,
                        1.0f,
                        config.getFollowStartRange(),
                        config.getFollowEndRange()));

        //todo 頭の装飾品を仕舞わないようにする
        this.goalSelector.add(++priority, new LMStoreItemToContainerGoal<>(this,
                stack -> stack.isIn(LMTags.Items.MAIDS_SALARY)
                        || this.hasModeImpl.getMode()
                        .filter(mode -> mode.getModeType().isModeItem(stack))
                        .isPresent(),
                128,
                8,
                100));

        this.goalSelector.add(++priority, new RedstoneTraceGoal(this, 0.65f));
        this.goalSelector.add(++priority, new FreedomGoal<>(this,
                0.65D, config.getFreedomRange()));

        this.goalSelector.add(++priority, new StartPredicateGoalWrapper<>(
                new LMMoveToDropItemGoal(this, 8, 1D), healthPredicate));

        //野良
        //todo 逃げる
        this.goalSelector.add(++priority, new StartPredicateGoalWrapper<>(
                new EscapeDangerGoal(this, 1.25),
                goal -> this.getTameOwner().isEmpty()));
        this.goalSelector.add(++priority, new FollowAtHeldItemGoal<>(this, false,
                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)));
        this.goalSelector.add(++priority, new LMStareAtHeldItemGoal<>(this, false,
                stack -> stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)));
        this.goalSelector.add(++priority, new StartPredicateGoalWrapper<>(
                new WanderAroundFarGoal(this, 0.65f),
                goal -> this.getTameOwner().isEmpty()));

        //視線
        this.goalSelector.add(++priority, new LookAtEntityGoal(this, LivingEntity.class, 8.0F));
        this.goalSelector.add(priority, new LookAroundGoal(this));

        //ターゲット系
        priority = -1;
        this.targetSelector.add(++priority, new PredicateRevengeGoal(this, entity -> !isFriend(entity)));
        this.targetSelector.add(++priority, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(++priority, new AttackWithOwnerGoal(this));
        this.targetSelector.add(++priority, new ActiveTargetGoal<>(
                this, LivingEntity.class, 5, true, false,
                this::isEnemy));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(LMM_FLAGS, (byte) 0);
        this.dataTracker.startTracking(MOVING_MODE, (byte) 0);
        this.dataTracker.startTracking(MODE_NAME, "");
    }

    public void addDefaultModes(LittleMaidEntity maid) {
        ModeManager.INSTANCE.createModes(maid).forEach(maid::addMode);
    }

    //読み書き系

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        writeInventory(nbt);

        if (getTameOwnerUuid().isPresent()) {
            nbt.putBoolean("Wait", this.isWait());
            nbt.putByte("MovingMode", (byte) this.getMovingMode().getId());
            writeContractable(nbt);
            writeIFF(nbt);
            writeModeData(nbt);
            nbt.putBoolean("isBloodSuck", isBloodSuck());
            if (this.getMovingMode() == MovingMode.FREEDOM
                    && freedomPos != null) {
                nbt.put("FreedomPos", NbtHelper.fromBlockPos(freedomPos));
            }
            this.multiModel.writeToNbt(nbt);
            nbt.putString("SoundConfigName", getConfigHolder().getName());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        readInventory(nbt);

        if (getTameOwnerUuid().isPresent()) {
            setWait(nbt.getBoolean("Wait"));
            setMovingMode(MovingMode.fromId(nbt.getInt("MovingMode")));
            readContractable(nbt);
            readIFF(nbt);
            readModeData(nbt);
            setBloodSuck(nbt.getBoolean("isBloodSuck"));
            if (this.getMovingMode() == MovingMode.FREEDOM
                    && nbt.contains("FreedomPos")) {
                freedomPos = NbtHelper.toBlockPos(nbt.getCompound("FreedomPos"));
            }
            this.multiModel.readFromNbt(nbt);
            this.calculateDimensions();
            if (nbt.contains("SoundConfigName")) {
                LMConfigManager.INSTANCE.getConfig(nbt.getString("SoundConfigName"))
                        .ifPresent(this::setConfigHolder);
            }
        }
    }

    public void setRandomTexture() {
        var textureHolderList = LMTextureManager.INSTANCE.getAllTextures().stream()
                .filter(h -> h.hasSkinTexture(false))//野生テクスチャがある
                .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))
                .toList();
        if (textureHolderList.isEmpty()) {
            return;
        }
        var textureHolder = textureHolderList.get(idFactor % textureHolderList.size());
        var colorList = Arrays.stream(TextureColors.values())
                .filter(c -> textureHolder.getTexture(c, false, false).isPresent())
                .toList();
        if (colorList.isEmpty()) {
            return;
        }
        var color = colorList.get(idFactor % colorList.size());
        this.setColorMM(color);
        this.setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD);
        if (textureHolder.hasArmorTexture()) {
            setTextureHolder(textureHolder, Layer.INNER, Part.HEAD);
            setTextureHolder(textureHolder, Layer.INNER, Part.BODY);
            setTextureHolder(textureHolder, Layer.INNER, Part.LEGS);
            setTextureHolder(textureHolder, Layer.INNER, Part.FEET);
            setTextureHolder(textureHolder, Layer.OUTER, Part.HEAD);
            setTextureHolder(textureHolder, Layer.OUTER, Part.BODY);
            setTextureHolder(textureHolder, Layer.OUTER, Part.LEGS);
            setTextureHolder(textureHolder, Layer.OUTER, Part.FEET);
        }
    }

    //鯖
    @Override
    public void saveAdditionalSpawnData(PacketByteBuf buf) {
        //モデル
        buf.writeEnumConstant(getColorMM());
        buf.writeBoolean(isContract());
        buf.writeString(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeString(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeString(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        //サウンド
        buf.writeString(getConfigHolder().getName());
        //頭の装飾品が表示されない対策
        //原因はインベントリを開くまで同期されないため
        buf.writeItemStack(getInventory().getStack(18));
    }

    //蔵
    @Override
    public void loadAdditionalSpawnData(PacketByteBuf buf) {
        //モデル
        //readString()はクラ処理。このメソッドでは、クラ側なので問題なし
        setColorMM(buf.readEnumConstant(TextureColors.class));
        setContractMM(buf.readBoolean());
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

        getInventory().setStack(18, buf.readItemStack());
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case 70 -> {//雇用時
                showEmoteParticle(true);
                play(LMSounds.GET_CAKE);
            }
            case 71 -> {//再雇用時
                showEmoteParticle(true);
                play(LMSounds.RECONTRACT);
            }
            case 72 -> {//砂糖あげた時
                this.world.addParticle(ParticleTypes.NOTE,
                        this.getX(),
                        this.getY() + this.getHeight(),
                        this.getZ(),
                        6 / 24f, 0, 0);
            }
            case 73 -> showFreedomParticle();//toFreedom
            case 74 -> showEmoteParticle(false);//toEscort
            case 75 -> showTracerParticle();//toTracer
            default -> super.handleStatus(status);
        }
    }

    protected void showFreedomParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.world.addParticle(new DustParticleEffect(
                            new Vector3f(
                                    this.random.nextFloat(),
                                    this.random.nextFloat(),
                                    this.random.nextFloat()),
                            1.0f),
                    this.getParticleX(1.0),
                    this.getRandomBodyY() + 0.5,
                    this.getParticleZ(1.0),
                    d, e, f);
        }
    }

    protected void showTracerParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.world.addParticle(ParticleTypes.CLOUD,
                    this.getParticleX(1.0),
                    this.getRandomBodyY() + 0.5,
                    this.getParticleZ(1.0),
                    d, e, f);
        }
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
        hasModeImpl.tick();
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return LMRBMod.getConfig().isCanDespawn() && getTameOwnerUuid().isEmpty();
    }

    //canSpawnとかでも使われる
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos.down()).isFullCube(world, pos) ? 10.0F : world.getPhototaxisFavor(pos);
    }

    @Override
    protected float getDropChance(EquipmentSlot slot) {
        return 0;
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target) && !isFriend(target);
    }

    @Nullable
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
        //ディメンション移動の時に、自由行動地点を削除する
        Entity entity = super.moveToWorld(destination);
        if (entity == null) return null;
        if (entity instanceof LittleMaidEntity
                && this.getMovingMode() == MovingMode.FREEDOM) {
            ((LittleMaidEntity) entity).setFreedomPos(null);
        }
        return entity;
    }

    @Override
    public boolean isInWalkTargetRange(BlockPos pos) {
        //自身または主人から16ブロック以内
        if (pos.isWithinDistance(pos, 16)
                || getTameOwner()
                .filter(owner -> owner.getBlockPos().isWithinDistance(pos, 16))
                .isPresent()) {
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
            if (result && 0 < amount && this.isWait() && getTameOwnerUuid().isPresent()) {
                this.setWait(false);
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
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        if (isBloodSuck()) play(LMSounds.LAUGHTER);

        return super.onKilledOther(world, other);
    }

    @Override
    protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
        if (type != MovementType.SELF && type != MovementType.PLAYER) {
            return movement;
        }

        LMRBConfig config = LMRBMod.getConfig();

        if (!config.isImmortal() && !LMRBMod.getConfig().isNonMobDamageImmunity() && !config.isCanMoveToDanger()
                && this.canClipAtLedge()) {
            boolean shouldBackByDamage = isDamageSourceEmpty(this.getBoundingBox())
                    && !this.isDamageSourceEmpty(this.getBoundingBox().offset(movement.x, 0, movement.z));
            boolean shouldBackByFall = !config.isFallImmunity()
                    && !isSafeFallHeight(this.getPos().add(movement.x, 0, movement.z));

            if (shouldBackByDamage || shouldBackByFall) {
                BiPredicate<Double, Double> shouldBackPredicate = (x, z) -> false;
                if (shouldBackByDamage) {
                    BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
                    shouldBackPredicate = (x, z) -> finalPredicate.test(x, z)
                            //危険物がbox内にある
                            || !this.isDamageSourceEmpty(this.getBoundingBox().offset(x, 0, z));
                }

                if (shouldBackByFall) {
                    BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
                    shouldBackPredicate = (x, z) -> finalPredicate.test(x, z)
                            //足場がbox内にない
                            || this.world.isSpaceEmpty(this, this.getBoundingBox()
                            .offset(x, 0, z)
                            .stretch(0, -(getDangerHeightThreshold() - fallDistance), 0))
                            //または、すぐ下に足場がなく、危険物がbox内にある
                            || (this.world.isSpaceEmpty(this, this.getBoundingBox()
                            .offset(x, 0, z)
                            .stretch(0, -stepHeight, 0))
                            && !this.isDamageSourceEmpty(this.getBoundingBox().offset(x, 0, z)
                            .stretch(0, -getDangerHeightThreshold(), 0)));
                }

                movement = pushBack(movement, shouldBackPredicate);
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
                pos.subtract(0, getDangerHeightThreshold() - fallDistance + 0.1, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (result.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3d hitPos = result.getPos();
        if (getDangerHeightThreshold() - fallDistance < pos.y - hitPos.y) {
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
        float canClipHeight = getDangerHeightThreshold() + 1.0f;
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

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0, this.getStandingEyeHeight() - 0.15f, 1f / 16f);
    }

    //success 動作を実行し、手を振る
    //consume 動作を実行するが、手を振らない
    //pass 動作を実行しないが、他の動作を許可する
    //fail 動作を実行せず、他の動作も許可しない
    //下二つならここ以外で手に持ったアイテムが使用される場合がある
    //継承元のコードは無視
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        //オーナーが居ない場合
        if (!hasTameOwner()) {
            if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(player, stack, false);
            }
            return ActionResult.PASS;
        }
        //オーナーじゃない場合
        if (!player.getUuid().equals(this.getOwnerUuid())) {
            return ActionResult.PASS;
        }
        //ストライキ時
        if (isStrike()) {
            if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
                return contract(player, stack, true);
            }
            this.world.sendEntityStatus(this, (byte) 6);
            return ActionResult.PASS;
        }
        //サドル持ってるとき
        if (stack.getItem() instanceof SaddleItem) {
            if (!this.hasVehicle()) {
                if (player.hasPassengers()) {
                    player.removeAllPassengers();
                }
                this.startRiding(player);
            } else {
                var vehicle = this.getVehicle();
                if (vehicle == player) {
                    this.stopRiding();
                }
            }
            return ActionResult.success(this.world.isClient);
        }
        //肩車されてるとき
        if (this.getVehicle() == player) {
            return ActionResult.PASS;
        }
        //砂糖
        if (stack.isIn(LMTags.Items.MAIDS_SALARY)) {
            var config = LMRBMod.getConfig();
            heal(config.getHealAmount());
            return changeState(player, stack);
        }
        //Freedom切替
        if (stack.getItem() == Items.FEATHER) {
            if (getMovingMode() == MovingMode.ESCORT) {
                this.world.sendEntityStatus(this, (byte) 73);
                this.setMovingMode(MovingMode.FREEDOM);
                this.setFreedomPos(this.getBlockPos());
            } else {
                this.world.sendEntityStatus(this, (byte) 74);
                this.setMovingMode(MovingMode.ESCORT);
            }
            return ActionResult.success(this.world.isClient);
        }
        //Tracer切替
        if ((this.getMovingMode() == MovingMode.FREEDOM
                || this.getMovingMode() == MovingMode.TRACER)
                && stack.getItem() == Items.REDSTONE) {
            if (this.getMovingMode() == MovingMode.FREEDOM) {
                this.world.sendEntityStatus(this, (byte) 75);
                this.setMovingMode(MovingMode.TRACER);
            } else {
                this.world.sendEntityStatus(this, (byte) 73);
                this.setMovingMode(MovingMode.FREEDOM);
                this.setFreedomPos(this.getBlockPos());
            }
            return ActionResult.success(this.world.isClient);
        }
        //モブミルク
        if (LMRBMod.getConfig().isCanMilking() && stack.isOf(Items.BUCKET)) {
            player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack2 = ItemUsage.exchangeStack(stack, player, Items.MILK_BUCKET.getDefaultStack());
            player.setStackInHand(hand, itemStack2);
            return ActionResult.success(this.world.isClient);
        }
        openInventory(player);
        return ActionResult.success(this.world.isClient);
    }

    public ActionResult changeState(PlayerEntity player, ItemStack stack) {
        this.world.sendEntityStatus(this, (byte) 72);
        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.random.nextFloat() * 0.1F + 1.0F);
        this.getNavigation().stop();
        this.setWait(!this.isWait());
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.getInventory().removeOne(stack);
            }
        }
        return ActionResult.success(world.isClient);
    }

    public ActionResult contract(PlayerEntity player, ItemStack stack, boolean isReContract) {
        if (!isReContract) {
            this.world.sendEntityStatus(this, (byte) 70);
            if (player instanceof ServerPlayerEntity) {
                LMRBCriteria.CONTRACT_MAID.trigger((ServerPlayerEntity) player, this);
            }
        } else {
            this.world.sendEntityStatus(this, (byte) 71);
        }
        this.setOwnerUuid(player.getUuid());
        setContractMM(true);
        //契約状態の更新
        if (!this.world.isClient) {
            SyncMultiModelPacket.sendS2CPacket(this, this);
        }
        setStrike(false);
        itemContractable.setUnpaidTimes(0);
        getNavigation().stop();
        setMovingMode(MovingMode.ESCORT);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.getInventory().removeOne(stack);
            }
        }
        return ActionResult.success(world.isClient);
    }

    //GUI開くやつ
    public void openInventory(PlayerEntity player) {
        if (player.world.isClient) {
            return;
        }
        setAttacker(null);
        getNavigation().stop();
        setModeName(getMode().map(Mode::getName).orElse(""));
        MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, screenFactory);
    }

    /**
     * 0:wait
     * 1:freedom
     * 2:tracer
     * 3:aiming
     * 4:begging
     * 5:blood suck
     */
    public void setLMMFlag(int index, boolean value) {
        int i = this.dataTracker.get(LMM_FLAGS);
        int mask = (1 << index);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }
        this.dataTracker.set(LMM_FLAGS, (byte) i);
    }

    public boolean getLMMFlag(int index) {
        return (this.dataTracker.get(LMM_FLAGS) & (1 << index)) != 0;
    }

    @Override
    public MovingMode getMovingMode() {
        return MovingMode.fromId(this.dataTracker.get(MOVING_MODE));
    }

    @Override
    public void setMovingMode(MovingMode movingMode) {
        this.dataTracker.set(MOVING_MODE, (byte) movingMode.getId());
    }

    //インベントリ関連

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
    public Iterable<ItemStack> getHandItems() {
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
        //lithium導入下でのバグ修正のためsuperを呼ぶ
        super.equipStack(slot, stack);

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
            ((LMHasInventory.LMInventory) inv).dropAll();
    }

    @Override
    public void setUuid(UUID uuid) {
        super.setUuid(uuid);
        initIdFactor();
    }

    public void initIdFactor() {
        this.idFactor = Math.abs(this.getUuid().hashCode());
    }

    public int getIdFactor() {
        return idFactor;
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
    public boolean isWait() {
        return this.getLMMFlag(WAIT_INDEX);
    }

    @Override
    public void setWait(boolean isWait) {
        this.setLMMFlag(WAIT_INDEX, isWait);
    }

    public void setFreedomPos(@Nullable BlockPos freedomPos) {
        this.freedomPos = freedomPos;
    }

    public Optional<BlockPos> getFreedomPos() {
        if (this.getMovingMode() != MovingMode.FREEDOM) {
            return Optional.empty();
        }
        if (freedomPos == null) {
            freedomPos = this.getBlockPos();
        }
        return Optional.of(freedomPos);
    }

    @Override
    public void setInSittingPose(boolean inSittingPose) {

    }

    @Override
    public boolean isInSittingPose() {
        return this.isWait();
    }

    @Override
    public void setSitting(boolean sitting) {
        this.setWait(sitting);
    }

    @Override
    public boolean isSitting() {
        return this.isWait();
    }

    @Override
    public boolean isTamed() {
        return hasTameOwner();
    }

    public boolean isBegging() {
        return this.getLMMFlag(BEGGING_INDEX);
    }

    public void setBegging(boolean begging) {
        this.setLMMFlag(BEGGING_INDEX, begging);
    }

    public boolean isBloodSuck() {
        return this.getLMMFlag(BLOOD_SUCK_INDEX);
    }

    public void setBloodSuck(boolean isBloodSuck) {
        this.setLMMFlag(BLOOD_SUCK_INDEX, isBloodSuck);
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
    public boolean isContract() {
        return this.hasTameOwner();
    }

    @Override
    public void setContract(boolean isContract) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStrike() {
        return this.getLMMFlag(STRIKE_INDEX);
    }

    @Override
    public void setStrike(boolean strike) {
        itemContractable.setStrike(strike);
        this.setLMMFlag(STRIKE_INDEX, strike);
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
        if (this.isStrike()) {
            return Optional.empty();
        }
        return hasModeImpl.getMode();
    }

    @Override
    public void writeModeData(NbtCompound tag) {
        hasModeImpl.writeModeData(tag);
    }

    @Override
    public void readModeData(NbtCompound tag) {
        hasModeImpl.readModeData(tag);
    }

    public void addMode(Mode mode) {
        hasModeImpl.addMode(mode);
    }

    public void setModeName(String modeName) {
        this.dataTracker.set(MODE_NAME, modeName);
    }

    @Environment(EnvType.CLIENT)
    public Optional<String> getModeName() {
        if (this.isStrike()) {
            //todo ストライキ時の表記
            return Optional.empty();
        }
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
            if (target instanceof net.sistr.littlemaidrebirth.entity.util.Tameable && ownerId.equals(((Tameable) target).getTameOwnerUuid().orElse(null))
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
        return this.getLMMFlag(AIMING_INDEX);
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.setLMMFlag(AIMING_INDEX, aiming);
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
    public void setColorMM(TextureColors textureColor) {
        multiModel.setColorMM(textureColor);
    }

    @Override
    public TextureColors getColorMM() {
        return multiModel.getColorMM();
    }

    @Override
    public void setContractMM(boolean isContract) {
        multiModel.setContractMM(isContract);
    }

    /**
     * マルチモデルの使用テクスチャが契約時のものかどうか
     * ※実際に契約状態かどうかをチェックする場合、
     * {@link #hasTameOwner()}か、
     * {@link #getTameOwnerUuid()}の返り値が存在するかでチェックすること
     */
    @Override
    public boolean isContractMM() {
        return multiModel.isContractMM();
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
                                    LMRBMod.getConfig().getVoiceVolume(), 1.0F, this.getRandom(),
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

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }

    public static class LMMoveToDropItemGoal extends MoveToDropItemGoal {
        protected final LittleMaidEntity maid;

        public LMMoveToDropItemGoal(LittleMaidEntity maid, int range, double speed) {
            super(maid, range, speed);
            this.maid = maid;
        }

        @Override
        public boolean canStart() {
            return (maid.getTameOwner().isPresent()
                    || LMRBMod.getConfig().isCanPickupItemByNoOwner())
                    && !maid.isWait()
                    && ((PlayerInventory) maid.getInventory()).getEmptySlot() != -1
                    && super.canStart();
        }

        @Override
        public List<ItemEntity> findAroundDropItem() {
            return maid.getTameOwner()
                    .filter(owner -> !maid.isWait())
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

    public static class LMStareAtHeldItemGoal<T extends LittleMaidEntity> extends TameableStareAtHeldItemGoal<T> {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(T maid, boolean isTamed, Predicate<ItemStack> targetItem) {
            super(maid, isTamed, targetItem);
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
