package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.nbt.NbtCompound;

public interface Mode {

    String MOVE = "move";
    String LOOK = "look";
    String JUMP = "jump";
    String TARGET = "target";

    //モード開始時(切り替わった時)に一度だけ処理
    void startModeTask();

    //処理を開始すべきか
    boolean shouldExecute();

    //処理を続行すべきか
    boolean shouldContinueExecuting();

    //処理開始時に一回だけ処理
    void startExecuting();

    //毎tick処理
    void tick();

    //処理終了時に一回だけ処理
    void resetTask();

    //モード終了時(切り替わった時)に一回だけ処理
    void endModeTask();

    //ワールド保存時に処理
    void writeModeData(NbtCompound nbt);

    //ワールド読み込み時に処理
    void readModeData(NbtCompound nbt);

    //モード判別用
    String getName();

}
