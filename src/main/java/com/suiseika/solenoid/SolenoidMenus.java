package com.suiseika.solenoid;

import com.suiseika.solenoid.energy.CapacitorMenu;
import com.suiseika.solenoid.energy.CrusherMenu;
import com.suiseika.solenoid.energy.InductionFurnaceMenu;
import com.suiseika.solenoid.energy.SeparatorMenu;
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
}
