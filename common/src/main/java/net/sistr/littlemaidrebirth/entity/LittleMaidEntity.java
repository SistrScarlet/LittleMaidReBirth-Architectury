package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import dev.architectury.extensions.network.EntitySpawnExtension;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.sistr.littlemaidmodelloader.entity.compound.IHasMultiModel;
import net.sistr.littlemaidmodelloader.entity.compound.MultiModelCompound;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayableCompound;
import net.sistr.littlemaidmodelloader.maidmodel.IModelCaps;
import net.sistr.littlemaidmodelloader.multimodel.IMultiModel;
import net.sistr.littlemaidmodelloader.multimodel.layer.MMPose;
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
import net.sistr.littlemaidrebirth.entity.mode.HasMode;
import net.sistr.littlemaidrebirth.entity.mode.HasModeImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;
import net.sistr.littlemaidrebirth.entity.util.*;
import net.sistr.littlemaidrebirth.network.SpawnLittleMaidPacket;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.sistr.littlemaidrebirth.util.LMCollidable;
import net.sistr.littlemaidrebirth.util.LMEnchantmentUtil;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

// メイドさん本体
// todo 声タイミング調整
// todo ドロップアイテム
// todo 契約期間の残りは砂糖をあげた時の音符の色で判断してください。
// todo 雪合戦 日が暮れると遊びは終わり
// todo モードトリガーアイテム指定
// todo 署名済みではない書き込み可能な本にパラメータを記述して、メイドさんに右クリックで使用すると値が反映されます。
// todo メイドさんも金リンゴや牛乳を飲めるようになりました。
// todo つまみ食い
// todo ダメージ/水没待機解除 実装済みだっけ？
// todo トランザム
// todo 経験値
// todo 座ったメイドでも追従時に立つように
// todo スト時砂糖ドカ食い
// todo GUIを開いている時に動きを止める
// todo リスポ
// todo 死亡メッセ追加
// todo はしご
// todo おさわり厳禁：他人のメイドに触ると殴られる
// todo 他人のメイドに視線を合わせた時、ご主人の名札を浮かべる
public class LittleMaidEntity extends TameableEntity
        implements EntitySpawnExtension,
                HasInventory,
                Contractable,
                HasMode,
                AimingPoseable,
                IHasMultiModel,
                SoundPlayable,
                HasMovingMode,
                CrossbowUser,
                SalaryBoxPosListener,
                TargetTagManager {
    // LMM_FLAGSのindex
    // todo enumにまとめる
    private static final int WAIT_INDEX = 0;
    private static final int AIMING_INDEX = 1;
    private static final int BEGGING_INDEX = 2;
    private static final int BLOOD_SUCK_INDEX = 3;
    private static final int STRIKE_INDEX = 4;
    private static final int PLAYING_SNOW_INDEX = 5;
    private static final TrackedData<Byte> LMM_FLAGS =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> MOVING_MODE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> MODE_NAME =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> CHARGING =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ACCELERATE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> MASTER_STANCE =
            DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.BYTE);
    // 移譲s
    public final LMHasInventory littleMaidInventory = new LMHasInventory();
    public final LMItemContractable<LittleMaidEntity> itemContractable =
            new LMItemContractable<>(
                    this,
                    () -> getConfig().contract.consumeSalaryInterval,
                    () -> getConfig().contract.unpaidDaysLimit,
                    (ItemStack stack) -> stack.isIn(LMTags.Items.MAIDS_SALARY));
    public final HasModeImpl hasModeImpl =
            new HasModeImpl(
                    this,
                    this,
                    new HashSet<>(),
                    mode -> {
                        setModeName(mode != null ? mode.getName() : "");
                    });
    public final MultiModelCompound multiModel;
    public final SoundPlayableCompound soundPlayer;
    private final LMScreenHandlerFactory screenFactory = new LMScreenHandlerFactory(this);
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private final LMSafeMovement safeMovement =
            new LMSafeMovement(this, LittleMaidEntity::getConfig, this::getDangerHeightThreshold);
    private final TargetTagManager targetTagManager;

    private final Map<MobEntity, Predicate<MobEntity>> fleeEntities =
            new HashMap<>(); // todo クラス化検討
    @Nullable private BlockPos freedomPos;

    // 首傾げのやつ
    @Environment(EnvType.CLIENT)
    private float interestedAngle;

    @Environment(EnvType.CLIENT)
    private float prevInterestedAngle;

    private int playSoundCool;
    private int idFactor;
    public int experiencePickUpDelay;
    // クライアント側のこの値は信用ならない
    private int accelerationTicks;
    private boolean maidManagerRegistered;

    // コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new FixedMoveControl(this);
        ((MobNavigation) getNavigation()).setCanPathThroughDoors(true);
        multiModel =
                new MultiModelCompound(
                        this,
                        LMTextureManager.INSTANCE
                                .getTexture("Default")
                                .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                        LMTextureManager.INSTANCE
                                .getTexture("Default")
                                .orElseThrow(
                                        () -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
        soundPlayer =
                new SoundPlayableCompound(
                        this,
                        () -> multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
        initIdFactor();
        setRandomTexture();
        setRandomVoice();
        this.targetTagManager = new TargetTagManagerImpl(worldIn);
    }

    // 基本使わない
    public LittleMaidEntity(World world) {
        this(Registration.LITTLE_MAID_MOB.get(), world);
    }

    // スタティックなメソッド

    // todo メイドさんに付与する属性の再考
    public static DefaultAttributeContainer.Builder createLittleMaidAttributes() {
        DefaultAttributeContainer.Builder builder =
                createMobAttributes()
                        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                        .add(EntityAttributes.GENERIC_ATTACK_SPEED)
                        .add(EntityAttributes.GENERIC_LUCK)
                        .add(
                                EntityAttributes.GENERIC_FOLLOW_RANGE,
                                LMRBMod.getConfig().target.followRange);
        ReachAttributeUtil.addAttribute(builder);
        return builder;
    }

    public static boolean isValidNaturalSpawn(WorldAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                && world.getBaseLightLevel(pos, 0) > LMRBMod.getConfig().spawn.spawnMinLightLevel;
    }

    // 登録メソッドたち

    @Override
    protected void initGoals() {
        LMGoalInitializer.initGoals(this);
    }

    GoalSelector getGoalSelector() {
        return this.goalSelector;
    }

    GoalSelector getTargetSelector() {
        return this.targetSelector;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LMM_FLAGS, (byte) 0);
        builder.add(MOVING_MODE, (byte) 0);
        builder.add(MODE_NAME, "");
        builder.add(CHARGING, false);
        builder.add(ACCELERATE, false);
        builder.add(MASTER_STANCE, (byte) 0);
    }

    public void addDefaultModes(LittleMaidEntity maid) {
        this.hasModeImpl.addAllMode(ModeManager.INSTANCE.createModes(maid));
    }

    // 読み書き系

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putByte("maidVersion", (byte) 2);

        writeInventory(nbt, this.getRegistryManager());
        nbt.putInt("XpTotal", this.experiencePoints);
        if (TameableUtil.getTameOwnerUuid(this).isPresent()) {
            nbt.putBoolean("Wait", TameableUtil.isWait(this));
            nbt.putByte("MovingMode", (byte) this.getMovingMode().getId());
            writeContractable(nbt);
            writeModeData(nbt);
            nbt.putBoolean("isBloodSuck", isBloodSuck());
            if (this.getMovingMode() == MovingMode.FREEDOM && freedomPos != null) {
                nbt.put("FreedomPos", NbtHelper.fromBlockPos(freedomPos));
            }
            writeTargetTags(nbt);
        }
        this.multiModel.writeToNbt(nbt);
        nbt.putString("SoundConfigName", getConfigHolder().getName());

        nbt.putInt("accelerationTicks", accelerationTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        int maidVersion = nbt.getByte("maidVersion") & 255;

        if (maidVersion <= 1) {
            var defaultAttributes = createLittleMaidAttributes().build();
            var entityAttributes =
                    List.of(
                            EntityAttributes.GENERIC_MOVEMENT_SPEED,
                            EntityAttributes.GENERIC_FOLLOW_RANGE);
            for (var attribute : entityAttributes) {
                var customInstance = this.getAttributes().getCustomInstance(attribute);
                if (customInstance != null) {
                    customInstance.setBaseValue(defaultAttributes.getBaseValue(attribute));
                }
            }
        }

        readInventory(nbt, this.getRegistryManager());
        this.experiencePoints = nbt.getInt("XpTotal");
        if (maidVersion == 0) {
            var list = nbt.getList("Inventory", 10);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound nbtCompound = list.getCompound(i);
                int j = nbtCompound.getByte("Slot") & 255;
                ItemStack stack =
                        ItemStack.fromNbt(this.getRegistryManager(), nbtCompound)
                                .orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    if (j == 0) {
                        this.equipStack(EquipmentSlot.MAINHAND, stack);
                    } else if (100 <= j && j < 104) {
                        EquipmentSlot[] armorSlots = {
                            EquipmentSlot.FEET,
                            EquipmentSlot.LEGS,
                            EquipmentSlot.CHEST,
                            EquipmentSlot.HEAD
                        };
                        this.equipStack(armorSlots[j - 100], stack);
                    } else if (j == 150) {
                        this.equipStack(EquipmentSlot.OFFHAND, stack);
                    }
                }
            }
        }

        if (TameableUtil.hasTameOwner(this)) {
            TameableUtil.setWait(this, nbt.getBoolean("Wait"));
            setMovingMode(MovingMode.fromId(nbt.getByte("MovingMode")));
            readContractable(nbt);
            readModeData(nbt);
            setBloodSuck(nbt.getBoolean("isBloodSuck"));
            if (this.getMovingMode() == MovingMode.FREEDOM && nbt.contains("FreedomPos")) {
                freedomPos = NbtHelper.toBlockPos(nbt, "FreedomPos").orElse(null);
            }
            readTargetTags(nbt);
        }
        this.multiModel.readFromNbt(nbt);
        this.calculateDimensions();
        if (nbt.contains("SoundConfigName")) {
            LMConfigManager.INSTANCE
                    .getConfig(nbt.getString("SoundConfigName"))
                    .ifPresent(this::setConfigHolder);
        }

        accelerationTicks = nbt.getInt("accelerationTicks");
    }

    // todo IdFactorが確実にセットされたタイミングで実行されるようにする
    public void setRandomTexture() {
        var textureHolderList =
                LMTextureManager.INSTANCE.getAllTextures().stream()
                        .filter(h -> h.hasSkinTexture(false)) // 野生テクスチャがある
                        .filter(h -> LMModelManager.INSTANCE.hasModel(h.getModelName()))
                        .toList();
        if (textureHolderList.isEmpty()) {
            return;
        }
        var textureHolder = textureHolderList.get(idFactor % textureHolderList.size());
        var colorList =
                Arrays.stream(TextureColors.values())
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

    public void setRandomVoice() {
        if (getConfig().spawn.silentDefaultVoice) {
            soundPlayer.setConfigHolder(LMConfigManager.EMPTY_CONFIG);
        } else {
            List<ConfigHolder> configs = LMConfigManager.INSTANCE.getAllConfig();
            soundPlayer.setConfigHolder(configs.get(idFactor % configs.size()));
        }
        String defaultSoundPackName = getConfig().spawn.defaultSoundPackName;
        if (!defaultSoundPackName.isEmpty()) {
            LMConfigManager.INSTANCE.getAllConfig().stream()
                    .filter(c -> c.getPackName().equalsIgnoreCase(defaultSoundPackName))
                    .findAny()
                    .ifPresent(soundPlayer::setConfigHolder);
        }
    }

    // 鯖
    @Override
    public void saveAdditionalSpawnData(PacketByteBuf buf) {
        // モデル
        buf.writeEnumConstant(getColorMM());
        buf.writeBoolean(isContractMM());
        buf.writeString(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeString(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeString(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        // サウンド
        buf.writeString(getConfigHolder().getName());
        // 頭の装飾品が表示されない対策
        // 原因はインベントリを開くまで同期されないため
        buf.writeNbt(getInventory().getStack(17).encodeAllowEmpty(this.getRegistryManager()));
        // architectury側のミスでPitchYawが逆に与えられているのを修正
        buf.writeFloat(this.getPitch());
        buf.writeFloat(this.getYaw());
        buf.writeVarInt(this.accelerationTicks);
        // 1.21 では再トラッキング時に DataTracker / 装備の follow-up が確実に届かないため、
        // 描画に必要な状態 (各種フラグ・モード・装備) を spawn data で直接送る。
        buf.writeByte(this.dataTracker.get(LMM_FLAGS));
        buf.writeByte(getMovingMode().getId());
        buf.writeString(this.dataTracker.get(MODE_NAME));
        buf.writeBoolean(this.dataTracker.get(CHARGING));
        buf.writeBoolean(this.dataTracker.get(ACCELERATE));
        buf.writeByte(this.dataTracker.get(MASTER_STANCE));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            buf.writeNbt(getEquippedStack(slot).encodeAllowEmpty(this.getRegistryManager()));
        }
    }

    // 蔵
    @Override
    public void loadAdditionalSpawnData(PacketByteBuf buf) {
        // モデル
        // readString()はクラ処理。このメソッドでは、クラ側なので問題なし
        setColorMM(buf.readEnumConstant(TextureColors.class));
        setContractMM(buf.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager
                .getTexture(buf.readString())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager
                    .getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager
                    .getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
        // サウンド
        LMConfigManager.INSTANCE.getConfig(buf.readString()).ifPresent(this::setConfigHolder);

        getInventory()
                .setStack(
                        17,
                        ItemStack.fromNbt(this.getRegistryManager(), buf.readNbt())
                                .orElse(ItemStack.EMPTY));
        this.setPitch(buf.readFloat());
        this.setYaw(buf.readFloat());
        this.accelerationTicks = buf.readVarInt();
        this.dataTracker.set(LMM_FLAGS, buf.readByte());
        setMovingMode(MovingMode.fromId(buf.readByte()));
        this.dataTracker.set(MODE_NAME, buf.readString());
        this.dataTracker.set(CHARGING, buf.readBoolean());
        this.dataTracker.set(ACCELERATE, buf.readBoolean());
        this.dataTracker.set(MASTER_STANCE, buf.readByte());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipStack(
                    slot,
                    ItemStack.fromNbt(this.getRegistryManager(), buf.readNbt())
                            .orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case 70 -> { // 雇用時
                showEmoteParticle(true);
                play(LMSounds.GET_CAKE);
            }
            case 71 -> { // 再雇用時
                showEmoteParticle(true);
                play(LMSounds.RECONTRACT);
            }
            case 72 -> { // 砂糖あげた時
                this.getWorld()
                        .addParticle(
                                ParticleTypes.NOTE,
                                this.getX(),
                                this.getY() + this.getHeight(),
                                this.getZ(),
                                6 / 24f,
                                0,
                                0);
            }
            case 73 -> showFreedomParticle(); // toFreedom
            case 74 -> showEmoteParticle(false); // toEscort
            case 75 -> showTracerParticle(); // toTracer
            default -> super.handleStatus(status);
        }
    }

    protected void showFreedomParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.getWorld()
                    .addParticle(
                            new DustParticleEffect(
                                    new Vector3f(
                                            this.random.nextFloat(),
                                            this.random.nextFloat(),
                                            this.random.nextFloat()),
                                    1.0f),
                            this.getParticleX(1.0),
                            this.getRandomBodyY() + 0.5,
                            this.getParticleZ(1.0),
                            d,
                            e,
                            f);
        }
    }

    protected void showTracerParticle() {
        for (int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.getWorld()
                    .addParticle(
                            ParticleTypes.CLOUD,
                            this.getParticleX(1.0),
                            this.getRandomBodyY() + 0.5,
                            this.getParticleZ(1.0),
                            d,
                            e,
                            f);
        }
    }

    // バニラメソッズ

    @Override
    public void tick() {
        if (!this.getWorld().isClient() && !this.maidManagerRegistered) {
            TameableUtil.getTameOwner(this)
                    .filter(owner -> owner instanceof MaidManager)
                    .ifPresent(
                            owner -> {
                                ((MaidManager) owner).registerMaid(this);
                                this.maidManagerRegistered = true;
                            });
        }
        int tickMultiple = getTickMultiple();
        for (int i = 0; i < tickMultiple; i++) {
            inTickMultiplePre();
            super.tick();
            inTickMultiplePost();
        }
    }

    protected void inTickMultiplePre() {
        if (this.experiencePickUpDelay > 0) {
            --this.experiencePickUpDelay;
        }
        if (this.getWorld().isClient) {
            tickInterestedAngle();
        }
        playSoundCool = Math.max(0, playSoundCool - 1);
        decAccelerationTicks();
    }

    protected void inTickMultiplePost() {}

    @Override
    public void tickMovement() {
        tickHandSwing();
        super.tickMovement();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (TameableUtil.hasTameOwner(this) || getConfig().misc.canPickupItemByNoOwner) {
            pickupItem();
        }
        itemContractable.tick();
        hasModeImpl.tick();
    }

    protected void pickupItem() {
        if (!getConfig().misc.canPickupExperienceOrb && !getConfig().misc.canPickupItem) {
            return;
        }
        if (this.getHealth() <= 0 || this.isSpectator()) {
            return;
        }
        // 乗り物にライド中の処理は省略
        var aabb = this.getBoundingBox().expand(1.0, 0.5, 1.0);
        var aroundItems = this.getWorld().getOtherEntities(this, aabb);
        var exps = Lists.newArrayList();
        for (Entity entity : aroundItems) {
            if (entity instanceof ExperienceOrbEntity) {
                if (getConfig().misc.canPickupExperienceOrb) {
                    exps.add(entity);
                }
                continue;
            }
            if (!getConfig().misc.canPickupItem) {
                continue;
            }
            if (entity.isRemoved()) continue;
            if (entity instanceof LMCollidable collidable) {
                collidable.onCollision_LMRB(this);
            }
        }
        if (!exps.isEmpty()) {
            var collidable = ((LMCollidable) Util.getRandom(exps, this.random));
            if (collidable != null) {
                collidable.onCollision_LMRB(this);
            }
        }
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return getConfig().spawn.canDespawn && TameableUtil.getTameOwnerUuid(this).isEmpty();
    }

    // canSpawnとかでも使われる
    // todo スポーン条件をコンフィグで設定可能にする
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getBlockState(pos.down()).isFullCube(world, pos)
                ? 10.0F
                : world.getPhototaxisFavor(pos);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target)
                && !TameableUtil.isFriend(this, target)
                && !getTargetTag(new TargetIdentifier(target))
                        .contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED);
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    // 1.21 で getHeightOffset が削除されたため、乗客として乗り物に乗る際の搭乗オフセットを
    // getVehicleAttachmentPos で補正する（乗り物側の搭乗位置にこの値が加算される）。
    // モデル連動式 (getyOffset - getHeight) は pose 依存で実質ゼロになり効かなかったため、
    // ボートで約0.2m高い分を直値で下げる。(プレイヤー肩車は MixinPlayerEntity 側で別途補正済み)
    @Override
    public Vec3d getVehicleAttachmentPos(Entity vehicle) {
        return new Vec3d(0.0, -0.2, 0.0);
    }

    // todo メイドさん自身が乗り物になる場合 (getMountedHeightOffset 相当) のモデル連動は未対応。
    //  必要なら EntityType.Builder の passengerAttachments で設定する。

    // このままだとEntityDimensionsが作っては捨てられてを繰り返すのでパフォーマンスはよろしくない
    // …が、そもそもそんなにたくさん呼ばれるメソッドでもない
    @Override
    protected EntityDimensions getBaseDimensions(EntityPose pose) {
        IMultiModel model =
                getModel(Layer.SKIN, Part.HEAD).orElse(LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps(), MMPose.convertPose(pose));
        float width = model.getWidth(getCaps(), MMPose.convertPose(pose));
        return EntityDimensions.changing(width, height);
    }

    @Nullable
    @Override
    public Entity teleportTo(TeleportTarget teleportTarget) {
        // ディメンション移動の時に、自由行動地点を削除する
        Entity entity = super.teleportTo(teleportTarget);
        if (entity == null) return null;
        if (entity instanceof LittleMaidEntity && this.getMovingMode() == MovingMode.FREEDOM) {
            ((LittleMaidEntity) entity).setFreedomPos(null);
        }
        return entity;
    }

    // todo これ何のメソッド？
    @Override
    public boolean isInWalkTargetRange(BlockPos pos) {
        // 自身または主人から16ブロック以内
        if (pos.isWithinDistance(pos, 16)
                || TameableUtil.getTameOwner(this)
                        .filter(owner -> owner.getBlockPos().isWithinDistance(pos, 16))
                        .isPresent()) {
            return super.isInWalkTargetRange(pos);
        }
        return false;
    }

    // todo ボイス周りの調整、コンフィグ化
    @Override
    public void playAmbientSound() {
        if (this.getWorld().isClient
                || this.dead
                || getConfigHolder()
                                .getParameter("LivingVoiceRate")
                                .map(
                                        s -> {
                                            try {
                                                return Float.parseFloat(s);
                                            } catch (Exception e) {
                                                return null;
                                            }
                                        })
                                .orElse(0.2f)
                        < random.nextFloat()) {
            return;
        }
        if (getHealth() / getMaxHealth() < 0.3F) {
            play(LMSounds.LIVING_WHINE);
        } else {
            if (age % 4 == 0 && this.getWorld().isSkyVisible(this.getBlockPos())) {
                Biome biome = this.getWorld().getBiome(getBlockPos()).value();
                if (biome.isCold(getBlockPos())) {
                    play(LMSounds.LIVING_COLD);
                } else if (2 <= biome.getTemperature()) {
                    play(LMSounds.LIVING_HOT);
                }
            } else if (age % 4 == 1 && this.getWorld().isRaining()) {
                var pos = getBlockPos();
                Biome biome = this.getWorld().getBiome(pos).value();
                if (biome.getPrecipitation(pos) == Biome.Precipitation.RAIN) {
                    play(LMSounds.LIVING_RAIN);
                } else if (biome.getPrecipitation(pos) == Biome.Precipitation.SNOW) {
                    play(LMSounds.LIVING_SNOW);
                }
            } else {
                if (this.getMainHandStack().getItem() == Items.CLOCK
                        || this.getOffHandStack().getItem() == Items.CLOCK) {
                    int time = (int) (this.getWorld().getTimeOfDay() % 24000);
                    // 時間約23500-1500はse_living_morning
                    // 時間約12500-23500はse_living_night
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
        super.onDeath(source);
        // todo 強制再生メソッドを生やす
        // 死亡ボイスは必ず聞かせる
        this.playSoundCool = 0;
        play(LMSounds.DEATH);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.getWorld() instanceof ServerWorld serverWorld && reason.shouldDestroy()) {
            TameableUtil.getTameOwnerUuid(this)
                    .ifPresent(
                            id -> {
                                var maidSoulEntity =
                                        new MaidSoulEntity(serverWorld, new MaidSoul(this));
                                maidSoulEntity.setPosition(this.getX(), this.getY(), this.getZ());
                                maidSoulEntity.setVelocity(
                                        new Vec3d(
                                                random.nextGaussian() * 0.02,
                                                0.2,
                                                random.nextGaussian() * 0.02));
                                serverWorld.spawnEntity(maidSoulEntity);
                            });
        }
    }

    public void installMaidSoul(MaidSoul maidSoul) {
        readNbt(maidSoul.getNbt());
        this.setHealth(getMaxHealth());
        this.unsetRemoved();
        this.dead = false;
        this.deathTime = 0;
    }

    // todo 処理の改善
    @Override
    public boolean tryAttack(Entity target) {
        boolean result = super.tryAttack(target);
        if (this.isBloodSuck()) {
            this.play(LMSounds.ATTACK_BLOOD_SUCK);
        } else {
            this.play(LMSounds.ATTACK);
        }
        // PlayerEntity の attack 処理に倣い、命中後処理と武器の耐久消費を行う
        if (result) {
            ItemStack mainHandStack = this.getMainHandStack();
            Entity entity = target;
            if (target instanceof EnderDragonPart) {
                entity = ((EnderDragonPart) target).owner;
            }
            if (!this.getWorld().isClient
                    && !mainHandStack.isEmpty()
                    && entity instanceof LivingEntity) {
                // バニラではこのメソッドの第三引数にはプレイヤーエンティティしか渡されない
                // そのため、他Modにおいて必ずプレイヤーであると仮定して実装した場合にクラッシュする可能性がある
                // その対策にtry/catchを置いておく
                try {
                    mainHandStack.getItem().postHit(mainHandStack, (LivingEntity) entity, this);
                    // 1.21 で武器の耐久消費は postHit から postDamageEntity へ移された
                    mainHandStack
                            .getItem()
                            .postDamageEntity(mainHandStack, (LivingEntity) entity, this);
                } catch (Exception e) {
                    LMRBMod.LOGGER.error("メイドさんの攻撃時に例外が発生しました。", e);
                }
                if (mainHandStack.isEmpty()) {
                    this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
        }
        return result;
    }

    // todo 処理の見直し
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.dead) {
            return super.damage(source, amount);
        }
        if (!this.getWorld().isClient) {
            // 味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getSource() instanceof SnowballEntity) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        LMRBConfig config = getConfig();
        if (config.health.nonMobDamageImmunity && source.getAttacker() == null) {
            return false;
        }
        if (config.health.immortal
                && !source.isOf(DamageTypes.OUT_OF_WORLD)
                && !source.isSourceCreativePlayer()) {
            return false;
        }
        if (config.health.fallImmunity && source.isOf(DamageTypes.FALL)) {
            return false;
        }
        Entity attacker = source.getAttacker();
        // Friendからの攻撃を除外
        if (!config.health.enableFriendlyFire
                && attacker instanceof LivingEntity
                && TameableUtil.isFriend(this, (LivingEntity) attacker)) {
            return false;
        }
        // 攻撃禁止対象からのダメージを除外
        if (config.health.blockDamageFromAttackProhibited
                && attacker instanceof LivingEntity
                && getTargetTag(new TargetIdentifier((LivingEntity) attacker))
                        .contains(TargetingSystem.TargetTag.ATTACK_PROHIBITED)) {
            return false;
        }

        float factor = config.health.generalMaidDamageFactor;
        if ((config.health.enableWorkInEmergency || !isEmergency())
                && !TameableUtil.isWait(this)
                && this.getMode().map(Mode::isBattleMode).orElse(false)) {
            factor *= config.health.battleModeMaidDamageFactor;
        } else {
            factor *= config.health.nonBattleModeMaidDamageFactor;
        }
        amount *= factor;

        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.damage(source, amount);
        if (!this.getWorld().isClient && !isHurtTime) {
            if (result
                    && 0 < amount
                    && TameableUtil.isWait(this)
                    && TameableUtil.getTameOwnerUuid(this).isPresent()) {
                TameableUtil.setWait(this, false);
            }
            if (!result || amount <= 0F) {
                play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && this.blockedByShield(source)) {
                play(LMSounds.HURT_GUARD);
            } else if (source.isOf(DamageTypes.FALL)) {
                play(LMSounds.HURT_FALL);
            } else if (source.getType().effects() == DamageEffects.BURNING) {
                play(LMSounds.HURT_FIRE);
            } else {
                play(LMSounds.HURT);
            }
        }
        return result;
    }

    public boolean isEmergency() {
        LMRBConfig config = getConfig();
        // 危機閾値以下の体力の場合、危機状態とする
        return this.getHealth() / this.getMaxHealth() <= config.health.emergencyMaidHealthThreshold;
    }

    @Override
    public void setHealth(float health) {
        LMRBConfig config = getConfig();
        if (config.health.disableMaidDeath && health <= 0) {
            super.setHealth(1);
            return;
        }
        super.setHealth(health);
    }

    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        if (isBloodSuck()) play(LMSounds.LAUGHTER);

        return super.onKilledOther(world, other);
    }

    // 射撃

    // todo try/catchを挟む。処理の見直し
    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        var stack = this.getMainHandStack();
        // 弾が無い場合は実行されないはずだが、念のためチェック
        var arrowStack = this.getProjectileType(stack);
        RegistryEntry<Enchantment> infinity =
                LMEnchantmentUtil.entry(this.getWorld(), Enchantments.INFINITY);
        boolean isInfinite = EnchantmentHelper.getLevel(infinity, stack) >= 1;
        if (arrowStack.isEmpty() && !isInfinite) {
            return;
        }
        if (stack.getItem() instanceof BowItem bowItem) {
            var arrow = ProjectileUtil.createArrowProjectile(this, arrowStack, pullProgress, stack);
            if (arrowStack.getItem() instanceof ArrowItem && !isInfinite) {
                arrow.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
            }
            arrow = EPEntityUtil.arrowCustomHook(bowItem, arrow);
            double xDiff = target.getX() - this.getX();
            double yDiff = target.getEyeY() - arrow.getY();
            double zDiff = target.getZ() - this.getZ();
            double horizonLen = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            arrow.setVelocity(
                    xDiff,
                    yDiff + horizonLen * 0.025,
                    zDiff,
                    pullProgress * 3.0f * getConfig().work.archerShootVelocityFactor,
                    14 - 2 * 4);
            this.playSound(
                    SoundEvents.ENTITY_ARROW_SHOOT,
                    1.0f,
                    1.0f / (this.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);
            this.getWorld().spawnEntity(arrow);
            arrowStack.decrement(1);
        } else if (stack.getItem() instanceof CrossbowItem) {
            // CrossbowUser#shoot の第1引数は射手 (クロスボウ保持者)、第2引数は弾速、狙いは getTarget()。
            // バニラ Mob と同じ基準速度 1.6F に archerShootVelocityFactor を掛ける (弓と同様)。
            this.shoot(this, 1.6F * getConfig().work.archerShootVelocityFactor);
        }
    }

    // クロスボウ

    public boolean isCharging() {
        return this.dataTracker.get(CHARGING);
    }

    @Override
    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }

    // 弾速は archerShootVelocityFactor で調整するが、弓のような距離補正アーク
    // (shootAt の horizonLen 補正) はバニラ shootAll に委譲しているため未適用。
    // クロスボウは平射のため実用上は問題になりにくい。

    @Override
    public void postShoot() {}

    @Override
    protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
        if (type != MovementType.SELF && type != MovementType.PLAYER) {
            return movement;
        }
        return safeMovement.adjust(movement);
    }

    // マイナスの値も返すことを利用しているため、バージョンアップ/mixinでの仕様変更に注意が必要
    private float getDangerHeightThreshold() {
        int fallDamage = computeFallDamage(0, 1);
        return -fallDamage;
    }

    // todo 複数モデルで問題ないかチェック
    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0.0, this.getStandingEyeHeight() - 0.15f, 1f / 16f);
    }

    // todo 処理の見直し、処理を追加可能に
    // todo 使用アイテムをコンフィグから追加可能に
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return LMInteractionHandler.handle(this, player, hand);
    }

    int getExperiencePoints() {
        return this.experiencePoints;
    }

    public void addExperience(int experience) {
        this.experiencePoints =
                MathHelper.clamp(this.experiencePoints + experience, 0, Integer.MAX_VALUE);
    }

    // GUI開くやつ
    public void openInventory(PlayerEntity player) {
        if (player.getWorld().isClient) {
            return;
        }
        setAttacker(null);
        getNavigation().stop();
        MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, screenFactory);
    }

    /** 0:wait 1:freedom 2:tracer 3:aiming 4:begging 5:blood suck */
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

    // Flee

    Map<MobEntity, Predicate<MobEntity>> getFleeEntities() {
        return this.fleeEntities;
    }

    public void addFleeEntity(MobEntity entity, Predicate<MobEntity> removePredicate) {
        this.fleeEntities.put(entity, removePredicate);
    }

    // インベントリ関連

    @Override
    public Inventory getInventory() {
        return this.littleMaidInventory.getInventory();
    }

    @Override
    public void writeInventory(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        this.littleMaidInventory.writeInventory(tag, lookup);
    }

    @Override
    public void readInventory(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        this.littleMaidInventory.readInventory(tag, lookup);
    }

    public int getWorkItemSlotSize() {
        return this.littleMaidInventory.getWorkItemSlotSize();
    }

    public void setWorkItemSlotNum(int num) {
        this.littleMaidInventory.setWorkItemSlotSize(num);
    }

    // todo 計算式の見直し
    @Override
    protected void damageArmor(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            int i = -1;
            EquipmentSlot[] armorSlots = {
                EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
            };
            for (ItemStack stack : this.getArmorItems()) {
                i++;
                if (source.isIn(DamageTypeTags.IS_FIRE)
                                && stack.contains(DataComponentTypes.FIRE_RESISTANT)
                        || !(stack.getItem() instanceof ArmorItem)) {
                    continue;
                }
                stack.damage((int) amount, this, armorSlots[i]);
            }
        }
    }

    @Override
    protected void damageHelmet(DamageSource source, float amount) {
        if (!(amount <= 0.0f)) {
            if ((amount /= 4.0f) < 1.0f) {
                amount = 1.0f;
            }
            var stack = getEquippedStack(EquipmentSlot.HEAD);
            if (source.isIn(DamageTypeTags.IS_FIRE)
                            && stack.contains(DataComponentTypes.FIRE_RESISTANT)
                    || !(stack.getItem() instanceof ArmorItem)) {
                return;
            }
            stack.damage((int) amount, this, EquipmentSlot.HEAD);
        }
    }

    @Override
    protected void damageShield(float amount) {
        // todo ガード実装
    }

    // todo どこで使われるメソッド？
    @Override
    public StackReference getStackReference(int mappedIndex) {
        var inv = getInventory();
        int i = mappedIndex - 200;
        if (0 <= i && i < inv.size()) {
            return StackReference.of(inv, i);
        }
        return super.getStackReference(mappedIndex);
    }

    // todo 処理の見直し
    @Override
    public ItemStack getProjectileType(ItemStack stack) {
        if (!(stack.getItem() instanceof RangedWeaponItem ranged)) {
            return ItemStack.EMPTY;
        }
        Predicate<ItemStack> predicate = ranged.getHeldProjectiles();
        ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
        if (!itemStack.isEmpty()) {
            return EPEntityUtil.arrowCustomHook(this, stack, itemStack);
        }
        predicate = ranged.getProjectiles();
        var inv = getInventory();
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack itemStack2 = inv.getStack(i);
            if (predicate.test(itemStack2)) {
                return EPEntityUtil.arrowCustomHook(this, stack, itemStack2);
            }
        }
        return EPEntityUtil.arrowCustomHook(this, stack, ItemStack.EMPTY);
    }

    // 防具の更新
    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        super.equipStack(slot, stack);

        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            multiModel.updateArmor();
        }
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        // dropInventoryで捨てるので不要
        // 実装的に、こちらはランダムドロップに使うもの
    }

    @Override
    protected void dropInventory() {
        Inventory inv = this.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()
                    || EnchantmentHelper.hasAnyEnchantmentsWith(
                            stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP))
                continue;
            this.dropStack(stack);
            inv.setStack(i, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getEquippedStack(slot);
            if (stack.isEmpty()
                    || EnchantmentHelper.hasAnyEnchantmentsWith(
                            stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP))
                continue;
            this.dropStack(stack);
            this.equipStack(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public int getXpToDrop() {
        return this.experiencePoints;
    }

    // todo IdFactorの仕様の見直し
    @Override
    public void setUuid(UUID uuid) {
        super.setUuid(uuid);
        initIdFactor();
    }

    public void initIdFactor() {
        this.idFactor = this.getUuid().hashCode() & 0x7fffffff;
    }

    public int getIdFactor() {
        return idFactor;
    }

    // テイム関連

    @Override
    public void setOwnerUuid(@Nullable UUID uuid) {
        super.setOwnerUuid(uuid);
        this.setContract(true);
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
    public void setInSittingPose(boolean inSittingPose) {}

    @Override
    public boolean isInSittingPose() {
        return TameableUtil.isWait(this);
    }

    @Override
    public void setSitting(boolean sitting) {
        this.setLMMFlag(WAIT_INDEX, sitting);
    }

    @Override
    public boolean isSitting() {
        return this.getLMMFlag(WAIT_INDEX);
    }

    @Override
    public boolean isTamed() {
        return TameableUtil.getTameOwnerUuid(this).isPresent();
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
        return (prevInterestedAngle + (interestedAngle - prevInterestedAngle) * tickDelta)
                * ((getId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI);
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

    // 　加速機能

    public int getTickMultiple() {
        return this.isAcceleration() ? getConfig().misc.accelerationMultiple : 1;
    }

    public void setAccelerationTicks(int ticks) {
        this.accelerationTicks = ticks;
        if (ticks > 0) {
            this.dataTracker.set(ACCELERATE, true);
        }
    }

    public void decAccelerationTicks() {
        if (this.accelerationTicks > 0) {
            this.accelerationTicks--;
        }
        if (this.accelerationTicks <= 0) {
            this.accelerationTicks = 0;
            this.dataTracker.set(ACCELERATE, false);
        }
    }

    public int getAccelerationTicks() {
        return this.accelerationTicks;
    }

    public boolean isAcceleration() {
        return this.dataTracker.get(ACCELERATE);
    }

    // お給料

    @Override
    public boolean isContract() {
        return TameableUtil.getTameOwnerUuid(this).isPresent();
    }

    @Override
    public void setContract(boolean isContract) {
        itemContractable.setContract(isContract);
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
        if (itemContractable.isStrike()) {
            this.setStrike(true);
        }
    }

    public int getUnpaidDays() {
        return itemContractable.getUnpaidTimes();
    }

    // お給料受け取り

    @Override
    public void listenSalaryBoxPos(BlockPos pos) {
        itemContractable.listenSalaryBoxPos(pos);
    }

    // モード機能

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

    public void addAllMode(Collection<Mode> mode) {
        hasModeImpl.addAllMode(mode);
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

    // TargetTag

    @Override
    public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
        return TameableUtil.getTameOwner(this)
                .map(l -> l instanceof TargetTagManager ? (TargetTagManager) l : null)
                .map(
                        t -> {
                            var otherSync = t.getTargetTagsSync();
                            var thisSync = this.getTargetTagsSync();
                            if (otherSync.hash() != thisSync.hash()) {
                                thisSync.syncFrom(otherSync);
                            }
                            return t;
                        })
                .orElse(this.targetTagManager)
                .getTargetTag(id);
    }

    @Override
    public void writeTargetTags(NbtCompound nbt) {
        this.targetTagManager.writeTargetTags(nbt);
    }

    @Override
    public void readTargetTags(NbtCompound nbt) {
        this.targetTagManager.readTargetTags(nbt);
    }

    @Override
    public Sync getTargetTagsSync() {
        return this.targetTagManager.getTargetTagsSync();
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return canTarget(target);
    }

    // 構え

    @Override
    public boolean isAimingBow() {
        return this.getLMMFlag(AIMING_INDEX);
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.setLMMFlag(AIMING_INDEX, aiming);
    }

    // マルチモデル関連

    @Override
    public boolean isAllowChangeTexture(
            Entity entity, TextureHolder textureHolder, Layer layer, Part part) {
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
     * マルチモデルの使用テクスチャが契約時のものかどうか ※実際に契約状態かどうかをチェックする場合、 {@link
     * TameableUtil#getTameOwnerUuid(Tameable)}がisPresent()かでチェックすること
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

    public boolean isPlayingSnow() {
        return this.getLMMFlag(PLAYING_SNOW_INDEX);
    }

    public void setPlayingSnow(boolean isPlayingSnow) {
        this.setLMMFlag(PLAYING_SNOW_INDEX, isPlayingSnow);
    }

    // 音声関係

    // todo 強制再生メソッドを生やす
    // todo 再生クールダウンをコンフィグ化
    @Override
    public void play(String soundName) {
        if (0 < this.playSoundCool) {
            return;
        }
        this.playSoundCool = getConfig().misc.playSoundInterval;
        if (isBloodSuck()) {
            if (soundName.equals(LMSounds.FIND_TARGET_N)) {
                soundName = LMSounds.FIND_TARGET_B;
            } else if (soundName.equals(LMSounds.ATTACK)) {
                soundName = LMSounds.ATTACK_BLOOD_SUCK;
            }
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
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry trackerEntry) {
        // Architectury 標準のスポーンパケットは生成が遅延し再トラッキング時に follow-up を取りこぼすため、
        // 即時生成する自前パケットを使う（詳細は SpawnLittleMaidPacket 参照）。
        return SpawnLittleMaidPacket.create(this);
    }

    public static LMRBConfig getConfig() {
        return LMRBMod.getConfig();
    }

    // MOVEとLOOKでGoalを分離
    public static class LMStareAtHeldItemGoal<T extends LittleMaidEntity>
            extends TameableStareAtHeldItemGoal<T> {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(
                T mob,
                Supplier<Float> stareAtRange,
                Predicate<ItemStack> targetItem,
                boolean isTamed) {
            super(mob, stareAtRange, targetItem, isTamed);
            this.maid = mob;
        }

        @Override
        public void tick() {
            super.tick();
            // 動いてたら傾げない
            this.maid.setBegging(this.maid.getNavigation().isIdle());
        }

        @Override
        public void stop() {
            super.stop();
            this.maid.setBegging(false);
        }
    }
}
