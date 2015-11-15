package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.api.IPipeWrench;
import flaxbeard.steamcraft.api.IWrenchDisplay;
import flaxbeard.steamcraft.api.SteamcraftRegistry;
import flaxbeard.steamcraft.integration.BotaniaIntegration;
import flaxbeard.steamcraft.integration.CrossMod;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

public class RenderHandler {
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onDrawScreen(RenderGameOverlayEvent.Post event) {
		if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
			Minecraft mc = Minecraft.getMinecraft();
			EntityPlayer player = mc.thePlayer;
            /*
            if (!player.capabilities.isCreativeMode && player.inventory.armorItemInSlot(1) != null && player.inventory.armorItemInSlot(1).getItem() instanceof ItemExosuitArmor) {
				ItemStack stack = player.inventory.armorItemInSlot(1);
				ItemExosuitArmor item = (ItemExosuitArmor) stack.getItem();
				if (item.hasUpgrade(stack, SteamcraftItems.doubleJump)) {
					if (!stack.stackTagCompound.hasKey("aidTicks")) {
						stack.stackTagCompound.setInteger("aidTicks", -1);
					}
					int aidTicks = stack.stackTagCompound.getInteger("aidTicks");

					int aidTicksScaled = 7-(int)(aidTicks*7.0F / 100.0F);
					int screenX = event.resolution.getScaledWidth() / 2  - 101;
					int screenY = event.resolution.getScaledHeight() - 39;
					mc.getTextureManager().bindTexture(icons);
					renderTexture(screenX,screenY,9,9,0,0,9D/256D,9D/256D);
					if (aidTicks > 0) {
						renderTexture(screenX+1,screenY,aidTicksScaled,9,10D/256D,0,(10D+aidTicksScaled)/256D,9D/256D);
					}
					else if (aidTicks == 0) {
						renderTexture(screenX,screenY,9,9,18D/256D,0,27D/256D,9D/256D);
					}
				}

			}
            */
			Item equipped = player.getCurrentEquippedItem() != null ? player.getCurrentEquippedItem().getItem() : null;
			MovingObjectPosition pos = mc.objectMouseOver;
			if (pos != null && mc.thePlayer.getCurrentEquippedItem() != null && mc.thePlayer.getCurrentEquippedItem().getItem() instanceof IPipeWrench && ((IPipeWrench) equipped).canWrench(player, pos.blockX, pos.blockY, pos.blockZ)) {
				TileEntity te = mc.theWorld.getTileEntity(pos.blockX, pos.blockY, pos.blockZ);
				if (te instanceof IWrenchDisplay) {
					((IWrenchDisplay) te).displayWrench(event);
				}
			}
			if (CrossMod.BOTANIA) {
				if (pos != null && player.getEquipmentInSlot(3) != null && player.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor && player.getEquipmentInSlot(4) != null && player.getEquipmentInSlot(4).getItem() instanceof ItemExosuitArmor && (player.getHeldItem() == null || player.getHeldItem().getItem() != BotaniaIntegration.twigWand())) {
					ItemExosuitArmor chest = (ItemExosuitArmor) player.getEquipmentInSlot(3).getItem();
					if (chest.hasUpgrade(player.getEquipmentInSlot(3), BotaniaIntegration.floralLaurel)) {
						Block block = mc.theWorld.getBlock(pos.blockX, pos.blockY, pos.blockZ);
						BotaniaIntegration.displayThings(pos, event);
					}
				}
			}
			if (pos != null && mc.thePlayer.getCurrentEquippedItem() != null && mc.thePlayer.getCurrentEquippedItem().getItem() == SteamcraftItems.book) {
				Block block = mc.theWorld.getBlock(pos.blockX, pos.blockY, pos.blockZ);
				ItemStack stack = block.getPickBlock(pos, player.worldObj, pos.blockX, pos.blockY, pos.blockZ, player);
				if (stack != null) {
					SteamcraftRegistry.bookRecipes.keySet().stream()
					.filter(stack2 -> stack2.getItem() == stack.getItem()
						    && stack2.getItemDamage() == stack.getItemDamage())
					.forEach(stack2 -> {
						GL11.glPushMatrix();
						int x = event.resolution.getScaledWidth() / 2 - 8;
						int y = event.resolution.getScaledHeight() / 2 - 8;

						int color = 0x6600FF00;
						RenderItem.getInstance().renderItemIntoGUI(mc.fontRenderer, mc.renderEngine, new ItemStack(SteamcraftItems.book), x, y);
						GL11.glDisable(GL11.GL_LIGHTING);
						mc.fontRenderer.drawStringWithShadow("", x + 15, y + 13, 0xC6C6C6);
						GL11.glPopMatrix();
						GL11.glEnable(GL11.GL_BLEND);
					});

				}
			}

		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void handleRocketDisplay(RenderGameOverlayEvent.Post event) {
		if (event.type == RenderGameOverlayEvent.ElementType.ALL && Minecraft.getMinecraft().thePlayer.getHeldItem() != null && Minecraft.getMinecraft().thePlayer.getHeldItem().getItem() == SteamcraftItems.rocketLauncher) {
			Minecraft mc = Minecraft.getMinecraft();
			ScaledResolution var5 = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
			int var6 = var5.getScaledWidth();
			int var7 = var5.getScaledHeight();
			FontRenderer var8 = mc.fontRenderer;
			int selectedRocketType = 0;
			boolean itemHasTag = Minecraft.getMinecraft().thePlayer.getHeldItem().hasTagCompound();
			boolean itemHasTagRocket = itemHasTag && Minecraft.getMinecraft().thePlayer.getHeldItem().stackTagCompound.hasKey("rocketType");
			if (itemHasTagRocket) {
				selectedRocketType = Minecraft.getMinecraft().thePlayer.getHeldItem().stackTagCompound.getInteger("rocketType");
			}
			if (selectedRocketType > SteamcraftRegistry.rockets.size() - 1) {
				selectedRocketType = 0;
			}
			String tooltip = StatCollector.translateToLocal("steamcraft.rocket") + " " +
				(selectedRocketType == 0 ? StatCollector.translateToLocal("item.steamcraft:rocket.name.2") : StatCollector.translateToLocal(((Item) SteamcraftRegistry.rockets.get(selectedRocketType)).getUnlocalizedName() + ".name"));

			int tooltipStartX = (var6 - var8.getStringWidth(tooltip)) / 2;
			int tooltipStartY = var7 - 35 - (Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode ? 0 : 35);

			GL11.glPushMatrix();
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			var8.drawStringWithShadow(tooltip, tooltipStartX, tooltipStartY, 0xFFFFFF);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glPopMatrix();
		}
	}

	/*
	@SubscribeEvent
	public void preRender(RenderLivingEvent.Pre event) {
		if (event.entity.isPotionActive(Steamcraft.semiInvisible)) {
	        GL11.glPushMatrix();
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.10F);
	        GL11.glDepthMask(false);
	        GL11.glEnable(GL11.GL_BLEND);
	        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
		}
	}

	@SubscribeEvent
	public void postRender(RenderLivingEvent.Post event) {
		if (event.entity.isPotionActive(Steamcraft.semiInvisible)) {
	        GL11.glDisable(GL11.GL_BLEND);
	        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
	        GL11.glPopMatrix();
	        GL11.glDepthMask(true);
		}
	}
    */
}
