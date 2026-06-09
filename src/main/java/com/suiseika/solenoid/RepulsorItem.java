package com.suiseika.solenoid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

/**
 * Rechargeable neodymium repulsor. Right-click emits a single magnetic repulsion pulse: every nearby
 * {@link LivingEntity} (except the user) is flung outward with a small upward pop, and every nearby
 * {@link Projectile} is batted away and re-owned to the user so it can no longer hit them. Each pulse
 * costs {@link #PULSE_COST} EMF and applies a {@link #COOLDOWN}-tick cooldown; with insufficient EMF
 * the right-click only fizzles (no pulse, no cooldown).
 *
 * <p>Energy reuses the shared {@link SolenoidDataComponents#EMF_ENERGY} buffer exposed as a
 * Forge-Energy item handler, so it charges in the Capacitor like the batteries/charm. The pulse runs
 * server-authoritatively -- {@link #use} only mutates state when the player is on the server.
 * Also wearable in a Curios {@code charm} slot.
 */
@EventBusSubscriber(modid = Solenoid.MODID)
public class RepulsorItem extends EmfPoweredItem implements ICurioItem {
    public static final int CAPACITY = 30_000;
    public static final int MAX_TRANSFER = 1_000;
    public static final double RADIUS = 6.0;
    public static final double STRENGTH = 1.8;
    public static final double DEFLECT_SPEED = 1.5;
    public static final int PULSE_COST = 500;
    public static final int PASSIVE_CREEPER_COST = 100;
    public static final int PASSIVE_DEFLECT_COST = 50;
    public static final int COOLDOWN = 40;

    public RepulsorItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            int energy = getEnergy(stack);
            if (energy < PULSE_COST) {
                // Fizzle: not enough charge. Click, no cooldown.
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.2f);
                return InteractionResult.FAIL;
            }

            pulse(serverLevel, player);
            setEnergy(stack, energy - PULSE_COST);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }

        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    /** Knocks back mobs, deflects projectiles, spawns the particle ring and plays the whump sound. */
    private void pulse(ServerLevel level, Player player) {
        Vec3 center = player.position();
        AABB area = player.getBoundingBox().inflate(RADIUS);

        // Mobs: outward + upward, scaled by proximity (closer = harder).
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive())) {
            Vec3 diff = target.position().subtract(center);
            double dist = diff.length();
            if (dist > RADIUS) {
                continue;
            }
            // Horizontal outward unit vector; fall back to a straight pop if dead-centred.
            Vec3 horizontal = new Vec3(diff.x, 0.0, diff.z);
            Vec3 dir = horizontal.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 0.0, 0.0) : horizontal.normalize();
            double scale = STRENGTH * (1.0 - dist / RADIUS);
            Vec3 push = new Vec3(dir.x * scale, 0.35 * scale + 0.2, dir.z * scale);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true; // force velocity sync to clients
        }

        // Projectiles: redirect outward and re-own to the player so they can no longer hurt them.
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area, e -> e.isAlive())) {
            Vec3 diff = projectile.position().subtract(center);
            if (diff.lengthSqr() < 1.0e-4) {
                continue;
            }
            projectile.setDeltaMovement(diff.normalize().scale(DEFLECT_SPEED));
            projectile.setOwner(player);
            projectile.hurtMarked = true;
        }

        // Expanding spark ring + electrical whump.
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y + 1.0, center.z,
                60, RADIUS * 0.4, 0.4, RADIUS * 0.4, 0.6);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    public boolean hasCurioCapability(ItemStack stack) {
        return true;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity().level().isClientSide() || !(slotContext.entity() instanceof Player player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        int energy = getEnergy(stack);
        boolean activated = false;

        Vec3 center = player.position();
        AABB area = player.getBoundingBox().inflate(RADIUS);

        // 1. Deflect projectiles (75% chance, evaluate once per projectile)
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
                e -> e.isAlive() && e.getOwner() != player)) {
            
            if (energy >= PASSIVE_DEFLECT_COST) {
                // Use deterministic UUID hash for ~75% chance to avoid re-evaluating the same projectile
                if ((projectile.getUUID().hashCode() & 0xFF) < 192) {
                    Vec3 diff = projectile.position().subtract(center);
                    if (diff.lengthSqr() >= 1.0e-4) {
                        projectile.setDeltaMovement(diff.normalize().scale(DEFLECT_SPEED));
                    }
                    projectile.setOwner(player);
                    projectile.hurtMarked = true;

                    energy -= PASSIVE_DEFLECT_COST;
                    activated = true;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }

        if (activated) {
            setEnergy(stack, energy);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4f, 1.2f);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Ensure damage comes from a creeper
        if (!(event.getSource().getEntity() instanceof Creeper)) {
            return;
        }

        ItemStack repulsorStack = ItemStack.EMPTY;

        if (player.getMainHandItem().is(MagnetiteItems.REPULSOR.get())) {
            repulsorStack = player.getMainHandItem();
        } else if (player.getOffhandItem().is(MagnetiteItems.REPULSOR.get())) {
            repulsorStack = player.getOffhandItem();
        } else {
            var opt = CuriosApi.getCuriosInventory(player)
                    .flatMap(inv -> inv.findFirstCurio(MagnetiteItems.REPULSOR.get()));
            if (opt.isPresent()) {
                repulsorStack = opt.get().stack();
            }
        }

        if (repulsorStack.isEmpty() || !(repulsorStack.getItem() instanceof RepulsorItem repulsor)) {
            return;
        }

        int energy = repulsor.getEnergy(repulsorStack);
        if (energy >= PASSIVE_CREEPER_COST) {
            float originalDamage = event.getAmount();
            // Negate 75% of the damage
            event.setAmount(originalDamage * 0.25f);
            
            repulsor.setEnergy(repulsorStack, energy - PASSIVE_CREEPER_COST);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
                sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.solenoid.energy_stored", getEnergy(stack), getCapacity())
                .withStyle(ChatFormatting.AQUA));
    }

    /** Exposes the EMF buffer as a Forge-Energy item handler so the Capacitor can charge it. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(
                        access, SolenoidDataComponents.EMF_ENERGY.get(), CAPACITY, MAX_TRANSFER),
                MagnetiteItems.REPULSOR.get());
    }
}
