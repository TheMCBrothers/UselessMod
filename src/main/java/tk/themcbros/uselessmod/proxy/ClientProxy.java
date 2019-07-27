package tk.themcbros.uselessmod.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import tk.themcbros.uselessmod.UselessMod;
import tk.themcbros.uselessmod.blocks.GreenstoneWireBlock;
import tk.themcbros.uselessmod.blocks.LampBlock;
import tk.themcbros.uselessmod.client.gui.ClosetScreen;
import tk.themcbros.uselessmod.client.gui.CoffeeMachineScreen;
import tk.themcbros.uselessmod.client.gui.CompressorScreen;
import tk.themcbros.uselessmod.client.gui.CrusherScreen;
import tk.themcbros.uselessmod.client.gui.ElectricCrusherScreen;
import tk.themcbros.uselessmod.client.gui.ElectricFurnaceScreen;
import tk.themcbros.uselessmod.client.gui.GlowstoneGeneratorScreen;
import tk.themcbros.uselessmod.client.renders.entity.UselessRenderRegistry;
import tk.themcbros.uselessmod.lists.CoffeeType;
import tk.themcbros.uselessmod.lists.ModBlocks;
import tk.themcbros.uselessmod.lists.ModContainerTypes;
import tk.themcbros.uselessmod.lists.ModItems;
import tk.themcbros.uselessmod.tileentity.ColorableTileEntity;
import tk.themcbros.uselessmod.tileentity.EnergyCableTileEntity;
import tk.themcbros.uselessmod.tileentity.renderer.EnergyCableTileEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

	public ClientProxy() {
		super();
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
	}
	
	private void clientSetup(FMLClientSetupEvent event) {
		UselessMod.LOGGER.debug("ClientProxy clientSetup method");
		
		ScreenManager.registerFactory(ModContainerTypes.CRUSHER, CrusherScreen::new);
		ScreenManager.registerFactory(ModContainerTypes.ELECTRIC_CRUSHER, ElectricCrusherScreen::new);
		ScreenManager.registerFactory(ModContainerTypes.ELECTRIC_FURNACE, ElectricFurnaceScreen::new);
		ScreenManager.registerFactory(ModContainerTypes.COMPRESSOR, CompressorScreen::new);
		ScreenManager.registerFactory(ModContainerTypes.GLOWSTONE_GENRATOR, GlowstoneGeneratorScreen::new);
		ScreenManager.registerFactory(ModContainerTypes.COFFEE_MACHINE, CoffeeMachineScreen::new);
		ScreenManager.registerFactory(ModContainerTypes.CLOSET, ClosetScreen::new);
		
		UselessRenderRegistry.registerEntityRenders();
		
		ClientRegistry.bindTileEntitySpecialRenderer(EnergyCableTileEntity.class, new EnergyCableTileEntityRenderer());
		
		OBJLoader.INSTANCE.addDomain(UselessMod.MOD_ID);
	}
	
	private void registerBlockColours(ColorHandlerEvent.Block event) {
        BlockColors blockColors = event.getBlockColors();
        
        blockColors.register((state, world, pos, tintIndex) -> {
            return tintIndex == 0 ? 0xcf2560 : tintIndex == 1 ? 0x46a37e : -1;
         }, ModBlocks.CHEESE_MAKER);
        
        blockColors.register((state, world, pos, tintIndex) -> {
            return GreenstoneWireBlock.colorMultiplier(state.get(BlockStateProperties.POWER_0_15));
         }, ModBlocks.GREENSTONE_WIRE);
        
        blockColors.register((state, world, pos, tintIndex) -> {
        	if(state != null && world != null && pos != null) {
	        	TileEntity tileEntity = world.getTileEntity(pos);
	        	if(tileEntity != null && tileEntity instanceof ColorableTileEntity) {
	        		return ((ColorableTileEntity) tileEntity).getColor();
	        	}
	        }
            return 4159204;
         }, ModBlocks.CANVAS, ModBlocks.PAINT_BUCKET);
        
        blockColors.register((state, world, pos, tintIndex) -> {
        	DyeColor color = state.get(LampBlock.COLOR);
        	float[] colourComponents = color.getColorComponentValues();
            int colour = (int) (colourComponents[0] * 255F);
            colour = (int) ((colour << 8) + colourComponents[1] * 255F);
            colour = (int) ((colour << 8) + colourComponents[2] * 255F);
            return colour;
        }, ModBlocks.LAMP);
        
    }
	
	private void registerItemColors(ColorHandlerEvent.Item event) {
		ItemColors itemColors = event.getItemColors();
		
		itemColors.register((stack, tintIndex) -> {
			return stack.hasTag() && stack.getTag().contains("color") && tintIndex == 1 ? stack.getTag().getInt("color") : -1;
		}, ModItems.PAINT_BRUSH);
		
		itemColors.register((stack, tintIndex) -> {
			return stack.hasTag() && stack.getTag().contains("color") ? stack.getTag().getInt("color") : -1;
		}, ModBlocks.CANVAS, ModBlocks.PAINT_BUCKET);
		
		itemColors.register((stack, tintIndex) -> {
			CompoundNBT compound = stack.getChildTag("uselessmod");
			if(compound != null) {
				for(CoffeeType type : CoffeeType.values()) {
					if(compound.getString("CoffeeType").equals(type.getName())) {
						if(type != null) {
							return type.getColor();
						}
					}
				}
			}
			return -1;
		}, ModItems.COFFEE_CUP);
	}
	
	@Override
	protected void postInit(InterModProcessEvent event) {
		super.postInit(event);
		
		this.registerBlockColours(new ColorHandlerEvent.Block(Minecraft.getInstance().getBlockColors()));
		this.registerItemColors(new ColorHandlerEvent.Item(Minecraft.getInstance().getItemColors(), Minecraft.getInstance().getBlockColors()));
	}
	
}