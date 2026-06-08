package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.recipe.SeparatingRecipe;
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

public class SeparatorBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    private static class SeparatorItemHandler extends ItemStacksResourceHandler {
        private final SeparatorBlockEntity be;

        SeparatorItemHandler(SeparatorBlockEntity be) {
            super(3);
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

    private final SeparatorItemHandler itemHandler = new SeparatorItemHandler(this);

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(10000, 1000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** Server-side processing progress (synced to the client for the progress arrow). */
    private int progress = 0;
    private int maxProgress = 200;
    /** Whether the separator currently sits in an active magnetic field (synced for the indicator). */
    private boolean fieldActive = false;

    /**
     * Server-authoritative view of the values the client GUI needs. Large ints (energy, capacity) are
     * split across two 16-bit data slots because the container-data wire format is a signed short
     * (see {@link net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket}). The client
     * reassembles the halves as unsigned in the menu.
     */
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
                case 6 -> fieldActive ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server reads from the block entity; the synced copy lives client-side in the menu.
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public SeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.SEPARATOR_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.separator");
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SeparatorMenu(containerId, playerInventory, this, dataAccess);
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
        ItemStack input = itemHandler.getStack(0);
        if (input.isEmpty()) {
            setFieldActive(false);
            resetProgress();
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SeparatingRecipe>> recipeOptional =
                level.recipeAccess().getRecipeFor(SolenoidRecipes.SEPARATING_TYPE.get(), recipeInput, level);

        if (recipeOptional.isEmpty()) {
            setFieldActive(false);
            resetProgress();
            return;
        }

        SeparatingRecipe recipe = recipeOptional.get().value();
        if (!canProcess(recipe)) {
            setFieldActive(false);
            resetProgress();
            return;
        }

        // Powered + valid recipe -> the separator sits in an active magnetic field.
        this.maxProgress = recipe.time();
        int energyPerTick = Math.max(1, recipe.energy() / recipe.time());
        if (energyHandler.getAmountAsInt() < energyPerTick) {
            setFieldActive(false);
            return;
        }

        setFieldActive(true);
        energyHandler.set(energyHandler.getAmountAsInt() - energyPerTick);
        progress++;
        if (progress >= maxProgress) {
            processRecipe(level, recipe);
            progress = 0;
        }
        setChanged();
    }

    /** True if at least the first output has room. */
    private boolean canProcess(SeparatingRecipe recipe) {
        if (recipe.outputs().isEmpty()) return true;
        ItemStack primary = recipe.outputs().get(0).create();
        ItemStack out = itemHandler.getStack(1);
        if (!out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, primary)) {
                return false;
            }
            if (out.getCount() + primary.getCount() > out.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private void processRecipe(net.minecraft.server.level.ServerLevel level, SeparatingRecipe recipe) {
        ItemStack input = itemHandler.getStack(0);
        input.shrink(1);
        itemHandler.setStack(0, input);

        for (SeparationOutput output : recipe.outputs()) {
            if (level.getRandom().nextFloat() <= output.chance()) {
                ItemStack stack = output.create();
                // Try to put into slot 1 or 2
                for (int slot = 1; slot <= 2; slot++) {
                    ItemStack existing = itemHandler.getStack(slot);
                    if (existing.isEmpty()) {
                        itemHandler.setStack(slot, stack);
                        stack = ItemStack.EMPTY;
                        break;
                    } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                        int grow = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                        if (grow > 0) {
                            existing.grow(grow);
                            itemHandler.setStack(slot, existing);
                            stack.shrink(grow);
                        }
                    }
                    if (stack.isEmpty()) break;
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

    private void setFieldActive(boolean active) {
        if (fieldActive != active) {
            fieldActive = active;
            setChanged();
        }
    }
}
