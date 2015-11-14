package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.entity.IMerchant;
import net.minecraft.util.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.client.event.GuiScreenEvent;

public class GuiEventHandler extends HandlerUtils {
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void updateVillagersClientside(GuiScreenEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (event.gui instanceof GuiMerchant && !lastViewVillagerGui) {
			GuiMerchant gui = (GuiMerchant) event.gui;
			if (mc.thePlayer.inventory.armorInventory[3] != null && (mc.thePlayer.inventory.armorInventory[3].getItem() == SteamcraftItems.tophat
				|| (mc.thePlayer.inventory.armorInventory[3].getItem() == SteamcraftItems.exoArmorHead
				&& ((ItemExosuitArmor) mc.thePlayer.inventory.armorInventory[3].getItem()).hasUpgrade(mc.thePlayer.inventory.armorInventory[3], SteamcraftItems.tophat)))) {
				IMerchant merch = ReflectionHelper.getPrivateValue(GuiMerchant.class, gui, 2);
				MerchantRecipeList recipeList = merch.getRecipes(mc.thePlayer);
				if (recipeList != null) {
					for (Object obj : recipeList) {
						MerchantRecipe recipe = (MerchantRecipe) obj;
						if (recipe.getItemToSell().stackSize > 1 && recipe.getItemToSell().stackSize != MathHelper.floor_float(recipe.getItemToSell().stackSize * 1.25F)) {
							recipe.getItemToSell().stackSize = MathHelper.floor_float(recipe.getItemToSell().stackSize * 1.25F);
						} else if (recipe.getItemToBuy().stackSize > 1 && recipe.getItemToBuy().stackSize != MathHelper.ceiling_float_int(recipe.getItemToBuy().stackSize / 1.25F)) {
							recipe.getItemToBuy().stackSize = MathHelper.ceiling_float_int(recipe.getItemToBuy().stackSize / 1.25F);
						} else if (recipe.getSecondItemToBuy() != null && recipe.getSecondItemToBuy().stackSize > 1 && recipe.getSecondItemToBuy().stackSize != MathHelper.ceiling_float_int(recipe.getSecondItemToBuy().stackSize / 1.25F)) {
							recipe.getSecondItemToBuy().stackSize = MathHelper.ceiling_float_int(recipe.getSecondItemToBuy().stackSize / 1.25F);
						}
					}
					lastViewVillagerGui = true;
				}
				merch.setRecipes(recipeList);
				ReflectionHelper.setPrivateValue(GuiMerchant.class, gui, merch, 2);
			}
		}
	}
}
