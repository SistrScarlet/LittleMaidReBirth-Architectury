package net.sistr.littlemaidrebirth.forge.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.setup.Registration;

@GameTestHolder(LMRBMod.MODID)
@PrefixGameTestTemplate(false)
public class LMRBForgeGameTests {

  @GameTest(templateName = "empty")
  public static void maidSpawn(TestContext context) {
    var maidType = Registration.LITTLE_MAID_MOB.get();
    var pos = new BlockPos(1, 1, 1);
    var maid = context.spawnEntity(maidType, pos);
    context.assertTrue(maid != null, "メイドさんがスポーンできること");
    context.complete();
  }
}
