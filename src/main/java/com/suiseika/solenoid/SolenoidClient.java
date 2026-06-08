package com.suiseika.solenoid;

import com.suiseika.solenoid.energy.CapacitorScreen;
import com.suiseika.solenoid.energy.CentrifugeScreen;
import com.suiseika.solenoid.energy.ChemicalReactorScreen;
import com.suiseika.solenoid.energy.CrusherScreen;
import com.suiseika.solenoid.energy.DigesterScreen;
import com.suiseika.solenoid.energy.InductionFurnaceScreen;
import com.suiseika.solenoid.energy.SeparatorScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Solenoid.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Solenoid.MODID, value = Dist.CLIENT)
public class SolenoidClient {
    public SolenoidClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Solenoid.LOGGER.info("HELLO FROM CLIENT SETUP");
        Solenoid.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SolenoidMenus.CRUSHER_MENU.get(), CrusherScreen::new);
        event.register(SolenoidMenus.SEPARATOR_MENU.get(), SeparatorScreen::new);
        event.register(SolenoidMenus.INDUCTION_FURNACE_MENU.get(), InductionFurnaceScreen::new);
        event.register(SolenoidMenus.CAPACITOR_MENU.get(), CapacitorScreen::new);
        event.register(SolenoidMenus.CHEMICAL_REACTOR_MENU.get(), ChemicalReactorScreen::new);
        event.register(SolenoidMenus.DIGESTER_MENU.get(), DigesterScreen::new);
        event.register(SolenoidMenus.CENTRIFUGE_MENU.get(), CentrifugeScreen::new);
    }
}
