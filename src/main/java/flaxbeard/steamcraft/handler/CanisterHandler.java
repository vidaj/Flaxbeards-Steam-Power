package flaxbeard.steamcraft.handler;

import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.misc.Tuple;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.RecipeSorter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CanisterHandler implements IRecipe {

    static {
        RecipeSorter.register("Steamcraft:canisterHandler", CanisterHandler.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
    }

    private Tuple<Boolean, ItemStack> craftHelper(InventoryCrafting inv) {
        ItemStack output = null;
        boolean hasCan = false;
        boolean canCraft = true;

        List<ItemStack> list = IntStream.range(0, inv.getSizeInventory())
            .parallel()
            .mapToObj(inv::getStackInSlot)
            .filter(stack -> stack != null)
            .collect(Collectors.toList());
        for (ItemStack stack : list) {
            if (stack.getItem() == SteamcraftItems.canister) {
                if (!hasCan) {
                    hasCan = true;
                } else {
                    canCraft = false;
                }
            } else {
                if (output == null) {
                    output = stack.copy();
                } else {
                    canCraft = false;
                }
            }
        }

        return new Tuple<>(canCraft && hasCan && output != null, output);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        Tuple<Boolean, ItemStack> result = craftHelper(inv);
        ItemStack output = result.second();
        return result.first() && !(output.hasTagCompound() && output.stackTagCompound.hasKey("canned"));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        Tuple<Boolean, ItemStack> result = craftHelper(inv);
        ItemStack output = result.second();
        if (result.first()) {
            if (output.hasTagCompound() && output.stackTagCompound.hasKey("canned")) {
                return null;
            }
            if (!output.hasTagCompound()) {
                output.setTagCompound(new NBTTagCompound());
            }
            output.stackTagCompound.setInteger("canned", 0);
            output.stackSize = 1;
            return output;
        }
        return null;
    }

    @Override
    public int getRecipeSize() {
        return 0;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }

}
