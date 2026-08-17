package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.recipe.ReactingRecipe;
import com.suiseika.solenoid.recipe.SolenoidRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Chemical Reactor: produces reagents (e.g. sawdust x4 -> lye). Slot 0 input, slot 1 output. */
public class ChemicalReactorBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    private static class ReactorItemHandler extends ItemStacksResourceHandler {
        private final ChemicalReactorBlockEntity be;

        ReactorItemHandler(ChemicalReactorBlockEntity be) {
            super(2);
            this.be = be;
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            be.setChanged();
        }

        ItemStack getStack(int slot) {
            return this.stacks.get(slot);
        }

        void setStack(int slot, ItemStack stack) {
            this.stacks.set(slot, stack);
            onContentsChanged(slot, stack);
        }
    }

    private final ReactorItemHandler itemHandler = new ReactorItemHandler(this);

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(20_000, 1_000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private int progress = 0;
    private int maxProgress = 120;
    private int currentEnergyUsage = 0;
    private boolean working = false;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            int energy = energyHandler.getAmountAsInt();
            int capacity = energyHandler.getCapacityAsInt();
            return switch (index) {
                case 0 -> energy & 0xFFFF;
                case 1 -> (energy >>> 16) & 0xFFFF;
                case 2 -> capacity & 0xFFFF;
                case 3 -> (capacity >>> 16) & 0xFFFF;
                case 4 -> progress;
                case 5 -> maxProgress;
                case 6 -> working ? 1 : 0;
                case 7 -> sideModes[0].ordinal();
                case 8 -> sideModes[1].ordinal();
                case 9 -> sideModes[2].ordinal();
                case 10 -> sideModes[3].ordinal();
                case 11 -> sideModes[4].ordinal();
                case 12 -> sideModes[5].ordinal();
                case 13 -> autoEject ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 14;
        }
    };

    public ChemicalReactorBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.CHEMICAL_REACTOR_BE.get(), pos, state);
    }

    @Override
    public int[] getInputSlots() {
        return new int[]{0};
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{1};
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.chemical_reactor");
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChemicalReactorMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyHandler;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("inventory"));
        energyHandler.serialize(output.child("energy"));
        saveSideConfig(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input.childOrEmpty("inventory"));
        energyHandler.deserialize(input.childOrEmpty("energy"));
        loadSideConfig(input);
    }

    @Override
    protected void serverTick(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state) {
        autoEjectOutputs(level, pos);
        ItemStack input = itemHandler.getStack(0);
        if (input.isEmpty()) {
            setWorking(false);
            resetProgress();
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<ReactingRecipe>> recipeOptional =
                level.recipeAccess().getRecipeFor(SolenoidRecipes.REACTING_TYPE.get(), recipeInput, level);

        if (recipeOptional.isEmpty()) {
            setWorking(false);
            resetProgress();
            return;
        }

        ReactingRecipe recipe = recipeOptional.get().value();
        if (input.getCount() < recipe.inputCount() || !canProcess(recipe)) {
            setWorking(false);
            resetProgress();
            return;
        }

        this.maxProgress = recipe.time();
        int energyPerTick = Math.max(1, recipe.energy() / recipe.time());
        this.currentEnergyUsage = energyPerTick;
        if (energyHandler.getAmountAsInt() < energyPerTick) {
            setWorking(false);
            return;
        }

        setWorking(true);
        energyHandler.set(energyHandler.getAmountAsInt() - energyPerTick);
        progress++;
        if (progress >= maxProgress) {
            processRecipe(recipe);
            progress = 0;
        }
        setChanged();
    }

    private boolean canProcess(ReactingRecipe recipe) {
        ItemStack result = recipe.result().create();
        ItemStack out = itemHandler.getStack(1);
        if (out.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(out, result)) {
            return false;
        }
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void processRecipe(ReactingRecipe recipe) {
        ItemStack input = itemHandler.getStack(0);
        input.shrink(recipe.inputCount());
        itemHandler.setStack(0, input);

        ItemStack result = recipe.result().create();
        ItemStack out = itemHandler.getStack(1);
        if (out.isEmpty()) {
            itemHandler.setStack(1, result);
        } else {
            out.grow(result.getCount());
            itemHandler.setStack(1, out);
        }
    }

    private void resetProgress() {
        this.currentEnergyUsage = 0;
        if (progress > 0) {
            progress = 0;
            setChanged();
        }
    }

    private void setWorking(boolean value) {
        if (working != value) {
            working = value;
            setChanged();
        }
    }

    @Override
    public int getEnergyUsage() {
        return progress > 0 ? currentEnergyUsage : 0;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getMaxProgress() {
        return maxProgress;
    }
}
