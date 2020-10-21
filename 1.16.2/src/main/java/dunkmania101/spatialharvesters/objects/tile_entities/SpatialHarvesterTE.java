package dunkmania101.spatialharvesters.objects.tile_entities;

import dunkmania101.spatialharvesters.data.CommonConfig;
import dunkmania101.spatialharvesters.data.CustomValues;
import dunkmania101.spatialharvesters.init.ItemInit;
import dunkmania101.spatialharvesters.objects.blocks.SpaceRipperBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Random;

public class SpatialHarvesterTE extends TickingRedstoneEnergyMachineTE {
    protected ArrayList<ItemStack> OUTPUTS = new ArrayList<>();
    protected ArrayList<Item> BLACKLIST = new ArrayList<>();
    private Block thisBlock = null;

    public SpatialHarvesterTE(TileEntityType<?> tileEntityTypeIn, ArrayList<Item> OUTPUTS) {
        super(tileEntityTypeIn, true, true, true);

        setOutputs(OUTPUTS);
    }

    @Override
    public void tick() {
        this.thisBlock = getBlockState().getBlock();
        updateEnergyStorage();
        super.tick();
    }

    @Override
    public void customTickActions() {
        boolean enableOre = CommonConfig.ENABLE_ORE_HARVESTERS.get();
        boolean enableBio = CommonConfig.ENABLE_ORE_HARVESTERS.get();
        boolean enableStone = CommonConfig.ENABLE_ORE_HARVESTERS.get();
        boolean enableSoil = CommonConfig.ENABLE_ORE_HARVESTERS.get();
        if (this instanceof OreHarvesterTE && !enableOre) {
            setActive(false);
        } else if (this instanceof BioHarvesterTE && !enableBio) {
            setActive(false);
        } else if (this instanceof StoneHarvesterTE && !enableStone) {
            setActive(false);
        } else if (this instanceof SoilHarvesterTE && !enableSoil) {
            setActive(false);
        } else {
            super.customTickActions();
            if (getWorld() != null && !getWorld().isRemote && this.thisBlock != null) {
                if (getCountedTicks() >= getSpeed(this.thisBlock)) {
                    resetCountedTicks();
                    setActive(false);
                    int price = getPrice(this.thisBlock);
                    if (getEnergyStorage().getEnergyStored() >= price) {
                        ArrayList<Direction> spaceRippers = new ArrayList<>();
                        ArrayList<IItemHandler> outInventories = new ArrayList<>();
                        for (Direction side : Direction.values()) {
                            if (getWorld().getBlockState(getPos().offset(side)).getBlock() instanceof SpaceRipperBlock) {
                                spaceRippers.add(side);
                            } else {
                                TileEntity out = getWorld().getTileEntity(getPos().offset(side));
                                if (out != null) {
                                    LazyOptional<IItemHandler> outCap = out.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
                                    outCap.ifPresent(outInventories::add);
                                }
                            }
                        }
                        if (!spaceRippers.isEmpty() && !outInventories.isEmpty()) {
                            lastMinuteActions();
                            if (!this.OUTPUTS.isEmpty()) {
                                for (Direction ignored : spaceRippers) {
                                    if (getEnergyStorage().getEnergyStored() >= price) {
                                        ItemStack chosenOutput;
                                        Random rand = getWorld().getRandom();
                                        int shardChance = CommonConfig.HARVESTER_SHARD_CHANCE.get();
                                        if (rand.nextInt(shardChance) == 1) {
                                            chosenOutput = new ItemStack(getShard(this.thisBlock));
                                        } else {
                                            chosenOutput = this.OUTPUTS.get(rand.nextInt(this.OUTPUTS.size())).copy();
                                        }
                                        if (!chosenOutput.isEmpty()) {
                                            if (this.BLACKLIST.contains(chosenOutput.getItem())) {
                                                getEnergyStorage().consumeEnergy(price);
                                                setActive(true);
                                            } else {
                                                int originalCount = chosenOutput.getCount();
                                                IItemHandler inventory = outInventories.get(rand.nextInt(outInventories.size()));
                                                ItemStack resultStack = ItemHandlerHelper.insertItemStacked(inventory, chosenOutput, false);
                                                if (resultStack.getCount() != originalCount) {
                                                    getEnergyStorage().consumeEnergy(price);
                                                    setActive(true);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void lastMinuteActions() {
    }

    public void setOutputs(ArrayList<Item> OUTPUTS) {
        this.OUTPUTS = new ArrayList<>();
        for (Item item : OUTPUTS) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                this.OUTPUTS.add(stack);
            }
        }
    }

    public void setOutputStacks(ArrayList<ItemStack> OUTPUTS) {
        this.OUTPUTS = OUTPUTS;
    }

    @Override
    protected int getCapacity() {
        if (this.thisBlock != null) {
            int multiplier = CommonConfig.HARVESTER_CAPACITY_MULTIPLIER.get();
            return getPrice(this.thisBlock) * multiplier;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getMaxInput() {
        return getCapacity();
    }

    @Override
    protected int getMaxExtract() {
        return getCapacity();
    }

    public int getPrice(Block block) {
        return Integer.MAX_VALUE;
    }

    public int getSpeed(Block block) {
        return Integer.MAX_VALUE;
    }

    public Item getShard(Block block) {
        return ItemInit.SHARD_1.get();
    }

    @Override
    public CompoundNBT saveSerializedValues() {
        CompoundNBT nbt =  super.saveSerializedValues();
        CompoundNBT disabledResources = new CompoundNBT();
        int i = 0;
        for (Item item : this.BLACKLIST) {
            ResourceLocation rn = item.getRegistryName();
            if (rn != null) {
                disabledResources.putString(Integer.toString(i), rn.toString());
                i++;
            }
        }
        if (!disabledResources.isEmpty()) {
            nbt.put(CustomValues.disabledResourcesKey, disabledResources);
        }
        return nbt;
    }

    @Override
    public void setDeserializedValues(CompoundNBT nbt) {
        super.setDeserializedValues(nbt);
        if (nbt.contains(CustomValues.removeDisabledNBTKey)) {
            this.BLACKLIST = new ArrayList<>();
        }
        if (nbt.contains(CustomValues.disabledResourcesKey)) {
            this.BLACKLIST = new ArrayList<>();
            CompoundNBT disabledResources = nbt.getCompound(CustomValues.disabledResourcesKey);
            for (String key : disabledResources.keySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(disabledResources.getString(key)));
                if (item != null && item != Items.AIR) {
                    this.BLACKLIST.add(item);
                }
            }
        }
        if (nbt.contains(CustomValues.disabledResourceKey)) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(nbt.getString(CustomValues.disabledResourceKey)));
            if (item != null && item != Items.AIR) {
                this.BLACKLIST.add(item);
            }
        }
    }
}
