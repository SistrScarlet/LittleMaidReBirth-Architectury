package net.sistr.littlemaidrebirth.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LMRBGameTests implements FabricGameTest {

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void maidSpawn(TestContext context) {
    var maidType = Registration.LITTLE_MAID_MOB.get();
    var pos = new BlockPos(1, 1, 1);
    var maid = context.spawnEntity(maidType, pos);
    context.assertTrue(maid != null, "メイドさんがスポーンできること");
    context.complete();
  }
}
