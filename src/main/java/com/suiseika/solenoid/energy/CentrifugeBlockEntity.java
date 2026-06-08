package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.recipe.CentrifugingRecipe;
import com.suiseika.solenoid.recipe.SeparationOutput;
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

/** Centrifuge: density separation into final dusts. Slot 0 input, slots 1-3 outputs. */
public class CentrifugeBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    private static final int INPUT = 0;
    private static final int OUT_FIRST = 1;
    private static final int OUT_LAST = 3;

    private static class CentrifugeItemHandler extends ItemStacksResourceHandler {
        private final CentrifugeBlockEntity be;

        CentrifugeItemHandler(CentrifugeBlockEntity be) {
            super(4);
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

    private final CentrifugeItemHandler itemHandler = new CentrifugeItemHandler(this);

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(50_000, 1_000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private int progress = 0;
    private int maxProgress = 160;
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
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.CENTRIFUGE_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.centrifuge");
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(containerId, playerInventory, this, dataAccess);
    }

    public ItemStacksResourceHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return energyHandler;
    }

    public ItemStacksResourceHandler getItemHandler(@Nullable Direction side) {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output.child("inventory"));
        energyHandler.serialize(output.child("energy"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input.childOrEmpty("inventory"));
        energyHandler.deserialize(input.childOrEmpty("energy"));
    }

    @Override
    protected void serverTick(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack input = itemHandler.getStack(INPUT);
        if (input.isEmpty()) {
            setWorking(false);
            resetProgress();
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<CentrifugingRecipe>> recipeOptional =
                level.recipeAccess().getRecipeFor(SolenoidRecipes.CENTRIFUGING_TYPE.get(), recipeInput, level);

        if (recipeOptional.isEmpty()) {
            setWorking(false);
            resetProgress();
            return;
        }

        CentrifugingRecipe recipe = recipeOptional.get().value();
        if (!canProcess(recipe)) {
            setWorking(false);
            resetProgress();
            return;
        }

        this.maxProgress = recipe.time();
        int energyPerTick = Math.max(1, recipe.energy() / recipe.time());
        if (energyHandler.getAmountAsInt() < energyPerTick) {
            setWorking(false);
            return;
        }

        setWorking(true);
        energyHandler.set(energyHandler.getAmountAsInt() - energyPerTick);
        progress++;
        if (progress >= maxProgress) {
            processRecipe(level, recipe);
            progress = 0;
        }
        setChanged();
    }

    private boolean canProcess(CentrifugingRecipe recipe) {
        if (recipe.outputs().isEmpty()) return true;
        ItemStack primary = recipe.outputs().get(0).create();
        for (int slot = OUT_FIRST; slot <= OUT_LAST; slot++) {
            ItemStack out = itemHandler.getStack(slot);
            if (out.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(out, primary)
                    && out.getCount() + primary.getCount() <= out.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void processRecipe(net.minecraft.server.level.ServerLevel level, CentrifugingRecipe recipe) {
        ItemStack input = itemHandler.getStack(INPUT);
        input.shrink(1);
        itemHandler.setStack(INPUT, input);

        for (SeparationOutput output : recipe.outputs()) {
            if (level.getRandom().nextFloat() <= output.chance()) {
                insertOutput(output.create());
            }
        }
    }

    private void insertOutput(ItemStack stack) {
        for (int slot = OUT_FIRST; slot <= OUT_LAST; slot++) {
            if (stack.isEmpty()) return;
            ItemStack existing = itemHandler.getStack(slot);
            if (existing.isEmpty()) {
                itemHandler.setStack(slot, stack);
                return;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int grow = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (grow > 0) {
                    existing.grow(grow);
                    itemHandler.setStack(slot, existing);
                    stack.shrink(grow);
                }
            }
        }
    }

    private void resetProgress() {
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
}
