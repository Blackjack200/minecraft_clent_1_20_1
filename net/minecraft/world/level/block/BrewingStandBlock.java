package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BrewingStandBlock extends BaseEntityBlock {
   public static final BooleanProperty[] HAS_BOTTLE = new BooleanProperty[]{BlockStateProperties.HAS_BOTTLE_0, BlockStateProperties.HAS_BOTTLE_1, BlockStateProperties.HAS_BOTTLE_2};
   protected static final VoxelShape SHAPE = Shapes.or(Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.0D, 15.0D), Block.box(7.0D, 0.0D, 7.0D, 9.0D, 14.0D, 9.0D));

   public BrewingStandBlock(BlockBehaviour.Properties blockbehaviour_properties) {
      super(blockbehaviour_properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(HAS_BOTTLE[0], Boolean.valueOf(false)).setValue(HAS_BOTTLE[1], Boolean.valueOf(false)).setValue(HAS_BOTTLE[2], Boolean.valueOf(false)));
   }

   public RenderShape getRenderShape(BlockState blockstate) {
      return RenderShape.MODEL;
   }

   public BlockEntity newBlockEntity(BlockPos blockpos, BlockState blockstate) {
      return new BrewingStandBlockEntity(blockpos, blockstate);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockstate, BlockEntityType<T> blockentitytype) {
      return level.isClientSide ? null : createTickerHelper(blockentitytype, BlockEntityType.BREWING_STAND, BrewingStandBlockEntity::serverTick);
   }

   public VoxelShape getShape(BlockState blockstate, BlockGetter blockgetter, BlockPos blockpos, CollisionContext collisioncontext) {
      return SHAPE;
   }

   public InteractionResult use(BlockState blockstate, Level level, BlockPos blockpos, Player player, InteractionHand interactionhand, BlockHitResult blockhitresult) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity blockentity = level.getBlockEntity(blockpos);
         if (blockentity instanceof BrewingStandBlockEntity) {
            player.openMenu((BrewingStandBlockEntity)blockentity);
            player.awardStat(Stats.INTERACT_WITH_BREWINGSTAND);
         }

         return InteractionResult.CONSUME;
      }
   }

   public void setPlacedBy(Level level, BlockPos blockpos, BlockState blockstate, LivingEntity livingentity, ItemStack itemstack) {
      if (itemstack.hasCustomHoverName()) {
         BlockEntity blockentity = level.getBlockEntity(blockpos);
         if (blockentity instanceof BrewingStandBlockEntity) {
            ((BrewingStandBlockEntity)blockentity).setCustomName(itemstack.getHoverName());
         }
      }

   }

   public void animateTick(BlockState blockstate, Level level, BlockPos blockpos, RandomSource randomsource) {
      double d0 = (double)blockpos.getX() + 0.4D + (double)randomsource.nextFloat() * 0.2D;
      double d1 = (double)blockpos.getY() + 0.7D + (double)randomsource.nextFloat() * 0.3D;
      double d2 = (double)blockpos.getZ() + 0.4D + (double)randomsource.nextFloat() * 0.2D;
      level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
   }

   public void onRemove(BlockState blockstate, Level level, BlockPos blockpos, BlockState blockstate1, boolean flag) {
      if (!blockstate.is(blockstate1.getBlock())) {
         BlockEntity blockentity = level.getBlockEntity(blockpos);
         if (blockentity instanceof BrewingStandBlockEntity) {
            Containers.dropContents(level, blockpos, (BrewingStandBlockEntity)blockentity);
         }

         super.onRemove(blockstate, level, blockpos, blockstate1, flag);
      }
   }

   public boolean hasAnalogOutputSignal(BlockState blockstate) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockstate, Level level, BlockPos blockpos) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(blockpos));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> statedefinition_builder) {
      statedefinition_builder.add(HAS_BOTTLE[0], HAS_BOTTLE[1], HAS_BOTTLE[2]);
   }

   public boolean isPathfindable(BlockState blockstate, BlockGetter blockgetter, BlockPos blockpos, PathComputationType pathcomputationtype) {
      return false;
   }
}
