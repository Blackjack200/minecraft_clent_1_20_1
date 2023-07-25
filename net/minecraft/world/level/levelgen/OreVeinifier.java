package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class OreVeinifier {
   private static final float VEININESS_THRESHOLD = 0.4F;
   private static final int EDGE_ROUNDOFF_BEGIN = 20;
   private static final double MAX_EDGE_ROUNDOFF = 0.2D;
   private static final float VEIN_SOLIDNESS = 0.7F;
   private static final float MIN_RICHNESS = 0.1F;
   private static final float MAX_RICHNESS = 0.3F;
   private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
   private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
   private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;

   private OreVeinifier() {
   }

   protected static NoiseChunk.BlockStateFiller create(DensityFunction densityfunction, DensityFunction densityfunction1, DensityFunction densityfunction2, PositionalRandomFactory positionalrandomfactory) {
      BlockState blockstate = null;
      return (densityfunction_functioncontext) -> {
         double d0 = densityfunction.compute(densityfunction_functioncontext);
         int i = densityfunction_functioncontext.blockY();
         OreVeinifier.VeinType oreveinifier_veintype = d0 > 0.0D ? OreVeinifier.VeinType.COPPER : OreVeinifier.VeinType.IRON;
         double d1 = Math.abs(d0);
         int j = oreveinifier_veintype.maxY - i;
         int k = i - oreveinifier_veintype.minY;
         if (k >= 0 && j >= 0) {
            int l = Math.min(j, k);
            double d2 = Mth.clampedMap((double)l, 0.0D, 20.0D, -0.2D, 0.0D);
            if (d1 + d2 < (double)0.4F) {
               return blockstate;
            } else {
               RandomSource randomsource = positionalrandomfactory.at(densityfunction_functioncontext.blockX(), i, densityfunction_functioncontext.blockZ());
               if (randomsource.nextFloat() > 0.7F) {
                  return blockstate;
               } else if (densityfunction1.compute(densityfunction_functioncontext) >= 0.0D) {
                  return blockstate;
               } else {
                  double d3 = Mth.clampedMap(d1, (double)0.4F, (double)0.6F, (double)0.1F, (double)0.3F);
                  if ((double)randomsource.nextFloat() < d3 && densityfunction2.compute(densityfunction_functioncontext) > (double)-0.3F) {
                     return randomsource.nextFloat() < 0.02F ? oreveinifier_veintype.rawOreBlock : oreveinifier_veintype.ore;
                  } else {
                     return oreveinifier_veintype.filler;
                  }
               }
            }
         } else {
            return blockstate;
         }
      };
   }

   protected static enum VeinType {
      COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), 0, 50),
      IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -60, -8);

      final BlockState ore;
      final BlockState rawOreBlock;
      final BlockState filler;
      protected final int minY;
      protected final int maxY;

      private VeinType(BlockState blockstate, BlockState blockstate1, BlockState blockstate2, int i, int j) {
         this.ore = blockstate;
         this.rawOreBlock = blockstate1;
         this.filler = blockstate2;
         this.minY = i;
         this.maxY = j;
      }
   }
}