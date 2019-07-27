package tk.themcbros.uselessmod.proxy;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import tk.themcbros.uselessmod.UselessMod;
import tk.themcbros.uselessmod.closet.BeddingRegistryEvent;
import tk.themcbros.uselessmod.closet.ClosetRegistry;
import tk.themcbros.uselessmod.config.Config;
import tk.themcbros.uselessmod.lists.ModBiomes;
import tk.themcbros.uselessmod.world.FlowerGeneration;
import tk.themcbros.uselessmod.world.OreGeneration;

public class CommonProxy {

	public CommonProxy() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.server_config);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.client_config);
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
	}
	
	protected void preInit(FMLCommonSetupEvent event) {
		UselessMod.LOGGER.debug("CommonProxy preInit method");
		
		Config.loadConfig(Config.client_config, FMLPaths.CONFIGDIR.get().resolve("uselessmod-client.toml").toString());
		Config.loadConfig(Config.server_config, FMLPaths.CONFIGDIR.get().resolve("uselessmod-server.toml").toString());
		
		OreGeneration.setupOreGeneration();
		OreGeneration.setupNetherOreGeneration();
		
		FlowerGeneration.setupFlowerGeneration();
	}
	
	protected void init(InterModEnqueueEvent event) {
		UselessMod.LOGGER.debug("CommonProxy init method");
	}
	
	protected void postInit(InterModProcessEvent event) {
		UselessMod.LOGGER.debug("CommonProxy postInit method");
		FMLJavaModLoadingContext.get().getModEventBus().post(new BeddingRegistryEvent(ClosetRegistry.CASINGS, ClosetRegistry.BEDDINGS));
		
		ModBiomes.addBiomesToManager();
	}
	
}