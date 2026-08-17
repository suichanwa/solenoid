package com.suiseika.solenoid.energy;

import com.suiseika.solenoid.recipe.CrushingRecipe;
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

public class CrusherBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    private static class CrusherItemHandler extends ItemStacksResourceHandler {
        private final CrusherBlockEntity be;

        public CrusherItemHandler(CrusherBlockEntity be) {
            super(2);
            this.be = be;
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            be.setChanged();
        }

        public ItemStack getStack(int slot) {
            return this.stacks.get(slot);
        }

        public void setStack(int slot, ItemStack stack) {
            this.stacks.set(slot, stack);
            onContentsChanged(slot, stack);
        }
    }

    private final CrusherItemHandler itemHandler;

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(10000, 1000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    /** Server-side processing progress (synced to the client for the progress arrow). */
    private int progress = 0;
    private int maxProgress = 200;
    private int currentEnergyUsage = 0;

    /**
     * Server-authoritative view of the values the client GUI needs. Each large int (energy, capacity)
     * is split across two 16-bit data slots because the container-data wire format is a signed short
     * (see {@link net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket}, which
     * truncates with {@code writeShort}). The client reassembles the halves as unsigned in the menu.
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
                case 6 -> sideModes[0].ordinal();
                case 7 -> sideModes[1].ordinal();
                case 8 -> sideModes[2].ordinal();
                case 9 -> sideModes[3].ordinal();
                case 10 -> sideModes[4].ordinal();
                case 11 -> sideModes[5].ordinal();
                case 12 -> autoEject ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 13;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.CRUSHER_BE.get(), pos, state);
        this.itemHandler = new CrusherItemHandler(this);
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
        return Component.translatable("container.solenoid.crusher");
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this, dataAccess);
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
            resetProgress();
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<CrushingRecipe>> recipeOptional = level.recipeAccess().getRecipeFor(SolenoidRecipes.CRUSHING_TYPE.get(), recipeInput, level);

        if (recipeOptional.isPresent()) {
            CrushingRecipe recipe = recipeOptional.get().value();
            if (canProcess(recipe)) {
                this.maxProgress = recipe.time();
                int energyPerTick = Math.max(1, recipe.energy() / recipe.time());
                this.currentEnergyUsage = energyPerTick;
                if (energyHandler.getAmountAsInt() >= energyPerTick) {
                    energyHandler.set(energyHandler.getAmountAsInt() - energyPerTick);
                    progress++;
                    if (progress >= maxProgress) {
                        processRecipe(recipe);
                        progress = 0;
                    }
                    setChanged();
                }
            } else {
                resetProgress();
            }
        } else {
            resetProgress();
        }
    }

    private boolean canProcess(CrushingRecipe recipe) {
        ItemStack output = itemHandler.getStack(1);
        if (output.isEmpty()) return true;
        ItemStack resultStack = recipe.result().create();
        if (!ItemStack.isSameItemSameComponents(output, resultStack)) return false;
        return output.getCount() + resultStack.getCount() <= output.getMaxStackSize();
    }

    private void processRecipe(CrushingRecipe recipe) {
        ItemStack input = itemHandler.getStack(0);
        input.shrink(1);
        itemHandler.setStack(0, input);

        ItemStack result = recipe.result().create();
        ItemStack existing = itemHandler.getStack(1);
        if (existing.isEmpty()) {
            itemHandler.setStack(1, result);
        } else {
            existing.grow(result.getCount());
            itemHandler.setStack(1, existing);
        }
    }

    private void resetProgress() {
        this.currentEnergyUsage = 0;
        if (progress > 0) {
            progress = 0;
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
