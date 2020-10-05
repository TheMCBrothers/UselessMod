package themcbros.uselessmod.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.data.ExistingFileHelper;
import themcbros.uselessmod.UselessMod;
import themcbros.uselessmod.UselessTags;
import themcbros.uselessmod.init.FluidInit;

import javax.annotation.Nullable;

/**
 * @author TheMCBrothers
 */
public class UselessFluidTagsProvider extends FluidTagsProvider {

    public UselessFluidTagsProvider(DataGenerator generatorIn, @Nullable ExistingFileHelper existingFileHelper) {
        super(generatorIn, UselessMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerTags() {
        this.getOrCreateBuilder(FluidTags.WATER).add(FluidInit.USELESS_WATER.getStillFluid(), FluidInit.USELESS_WATER.getFlowingFluid());
        this.getOrCreateBuilder(UselessTags.Fluids.GAS).add(FluidInit.USELESS_GAS.getStillFluid(), FluidInit.USELESS_GAS.getFlowingFluid());
    }

    @Override
    public String getName() {
        return "Useless Fluid Tags";
    }

}
