package themcbros.uselessmod.entity;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DrinkHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import themcbros.uselessmod.UselessTags;
import themcbros.uselessmod.init.EntityInit;
import themcbros.uselessmod.init.ItemInit;

public class UselessCowEntity extends CowEntity {

    private static final Ingredient BREED_ITEMS = Ingredient.fromTag(UselessTags.Items.CROPS_USELESS_WHEAT);

    public UselessCowEntity(EntityType<? extends CowEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, BREED_ITEMS, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
    }

    @Override
    public ActionResultType func_230254_b_(PlayerEntity playerEntity, Hand hand) {
        ItemStack heldItem = playerEntity.getHeldItem(hand);
        if (heldItem.getItem() == Items.BUCKET && !this.isChild()) {
            playerEntity.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
            ItemStack itemStack = DrinkHelper.fill(heldItem, playerEntity, ItemInit.SUGARED_MILK.get().getDefaultInstance());
            playerEntity.setHeldItem(hand, itemStack);
            return ActionResultType.func_233537_a_(this.world.isRemote);
        } else {
            return super.func_230254_b_(playerEntity, hand);
        }
    }

    @Override
    public void func_241841_a(ServerWorld p_241841_1_, LightningBoltEntity p_241841_2_) {
        super.func_241841_a(p_241841_1_, p_241841_2_);
        this.setGlowing(true);
    }

    @Override
    public CowEntity func_241840_a(ServerWorld serverWorld, AgeableEntity entity) {
        return EntityInit.USELESS_COW.get().create(this.world);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return BREED_ITEMS.test(stack);
    }
}
