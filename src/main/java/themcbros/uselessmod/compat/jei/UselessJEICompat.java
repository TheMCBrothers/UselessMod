package themcbros.uselessmod.compat.jei;

import com.google.common.collect.Lists;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import themcbros.uselessmod.UselessMod;
import themcbros.uselessmod.client.screen.CoffeeMachineScreen;
import themcbros.uselessmod.compat.jei.coffee_machine.CoffeeMachineCategory;
import themcbros.uselessmod.init.BlockInit;
import themcbros.uselessmod.init.ItemInit;
import themcbros.uselessmod.recipe.RecipeValidator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

@JeiPlugin
public class UselessJEICompat implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return UselessMod.rl(UselessMod.MOD_ID);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(BlockInit.WALL_CLOSET.get().asItem(), ItemInit.COFFEE_CUP.get(),
                ItemInit.USELESS_BUCKET.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CoffeeMachineCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BlockInit.COFFEE_MACHINE.get()), UselessRecipeCategoryUid.COFFEE_MACHINE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(Lists.newArrayList(RecipeValidator.getCoffeeRecipes()), UselessRecipeCategoryUid.COFFEE_MACHINE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CoffeeMachineScreen.class, new CoffeeMachineHandler());
    }

    private static class CoffeeMachineHandler implements IGuiContainerHandler<CoffeeMachineScreen> {
        @Nullable
        @Override
        public Object getIngredientUnderMouse(CoffeeMachineScreen containerScreen, double mouseX, double mouseY) {
            return containerScreen.getHoveredFluid();
        }

        @Override
        public Collection<IGuiClickableArea> getGuiClickableAreas(CoffeeMachineScreen containerScreen, double mouseX, double mouseY) {
            IGuiClickableArea clickableArea = IGuiClickableArea.createBasic(62, 33, 52, 18, UselessRecipeCategoryUid.COFFEE_MACHINE);
            return Collections.singleton(clickableArea);
        }
    }

}
