package com.suiseika.solenoid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Solenoid.MODID)
public class ForceFieldGeneratorItem extends EmfPoweredItem implements ICurioItem {
    public static final int CAPACITY = 50_000;
    public static final int MAX_TRANSFER = 2_000;
    /** EMF cost per 1.0 damage absorbed (200 per full heart). */
    public static final int COST_PER_DAMAGE = 200;

    public ForceFieldGeneratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public boolean hasCurioCapability(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean active = !isActive(stack);
            stack.set(SolenoidDataComponents.ACTIVE.get(), active);
            serverPlayer.sendSystemMessage(Component.translatable("message.solenoid.force_field.toggle",
                    active ? Component.translatable("message.solenoid.on").withStyle(ChatFormatting.GREEN)
                           : Component.translatable("message.solenoid.off").withStyle(ChatFormatting.RED)), true);
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack generatorStack = findGenerator(player);
        if (generatorStack.isEmpty() || !(generatorStack.getItem() instanceof ForceFieldGeneratorItem generator)) return;
        if (!isActive(generatorStack)) return;

        int energy = generator.getEnergy(generatorStack);
        int cost = (int) Math.ceil(event.getAmount() * COST_PER_DAMAGE);

        if (energy >= cost) {
            event.setAmount(0f);
            generator.setEnergy(generatorStack, energy - cost);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        30, 0.5, 0.5, 0.5, 0.2);
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.5f);
            }
        } else {
            generatorStack.set(SolenoidDataComponents.ACTIVE.get(), false);
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(
                        Component.translatable("message.solenoid.force_field.depleted")
                                .withStyle(ChatFormatting.RED), true);
            }
        }
    }

    private static ItemStack findGenerator(Player player) {
        var opt = CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(MagnetiteItems.FORCE_FIELD_GENERATOR.get()));
        if (opt.isPresent()) return opt.get().stack();
        if (player.getMainHandItem().is(MagnetiteItems.FORCE_FIELD_GENERATOR.get())) return player.getMainHandItem();
        if (player.getOffhandItem().is(MagnetiteItems.FORCE_FIELD_GENERATOR.get())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    private static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(SolenoidDataComponents.ACTIVE.get(), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        boolean active = isActive(stack);
        tooltip.accept(Component.translatable("tooltip.solenoid.force_field.state",
                active ? Component.translatable("message.solenoid.on").withStyle(ChatFormatting.GREEN)
                       : Component.translatable("message.solenoid.off").withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.solenoid.energy_stored", getEnergy(stack), getCapacity())
                .withStyle(ChatFormatting.AQUA));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(
                        access, SolenoidDataComponents.EMF_ENERGY.get(), CAPACITY, MAX_TRANSFER),
                MagnetiteItems.FORCE_FIELD_GENERATOR.get());
    }
}
