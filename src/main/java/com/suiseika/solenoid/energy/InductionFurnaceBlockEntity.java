package com.suiseika.solenoid.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * EMF-powered, fuel-free smelter. Smelts any vanilla {@link RecipeType#SMELTING} recipe in
 * {@link EmfConstants#INDUCTION_FURNACE_SMELT_TICKS} ticks (faster than a vanilla furnace's 200),
 * with no fuel slot — EMF is the fuel. Metal ingot outputs (gated on the {@code c:ingots} tag) have
 * a {@link EmfConstants#INDUCTION_FURNACE_BONUS_CHANCE} chance of a +1 bonus on completion.
 *
 * <p>Energy/progress are synced to the client exactly like the {@link CrusherBlockEntity}: the large
 * ints are split across two 16-bit data slots because the container-data wire format truncates to a
 * signed short.</p>
 */
public class InductionFurnaceBlockEntity extends AbstractEmfBlockEntity implements MenuProvider {
    /** NeoForge common tag for metal ingots; gates the bonus-yield perk. */
    private static final TagKey<Item> INGOTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots"));

    private static class FurnaceItemHandler extends ItemStacksResourceHandler {
        private final InductionFurnaceBlockEntity be;

        FurnaceItemHandler(InductionFurnaceBlockEntity be) {
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

    private final FurnaceItemHandler itemHandler;

    private final SimpleEnergyHandler energyHandler = new SimpleEnergyHandler(
            EmfConstants.INDUCTION_FURNACE_CAPACITY, EmfConstants.INDUCTION_FURNACE_INPUT, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            setChanged();
        }
    };

    private int progress = 0;
    private int maxProgress = EmfConstants.INDUCTION_FURNACE_SMELT_TICKS;

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
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server reads from the block entity; the synced copy lives client-side in the menu.
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(EmfBlocks.INDUCTION_FURNACE_BE.get(), pos, state);
        this.itemHandler = new FurnaceItemHandler(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.solenoid.induction_furnace");
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InductionFurnaceMenu(containerId, playerInventory, this, dataAccess);
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
    protected void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack input = itemHandler.getStack(0);
        if (input.isEmpty()) {
            resetProgress();
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SmeltingRecipe>> recipeOptional =
                level.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, level);

        if (recipeOptional.isEmpty()) {
            resetProgress();
            return;
        }

        ItemStack result = recipeOptional.get().value().assemble(recipeInput);
        if (!canProcess(result)) {
            resetProgress();
            return;
        }

        this.maxProgress = EmfConstants.INDUCTION_FURNACE_SMELT_TICKS;
        int energyPerTick = EmfConstants.INDUCTION_FURNACE_ENERGY_PER_TICK;
        if (energyHandler.getAmountAsInt() < energyPerTick) {
            return;
        }

        energyHandler.set(energyHandler.getAmountAsInt() - energyPerTick);
        progress++;
        if (progress >= maxProgress) {
            processRecipe(level, result);
            progress = 0;
        }
        setChanged();
    }

    /** True if the output slot can accept at least the base result this completion. */
    private boolean canProcess(ItemStack result) {
        ItemStack output = itemHandler.getStack(1);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void processRecipe(ServerLevel level, ItemStack result) {
        ItemStack input = itemHandler.getStack(0);
        input.shrink(1);
        itemHandler.setStack(0, input);

        int produced = result.getCount();
        // Bonus yield: only metal ingots, only when there is room for the extra item.
        ItemStack output = itemHandler.getStack(1);
        int currentCount = output.isEmpty() ? 0 : output.getCount();
        int maxStack = result.getMaxStackSize();
        if (result.is(INGOTS)
                && currentCount + produced + 1 <= maxStack
                && level.getRandom().nextFloat() < EmfConstants.INDUCTION_FURNACE_BONUS_CHANCE) {
            produced += 1;
        }

        if (output.isEmpty()) {
            ItemStack out = result.copy();
            out.setCount(produced);
            itemHandler.setStack(1, out);
        } else {
            output.grow(produced);
            itemHandler.setStack(1, output);
        }
    }

    private void resetProgress() {
        if (progress > 0) {
            progress = 0;
            setChanged();
        }
    }
}
