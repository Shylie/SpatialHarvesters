package dunkmania101.spatialharvesters.objects.tile_entities;

import com.mojang.authlib.GameProfile;
import dunkmania101.spatialharvesters.data.CommonConfig;
import dunkmania101.spatialharvesters.data.CustomValues;
import dunkmania101.spatialharvesters.init.ItemInit;
import dunkmania101.spatialharvesters.init.TileEntityInit;
import dunkmania101.spatialharvesters.objects.entities.FakeMobEntity;
import dunkmania101.spatialharvesters.util.Tools;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class MobHarvesterTE extends SpatialHarvesterTE {
    private String entity = null;
    private PlayerEntity player = null;
    private CompoundNBT weapon = new CompoundNBT();

    public MobHarvesterTE() {
        super(TileEntityInit.MOB_HARVESTER.get(), new ArrayList<>());
    }

    @Override
    protected void lastMinuteActions() {
        super.lastMinuteActions();
        setEntityDrops();
    }

    protected void setEntityDrops() {
        ArrayList<ItemStack> newOutputs = new ArrayList<>();
        if (this.player != null && this.entity != null) {
            FakeMobEntity fakeMobEntity = getFakeMobEntity();
            if (fakeMobEntity != null) {
                updateWeapon();
                DamageSource playerDamage = DamageSource.causePlayerDamage(this.player);
                fakeMobEntity.publicSpawnDrops(playerDamage);
                newOutputs = fakeMobEntity.getDrops();
                fakeMobEntity.remove();
            }
        }
        setOutputStacks(newOutputs);
    }

    protected FakeMobEntity getFakeMobEntity() {
        FakeMobEntity fakeMobEntity = null;
        if (world != null && this.entity != null) {
            if (world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) world;
                Optional<EntityType<?>> optionalEntityType = EntityType.byKey(this.entity);
                if (optionalEntityType.isPresent()) {
                    EntityType<?> entityType = optionalEntityType.get();
                    ResourceLocation mobRN = entityType.getRegistryName();
                    if (mobRN != null) {
                        ArrayList<ArrayList<String>> blacklist_mobs = CommonConfig.BLACKLIST_MOBS.get();
                        ArrayList<String> blacklist_mobs_mod = CommonConfig.BLACKLIST_MOBS_MOD.get();
                        if (!Tools.isResourceBanned(mobRN, blacklist_mobs, blacklist_mobs_mod)) {
                            Entity entity = entityType.create(serverWorld);
                            if (entity != null) {
                                if (entity instanceof MobEntity) {
                                    MobEntity mobEntity = (MobEntity) entity;
                                    EntityType<? extends MobEntity> mobEntityType = (EntityType<? extends MobEntity>)mobEntity.getType();
                                    fakeMobEntity = new FakeMobEntity(mobEntityType, serverWorld);
                                    fakeMobEntity.onInitialSpawn(serverWorld, serverWorld.getDifficultyForLocation(pos), SpawnReason.NATURAL, null, null);
                                    mobEntity.remove();
                                }
                                entity.remove();
                            }
                        }
                    }
                }
            }
        }
        return fakeMobEntity;
    }

    protected void updateWeapon() {
        if (this.player != null && this.weapon != null) {
            ItemStack stack = ItemStack.read(this.weapon.copy());
            CompoundNBT stackNBT = stack.getTag();
            ItemStack mainHandStack = this.player.getHeldItemMainhand();
            if (!ItemStack.areItemStacksEqual(mainHandStack, stack)) {
                this.player.setHeldItem(Hand.MAIN_HAND, stack);
            }
            if (mainHandStack.getTag() != stackNBT) {
                mainHandStack.deserializeNBT(stackNBT);
            }
            ItemStack offHandStack = this.player.getHeldItemOffhand();
            if (!ItemStack.areItemStacksEqual(offHandStack, stack)) {
                this.player.setHeldItem(Hand.OFF_HAND, stack);
            }
            if (offHandStack.getTag() != stackNBT) {
                offHandStack.deserializeNBT(stackNBT);
            }
        }
    }

    protected void removeAll() {
        removeMobEntity();
        removePlayer();
        removeWeapon();
        this.weapon = null;
    }

    protected void removeMobEntity() {
        this.entity = null;
    }

    protected void removePlayer() {
        if (this.player != null) {
            this.player.remove();
            this.player = null;
        }
    }

    protected void removeWeapon() {
        this.weapon = new CompoundNBT();
    }

    @Override
    public CompoundNBT saveSerializedValues() {
        CompoundNBT nbt = super.saveSerializedValues();
        if (this.entity != null) {
            nbt.putString(CustomValues.entityNBTKey, this.entity);
        }
        if (this.player != null) {
            nbt.putString(CustomValues.playerNameNBTKey, this.player.getName().getString());
        }
        if (this.weapon != null) {
            if (!this.weapon.isEmpty()) {
                nbt.put(CustomValues.weaponNBTKey, this.weapon);
            }
        }
        return nbt;
    }

    @Override
    public void setDeserializedValues(CompoundNBT nbt) {
        super.setDeserializedValues(nbt);
        if (nbt.contains(CustomValues.removeEntityNBTKey)) {
            removeAll();
        }
        if (nbt.contains(CustomValues.entityNBTKey)) {
            removeMobEntity();
            this.entity = nbt.getString(CustomValues.entityNBTKey);
        }
        if (nbt.contains(CustomValues.playerNameNBTKey)) {
            if (world != null) {
                if (world instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    String name = nbt.getString(CustomValues.playerNameNBTKey);
                    if (!StringUtils.isNullOrEmpty(name)) {
                        removePlayer();
                        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
                        this.player = FakePlayerFactory.get(serverWorld, profile);
                        this.player.setSilent(true);
                    }
                }
            }
        }
        if (nbt.contains(CustomValues.weaponNBTKey)) {
            removeWeapon();
            this.weapon = nbt.getCompound(CustomValues.weaponNBTKey);
        }
    }

    @Override
    public void remove() {
        super.remove();
        removeAll();
    }

    @Override
    public int getPrice(Block block) {
        return CommonConfig.MOB_PRICE.get();
    }

    @Override
    public int getSpeed(Block block) {
        return CommonConfig.MOB_SPEED.get();
    }

    @Override
    public Item getShard(Block block) {
        return ItemInit.SHARD_7.get();
    }
}
