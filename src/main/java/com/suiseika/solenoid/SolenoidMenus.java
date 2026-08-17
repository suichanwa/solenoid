package com.suiseika.solenoid;

import com.suiseika.solenoid.energy.CapacitorMenu;
import com.suiseika.solenoid.energy.CentrifugeMenu;
import com.suiseika.solenoid.energy.ChemicalReactorMenu;
import com.suiseika.solenoid.energy.CrusherMenu;
import com.suiseika.solenoid.energy.DigesterMenu;
import com.suiseika.solenoid.energy.InductionFurnaceMenu;
import com.suiseika.solenoid.energy.MagneticCraneMenu;
import com.suiseika.solenoid.energy.MobMagnetMenu;
import com.suiseika.solenoid.energy.RechargerMenu;
import com.suiseika.solenoid.energy.SeparatorMenu;
import com.suiseika.solenoid.energy.ThoriumRtgMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SolenoidMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Solenoid.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher", () -> IMenuTypeExtension.create(CrusherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SeparatorMenu>> SEPARATOR_MENU =
            MENUS.register("separator", () -> IMenuTypeExtension.create(SeparatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<InductionFurnaceMenu>> INDUCTION_FURNACE_MENU =
            MENUS.register("induction_furnace", () -> IMenuTypeExtension.create(InductionFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CapacitorMenu>> CAPACITOR_MENU =
            MENUS.register("capacitor", () -> IMenuTypeExtension.create(CapacitorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ChemicalReactorMenu>> CHEMICAL_REACTOR_MENU =
            MENUS.register("chemical_reactor", () -> IMenuTypeExtension.create(ChemicalReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<DigesterMenu>> DIGESTER_MENU =
            MENUS.register("digester", () -> IMenuTypeExtension.create(DigesterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE_MENU =
            MENUS.register("centrifuge", () -> IMenuTypeExtension.create(CentrifugeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ThoriumRtgMenu>> THORIUM_RTG_MENU =
            MENUS.register("thorium_rtg", () -> IMenuTypeExtension.create(ThoriumRtgMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RechargerMenu>> RECHARGER_MENU =
            MENUS.register("recharger", () -> IMenuTypeExtension.create(RechargerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MobMagnetMenu>> MOB_MAGNET_MENU =
            MENUS.register("mob_magnet", () -> IMenuTypeExtension.create(MobMagnetMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MagneticCraneMenu>> MAGNETIC_CRANE_MENU =
            MENUS.register("magnetic_crane", () -> IMenuTypeExtension.create(MagneticCraneMenu::new));
}
