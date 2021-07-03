package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.nbt.CompoundTag;

public abstract class Mode {
    private final ModeType<? extends Mode> modeType;
    private final String name;

    protected Mode(ModeType<? extends Mode> modeType, String name) {
        this.modeType = modeType;
        this.name = name;
    }

    /**
     * モード開始時(切り替わった時)に一度だけ処理
     */
    public void startModeTask() {

    }

    /**
     * 処理を開始すべきか
     */
    abstract public boolean shouldExecute();

    /**
     * 処理を続行すべきか
     */
    abstract public boolean shouldContinueExecuting();

    /**
     * 処理開始時に一回だけ処理
     */
    public void startExecuting() {

    }

    /**
     * 毎tick処理
     */
    public void tick() {

    }

    /**
     * 処理終了時に一回だけ処理
     */
    public void resetTask() {

    }

    /**
     * モード終了時(切り替わった時)に一回だけ処理
     */
    public void endModeTask() {

    }

    /**
     * ワールド保存時に処理
     */
    public void writeModeData(CompoundTag nbt) {

    }

    /**
     * ワールド読み込み時に処理
     */
    public void readModeData(CompoundTag nbt) {

    }

    /**
     * モード名表示用
     */
    public final String getName() {
        return name;
    }

    /**
     * モードタイプ取得
     */
    public final ModeType<? extends Mode> getModeType() {
        return modeType;
    }

}
