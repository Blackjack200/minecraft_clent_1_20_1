package net.minecraft.world.entity.animal;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.commons.lang3.tuple.Pair;

public class MushroomCow extends Cow implements Shearable, VariantHolder<MushroomCow.MushroomType> {
   private static final EntityDataAccessor<String> DATA_TYPE = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.STRING);
   private static final int MUTATE_CHANCE = 1024;
   @Nullable
   private MobEffect effect;
   private int effectDuration;
   @Nullable
   private UUID lastLightningBoltUUID;

   public MushroomCow(EntityType<? extends MushroomCow> entitytype, Level level) {
      super(entitytype, level);
   }

   public float getWalkTargetValue(BlockPos blockpos, LevelReader levelreader) {
      return levelreader.getBlockState(blockpos.below()).is(Blocks.MYCELIUM) ? 10.0F : levelreader.getPathfindingCostFromLightLevels(blockpos);
   }

   public static boolean checkMushroomSpawnRules(EntityType<MushroomCow> entitytype, LevelAccessor levelaccessor, MobSpawnType mobspawntype, BlockPos blockpos, RandomSource randomsource) {
      return levelaccessor.getBlockState(blockpos.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && isBrightEnoughToSpawn(levelaccessor, blockpos);
   }

   public void thunderHit(ServerLevel serverlevel, LightningBolt lightningbolt) {
      UUID uuid = lightningbolt.getUUID();
      if (!uuid.equals(this.lastLightningBoltUUID)) {
         this.setVariant(this.getVariant() == MushroomCow.MushroomType.RED ? MushroomCow.MushroomType.BROWN : MushroomCow.MushroomType.RED);
         this.lastLightningBoltUUID = uuid;
         this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F);
      }

   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_TYPE, MushroomCow.MushroomType.RED.type);
   }

   public InteractionResult mobInteract(Player player, InteractionHand interactionhand) {
      ItemStack itemstack = player.getItemInHand(interactionhand);
      if (itemstack.is(Items.BOWL) && !this.isBaby()) {
         boolean flag = false;
         ItemStack itemstack1;
         if (this.effect != null) {
            flag = true;
            itemstack1 = new ItemStack(Items.SUSPICIOUS_STEW);
            SuspiciousStewItem.saveMobEffect(itemstack1, this.effect, this.effectDuration);
            this.effect = null;
            this.effectDuration = 0;
         } else {
            itemstack1 = new ItemStack(Items.MUSHROOM_STEW);
         }

         ItemStack itemstack3 = ItemUtils.createFilledResult(itemstack, player, itemstack1, false);
         player.setItemInHand(interactionhand, itemstack3);
         SoundEvent soundevent;
         if (flag) {
            soundevent = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
         } else {
            soundevent = SoundEvents.MOOSHROOM_MILK;
         }

         this.playSound(soundevent, 1.0F, 1.0F);
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (itemstack.is(Items.SHEARS) && this.readyForShearing()) {
         this.shear(SoundSource.PLAYERS);
         this.gameEvent(GameEvent.SHEAR, player);
         if (!this.level().isClientSide) {
            itemstack.hurtAndBreak(1, player, (player1) -> player1.broadcastBreakEvent(interactionhand));
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (this.getVariant() == MushroomCow.MushroomType.BROWN && itemstack.is(ItemTags.SMALL_FLOWERS)) {
         if (this.effect != null) {
            for(int i = 0; i < 2; ++i) {
               this.level().addParticle(ParticleTypes.SMOKE, this.getX() + this.random.nextDouble() / 2.0D, this.getY(0.5D), this.getZ() + this.random.nextDouble() / 2.0D, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
            }
         } else {
            Optional<Pair<MobEffect, Integer>> optional = this.getEffectFromItemStack(itemstack);
            if (!optional.isPresent()) {
               return InteractionResult.PASS;
            }

            Pair<MobEffect, Integer> pair = optional.get();
            if (!player.getAbilities().instabuild) {
               itemstack.shrink(1);
            }

            for(int j = 0; j < 4; ++j) {
               this.level().addParticle(ParticleTypes.EFFECT, this.getX() + this.random.nextDouble() / 2.0D, this.getY(0.5D), this.getZ() + this.random.nextDouble() / 2.0D, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
            }

            this.effect = pair.getLeft();
            this.effectDuration = pair.getRight();
            this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0F, 1.0F);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(player, interactionhand);
      }
   }

   public void shear(SoundSource soundsource) {
      this.level().playSound((Player)null, this, SoundEvents.MOOSHROOM_SHEAR, soundsource, 1.0F, 1.0F);
      if (!this.level().isClientSide()) {
         Cow cow = EntityType.COW.create(this.level());
         if (cow != null) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5D), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            this.discard();
            cow.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            cow.setHealth(this.getHealth());
            cow.yBodyRot = this.yBodyRot;
            if (this.hasCustomName()) {
               cow.setCustomName(this.getCustomName());
               cow.setCustomNameVisible(this.isCustomNameVisible());
            }

            if (this.isPersistenceRequired()) {
               cow.setPersistenceRequired();
            }

            cow.setInvulnerable(this.isInvulnerable());
            this.level().addFreshEntity(cow);

            for(int i = 0; i < 5; ++i) {
               this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(1.0D), this.getZ(), new ItemStack(this.getVariant().blockState.getBlock())));
            }
         }
      }

   }

   public boolean readyForShearing() {
      return this.isAlive() && !this.isBaby();
   }

   public void addAdditionalSaveData(CompoundTag compoundtag) {
      super.addAdditionalSaveData(compoundtag);
      compoundtag.putString("Type", this.getVariant().getSerializedName());
      if (this.effect != null) {
         compoundtag.putInt("EffectId", MobEffect.getId(this.effect));
         compoundtag.putInt("EffectDuration", this.effectDuration);
      }

   }

   public void readAdditionalSaveData(CompoundTag compoundtag) {
      super.readAdditionalSaveData(compoundtag);
      this.setVariant(MushroomCow.MushroomType.byType(compoundtag.getString("Type")));
      if (compoundtag.contains("EffectId", 99)) {
         this.effect = MobEffect.byId(compoundtag.getInt("EffectId"));
      }

      if (compoundtag.contains("EffectDuration", 99)) {
         this.effectDuration = compoundtag.getInt("EffectDuration");
      }

   }

   private Optional<Pair<MobEffect, Integer>> getEffectFromItemStack(ItemStack itemstack) {
      SuspiciousEffectHolder suspiciouseffectholder = SuspiciousEffectHolder.tryGet(itemstack.getItem());
      return suspiciouseffectholder != null ? Optional.of(Pair.of(suspiciouseffectholder.getSuspiciousEffect(), suspiciouseffectholder.getEffectDuration())) : Optional.empty();
   }

   public void setVariant(MushroomCow.MushroomType mushroomcow_mushroomtype) {
      this.entityData.set(DATA_TYPE, mushroomcow_mushroomtype.type);
   }

   public MushroomCow.MushroomType getVariant() {
      return MushroomCow.MushroomType.byType(this.entityData.get(DATA_TYPE));
   }

   @Nullable
   public MushroomCow getBreedOffspring(ServerLevel serverlevel, AgeableMob ageablemob) {
      MushroomCow mushroomcow = EntityType.MOOSHROOM.create(serverlevel);
      if (mushroomcow != null) {
         mushroomcow.setVariant(this.getOffspringType((MushroomCow)ageablemob));
      }

      return mushroomcow;
   }

   private MushroomCow.MushroomType getOffspringType(MushroomCow mushroomcow) {
      MushroomCow.MushroomType mushroomcow_mushroomtype = this.getVariant();
      MushroomCow.MushroomType mushroomcow_mushroomtype1 = mushroomcow.getVariant();
      MushroomCow.MushroomType mushroomcow_mushroomtype2;
      if (mushroomcow_mushroomtype == mushroomcow_mushroomtype1 && this.random.nextInt(1024) == 0) {
         mushroomcow_mushroomtype2 = mushroomcow_mushroomtype == MushroomCow.MushroomType.BROWN ? MushroomCow.MushroomType.RED : MushroomCow.MushroomType.BROWN;
      } else {
         mushroomcow_mushroomtype2 = this.random.nextBoolean() ? mushroomcow_mushroomtype : mushroomcow_mushroomtype1;
      }

      return mushroomcow_mushroomtype2;
   }

   public static enum MushroomType implements StringRepresentable {
      RED("red", Blocks.RED_MUSHROOM.defaultBlockState()),
      BROWN("brown", Blocks.BROWN_MUSHROOM.defaultBlockState());

      public static final StringRepresentable.EnumCodec<MushroomCow.MushroomType> CODEC = StringRepresentable.fromEnum(MushroomCow.MushroomType::values);
      final String type;
      final BlockState blockState;

      private MushroomType(String s, BlockState blockstate) {
         this.type = s;
         this.blockState = blockstate;
      }

      public BlockState getBlockState() {
         return this.blockState;
      }

      public String getSerializedName() {
         return this.type;
      }

      static MushroomCow.MushroomType byType(String s) {
         return CODEC.byName(s, RED);
      }
   }
}
