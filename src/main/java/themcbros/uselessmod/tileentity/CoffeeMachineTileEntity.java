package themcbros.uselessmod.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import themcbros.uselessmod.api.energy.CapabilityUselessEnergy;
import themcbros.uselessmod.config.Config;
import themcbros.uselessmod.container.CoffeeMachineContainer;
import themcbros.uselessmod.energy.UselessEnergyStorage;
import themcbros.uselessmod.helpers.TextUtils;
import themcbros.uselessmod.init.TileEntityInit;
import themcbros.uselessmod.network.Messages;
import themcbros.uselessmod.network.packets.SyncTileEntityPacket;
import themcbros.uselessmod.recipe.CoffeeRecipe;
import themcbros.uselessmod.recipe.RecipeValidator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author TheMCBrothers
 */
public class CoffeeMachineTileEntity extends TileEntity implements ITickableTileEntity, ISidedInventory, INamedContainerProvider, IWrenchableTileEntity, ISyncableTileEntity {

    private final int[] INPUT_SLOTS = {0, 1, 2, 4, 6};
    private final int[] OUTPUT_SLOTS = {3, 5};

    private final IIntArray fields = new IIntArray() {
        @SuppressWarnings("deprecation")
        @Override
        public int get(int index) {
            switch (index) {
                case 0:
                    return CoffeeMachineTileEntity.this.energyStorage.getEnergyStored();
                case 1:
                    return CoffeeMachineTileEntity.this.energyStorage.getMaxEnergyStored();
                case 2:
                    return CoffeeMachineTileEntity.this.waterTank.getFluidAmount();
                case 3:
                    return CoffeeMachineTileEntity.this.waterTank.getCapacity();
                case 4:
                    return Registry.FLUID.getId(CoffeeMachineTileEntity.this.waterTank.getFluid().getFluid());
                case 5:
                    return CoffeeMachineTileEntity.this.isActive() ? 1 : 0;
                case 6:
                    return CoffeeMachineTileEntity.this.getCurrentRecipe() != null ? 1 : 0;
                case 7:
                    return CoffeeMachineTileEntity.this.cookTime;
                case 8:
                    return CoffeeMachineTileEntity.this.cookTimeTotal;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    CoffeeMachineTileEntity.this.energyStorage.setEnergyStored(value);
                    break;
                case 1:
                    CoffeeMachineTileEntity.this.energyStorage.setMaxEnergyStored(value);
                    break;
                case 7:
                    CoffeeMachineTileEntity.this.cookTime = value;
                    break;
                case 8:
                    CoffeeMachineTileEntity.this.cookTimeTotal = value;
                    break;
                default:
                    break;
            }
        }

        @Override
        public int size() {
            return 9;
        }
    };

    public final NonNullList<ItemStack> coffeeStacks = NonNullList.withSize(7, ItemStack.EMPTY);
    public final UselessEnergyStorage energyStorage = new UselessEnergyStorage(Config.SERVER_CONFIG.coffeeMachineEnergyCapacity.get(), 1000, 1000);
    public final FluidTank waterTank = new FluidTank(Config.SERVER_CONFIG.coffeeMachineWaterCapacity.get()) {
        @Override
        protected void onContentsChanged() {
            CoffeeMachineTileEntity.this.sendSyncPacket(0);
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid().isIn(FluidTags.WATER);
        }
    };

    public void sendSyncPacket(int type) {
        if (world == null || world.isRemote) return;

        CompoundNBT nbt = new CompoundNBT();
        if (type == 0) {
            nbt.put("Fluid", this.waterTank.writeToNBT(new CompoundNBT()));
        }

        Messages.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(this.pos)),
                new SyncTileEntityPacket(this, nbt));
    }

    @Override
    public void receiveMessageFromServer(CompoundNBT nbt) {
        if (nbt.contains("Fluid", Constants.NBT.TAG_COMPOUND)) {
            this.waterTank.readFromNBT(nbt.getCompound("Fluid"));
        }
    }

    private int burnTime;
    private int cookTime;
    private int cookTimeTotal;

    public CoffeeMachineTileEntity() {
        super(TileEntityInit.COFFEE_MACHINE.get());
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        ItemStackHelper.saveAllItems(compound, this.coffeeStacks, false);
        compound.putInt("BurnTime", this.burnTime);
        compound.putInt("CookTime", this.cookTime);
        compound.putInt("CookTimeTotal", this.cookTimeTotal);
        compound.put("Fluid", this.waterTank.writeToNBT(new CompoundNBT()));
        compound.putInt("EnergyStored", this.energyStorage.getEnergyStored());
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound);
        ItemStackHelper.loadAllItems(compound, this.coffeeStacks);
        this.burnTime = compound.getInt("BurnTime");
        this.cookTime = compound.getInt("CookTime");
        this.cookTimeTotal = compound.getInt("CookTimeTotal");
        this.waterTank.readFromNBT(compound.getCompound("Fluid"));
        this.energyStorage.setEnergyStored(compound.getInt("EnergyStored"));
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return this.write(new CompoundNBT());
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        assert this.world != null;
        this.read(this.world.getBlockState(pkt.getPos()), pkt.getNbtCompound());
    }

    public boolean isActive() {
        return this.burnTime > 0;
    }

    @Override
    public void tick() {
        assert world != null;
        if (!world.isRemote) {

            if (!this.coffeeStacks.get(4).isEmpty()) {
                ItemStack slotFluidIn = this.coffeeStacks.get(4);
                ItemStack slotFluidOut = this.coffeeStacks.get(5);
                FluidActionResult result = FluidUtil.tryEmptyContainer(slotFluidIn, this.waterTank, this.waterTank.getCapacity(), null, false);
                if (result.isSuccess()) {
                    slotFluidIn.shrink(1);

                    ItemStack resultStack = result.getResult();
                    if (slotFluidOut.isEmpty()) {
                        this.coffeeStacks.set(5, resultStack);
                    } else if (ItemHandlerHelper.canItemStacksStack(resultStack, slotFluidOut)
                            && slotFluidOut.getCount() <= slotFluidOut.getMaxStackSize() - resultStack.getCount()) {
                        this.coffeeStacks.get(5).grow(resultStack.getCount());
                    }
                    this.markDirty();
                    this.requestModelDataUpdate();
                }
            }

            ItemStack energySlotStack = this.coffeeStacks.get(6);
            if (!energySlotStack.isEmpty()) {
                int freeEnergySpace = this.energyStorage.getMaxEnergyStored() - this.energyStorage.getEnergyStored();
                int maxReceive = this.energyStorage.getMaxTransfer(false);
                if (freeEnergySpace > 0) {
                    energySlotStack.getCapability(CapabilityEnergy.ENERGY).ifPresent(itemEnergyStorage -> {
                        if (itemEnergyStorage.canExtract()) {
                            int extracted = itemEnergyStorage.extractEnergy(Math.min(freeEnergySpace, maxReceive), false);
                            this.energyStorage.growEnergy(extracted);
                        }
                    });
                }
            }

        }

        if (this.energyStorage.getEnergyStored() > 0 && this.cookTime > 0) {
            if (this.getCurrentRecipe() != null) {
                this.energyStorage.consumeEnergy(Config.SERVER_CONFIG.coffeeMachineEnergyPerTick.get());
                if (this.cookTime < this.cookTimeTotal && this.getCurrentRecipe() != null) {
                    this.cookTime++;
                } else {
                    this.process(this.getCurrentRecipe());
                    this.cookTime = 0;
                    this.cookTimeTotal = 0;
                }
            } else {
                this.cookTime = 0;
                this.cookTimeTotal = 0;
            }
        } else if (burnTime > 0){
            this.burnTime--;
        }

    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN)
            return OUTPUT_SLOTS;
        return INPUT_SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            if (i == index) return true;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        for (int i = 0; i < OUTPUT_SLOTS.length; i++) {
            if (i == index) return true;
        }
        return false;
    }

    @Override
    public int getSizeInventory() {
        return this.coffeeStacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.coffeeStacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.coffeeStacks.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.coffeeStacks, index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(this.coffeeStacks, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack itemStack = this.coffeeStacks.get(index);
        boolean flag = !stack.isEmpty() && stack.isItemEqual(itemStack) && ItemStack.areItemStackTagsEqual(stack, itemStack);
        this.coffeeStacks.set(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }

        if (!flag) {
            this.markDirty();
        }
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        assert this.world != null;
        if (this.world.getTileEntity(this.pos) != this) {
            return false;
        } else {
            return player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clear() {
        this.coffeeStacks.clear();
    }

    @Override
    public ITextComponent getDisplayName() {
        return TextUtils.translate("container", "coffee_machine");
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory playerInventory, PlayerEntity playerEntity) {
        return new CoffeeMachineContainer(windowId, playerInventory, this);
    }

    private final LazyOptional<IItemHandlerModifiable>[] itemHandlers = SidedInvWrapper.create(this, Direction.values());
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> this.waterTank);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY || cap == CapabilityUselessEnergy.USELESS_ENERGY)
            return LazyOptional.of(() -> this.energyStorage).cast();
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && side != null)
            return itemHandlers[side.getIndex()].cast();
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return fluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void remove() {
        super.remove();
        for (LazyOptional<IItemHandlerModifiable> handler : itemHandlers) {
            handler.invalidate();
        }
        fluidHandler.invalidate();
    }

    public void startMachine(boolean start) {
        assert this.world != null;
        SoundEvent sound = SoundEvents.ITEM_TOTEM_USE;
        if (start) {
            if (getCurrentRecipe() == null) return;
            this.cookTimeTotal = 20 * 3;
            this.cookTime = 1;
            this.burnTime = 20 * 3;
        } else {
            sound = SoundEvents.ITEM_HOE_TILL;
            this.burnTime = 0;
            this.cookTime = 0;
            this.cookTimeTotal = 0;
        }
        this.world.playSound(null, this.pos, sound, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    @Nullable
    private CoffeeRecipe getCurrentRecipe() {
        if (this.world == null) return null;
        for (CoffeeRecipe recipe : RecipeValidator.getCoffeeRecipes(this.world)) {
            boolean emptyIngredient = recipe.getExtraIngredient() == Ingredient.EMPTY;
            boolean flag = recipe.getCupIngredient().test(getStackInSlot(0))
                    && recipe.getBeanIngredient().test(getStackInSlot(1))
                    && (emptyIngredient || recipe.getExtraIngredient().test(getStackInSlot(2)))
                    && this.waterTank.getFluidAmount() >= recipe.getWaterAmount();
            if (this.canProcess(recipe) && flag) return recipe;
        }
        return null;
    }

    private boolean canProcess(@Nullable CoffeeRecipe recipe) {
        if (!this.coffeeStacks.get(0).isEmpty() && !this.coffeeStacks.get(1).isEmpty() && !this.coffeeStacks.get(2).isEmpty() && recipe != null) {
            ItemStack recipeOutput = recipe.getRecipeOutput();
            if (recipeOutput.isEmpty()) {
                return false;
            } else {
                ItemStack outSlotStack = this.coffeeStacks.get(3);
                if (outSlotStack.isEmpty()) {
                    return true;
                } else if (!outSlotStack.isItemEqual(recipeOutput)) {
                    return false;
                } else if (outSlotStack.getCount() + recipeOutput.getCount() <= this.getInventoryStackLimit() && outSlotStack.getCount() + recipeOutput.getCount() <= outSlotStack.getMaxStackSize()) { // Forge fix: make furnace respect stack sizes in furnace recipes
                    return true;
                } else {
                    return outSlotStack.getCount() + recipeOutput.getCount() <= recipeOutput.getMaxStackSize(); // Forge fix: make furnace respect stack sizes in furnace recipes
                }
            }
        } else {
            return false;
        }
    }

    private void process(@Nullable CoffeeRecipe recipe) {
        if (recipe != null && this.canProcess(recipe)) {
            ItemStack inputCup = this.coffeeStacks.get(0);
            ItemStack inputBean = this.coffeeStacks.get(1);
            ItemStack inputExtra = this.coffeeStacks.get(2);
            ItemStack itemstack1 = recipe.getRecipeOutput();
            ItemStack itemstack2 = this.coffeeStacks.get(3);
            if (itemstack2.isEmpty()) {
                this.coffeeStacks.set(3, itemstack1.copy());
            } else if (itemstack2.getItem() == itemstack1.getItem()) {
                itemstack2.grow(itemstack1.getCount());
            }

            inputCup.shrink(1);
            inputBean.shrink(1);

            if (inputExtra.hasContainerItem()) {
                this.coffeeStacks.set(2, inputExtra.getContainerItem());
            } else {
                inputExtra.shrink(1);
            }
            this.waterTank.drain(recipe.getWaterAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public IIntArray getMachineData() {
        return this.fields;
    }
}
