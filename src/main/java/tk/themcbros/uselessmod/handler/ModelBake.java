package tk.themcbros.uselessmod.handler;

import java.util.Map;

import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tk.themcbros.uselessmod.UselessMod;
import tk.themcbros.uselessmod.client.models.block.ClosetModel;
import tk.themcbros.uselessmod.lists.ModBlocks;

@Mod.EventBusSubscriber(modid = UselessMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModelBake {

    @SubscribeEvent
    public static void onModelBakeEvent(final ModelBakeEvent event) {
        Map<ResourceLocation, IBakedModel> modelRegistry = event.getModelRegistry();
        
        try {
            ResourceLocation resourceLocation = ForgeRegistries.BLOCKS.getKey(ModBlocks.CLOSET);
            ResourceLocation unbakedModelLoc = new ResourceLocation(resourceLocation.getNamespace(), "block/" + resourceLocation.getPath());
            ResourceLocation unbakedModelOpenLoc = new ResourceLocation(resourceLocation.getNamespace(), "block/" + resourceLocation.getPath() + "_open");
            
            BlockModel model = (BlockModel)event.getModelLoader().getUnbakedModel(unbakedModelLoc);
            BlockModel openModel = (BlockModel)event.getModelLoader().getUnbakedModel(unbakedModelOpenLoc);
            IBakedModel customModel = new ClosetModel(event.getModelLoader(), 
            		model, model.bake(event.getModelLoader(), ModelLoader.defaultTextureGetter(), TRSRTransformation.getRotation(Direction.NORTH), DefaultVertexFormats.BLOCK),
            		openModel, openModel.bake(event.getModelLoader(), ModelLoader.defaultTextureGetter(), TRSRTransformation.getRotation(Direction.NORTH), DefaultVertexFormats.BLOCK),
            		DefaultVertexFormats.BLOCK);

            // Replace all valid block states
            ModBlocks.CLOSET.getStateContainer().getValidStates().forEach(state -> {
                modelRegistry.put(BlockModelShapes.getModelLocation(state), customModel);
            });
            
            // Replace inventory model
            modelRegistry.put(new ModelResourceLocation(resourceLocation, "inventory"), customModel);
            
        }
        catch(Exception e) {
            UselessMod.LOGGER.warn("Could not get base Closet model. Reverting to default textures...");
            e.printStackTrace();
        }
    }
}