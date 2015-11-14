package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.steamcraft.SteamcraftBlocks;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.api.ISteamTransporter;
import flaxbeard.steamcraft.api.block.IDisguisableBlock;
import flaxbeard.steamcraft.api.exosuit.UtilPlates;
import flaxbeard.steamcraft.integration.BloodMagicIntegration;
import flaxbeard.steamcraft.integration.CrossMod;
import flaxbeard.steamcraft.integration.baubles.BaublesIntegration;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import flaxbeard.steamcraft.item.firearm.ItemFirearm;
import flaxbeard.steamcraft.item.firearm.ItemRocketLauncher;
import flaxbeard.steamcraft.item.tool.steam.ItemSteamAxe;
import flaxbeard.steamcraft.item.tool.steam.ItemSteamDrill;
import flaxbeard.steamcraft.item.tool.steam.ItemSteamShovel;
import flaxbeard.steamcraft.packet.SteamcraftClientPacketHandler;
import flaxbeard.steamcraft.tile.TileEntitySteamHeater;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;

public class PlayerHandler extends HandlerUtils {
	@SubscribeEvent
	public void handleSteamcraftArmorMining(PlayerEvent.BreakSpeed event) {
		boolean hasPower = hasPower(event.entityLiving, 1);
		int armor = getExoArmor(event.entityLiving);
		EntityLivingBase entity = event.entityLiving;
		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			if (CrossMod.BAUBLES) {
				if (player.getHeldItem() != null && BaublesIntegration.checkForSurvivalist(player)) {
					if (player.getHeldItem().getItem() instanceof ItemTool) {
						if (player.getHeldItem().getItemDamage() >= player.getHeldItem().getMaxDamage() - 1) {

							event.newSpeed = 0.0F;
						}
					}

				}
			} else if (player.getHeldItem() != null && hasItemInHotbar(player, SteamcraftItems.survivalist)) {
				if (player.getHeldItem().getItem() instanceof ItemTool) {
					if (player.getHeldItem().getItemDamage() >= player.getHeldItem().getMaxDamage() - 1) {
						event.newSpeed = 0.0F;
					}
				}
			}
			if (player.getHeldItem() != null) {
				if (player.getHeldItem().getItem() instanceof ItemSteamDrill) {
					ItemSteamDrill.checkNBT(player);
					MutablePair info = ItemSteamDrill.stuff.get(player.getEntityId());
					int ticks = (Integer) info.left;
					int speed = (Integer) info.right;
					if (speed > 0 && Items.iron_pickaxe.func_150893_a(player.getHeldItem(), event.block) != 1.0F) {
						event.newSpeed *= 1.0F + 11.0F * (speed / 1000.0F);
					}
				}
				if (player.getHeldItem().getItem() instanceof ItemSteamAxe) {
					ItemSteamAxe.checkNBT(player);
					MutablePair info = ItemSteamAxe.stuff.get(player.getEntityId());
					int ticks = (Integer) info.left;
					int speed = (Integer) info.right;
					if (speed > 0 && Items.diamond_axe.func_150893_a(player.getHeldItem(), event.block) != 1.0F) {
						event.newSpeed *= 1.0F + 11.0F * (speed / 1000.0F);
					}
				}
				if (player.getHeldItem().getItem() instanceof ItemSteamShovel) {
					ItemSteamShovel.checkNBT(player);
					ItemSteamShovel shovel = (ItemSteamShovel) player.getHeldItem().getItem();
					MutablePair info = ItemSteamShovel.stuff.get(player.getEntityId());
					int ticks = (Integer) info.left;
					int speed = (Integer) info.right;

					if (speed > 0 && Items.diamond_shovel.func_150893_a(player.getHeldItem(), event.block) != 1.0F) {
						event.newSpeed *= 1.0F + 19.0F * (speed / 3000.0F);
					}
				}
			}
		}


		if (hasPower && armor == 4) {
			event.newSpeed = event.newSpeed * 1.2F;
		}
	}

	@SubscribeEvent
	public void clickLeft(PlayerInteractEvent event) {
		if (CrossMod.BLOOD_MAGIC) {
			BloodMagicIntegration.clickLeft(event);
		}
		if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && event.face != 1 && event.world.getBlock(event.x, event.y, event.z).isSideSolid(event.world, event.x, event.y, event.z, ForgeDirection.getOrientation(event.face))) {

			EntityPlayer player = event.entityPlayer;
			if (event.world.isRemote && player.getEquipmentInSlot(3) != null && player.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor) {
				if (event.face != 0) {
					ItemExosuitArmor chest = (ItemExosuitArmor) player.getEquipmentInSlot(3).getItem();
					boolean canStick = false;
					ForgeDirection dir = ForgeDirection.getOrientation(event.face);
					List list = event.world.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(event.x + (dir.offsetX / 6F), event.y + (dir.offsetY / 6F) - 1.0F, event.z + (dir.offsetZ / 6F), event.x + (dir.offsetX / 6F) + 1, event.y + (dir.offsetY / 6F) + 2.0F, event.z + (dir.offsetZ / 6F) + 1));
					for (Object obj : list) {
						if (obj == player) {
							canStick = true;
						}
					}
					if (event.world.isRemote && canStick && chest.hasUpgrade(player.getEquipmentInSlot(3), SteamcraftItems.pitonDeployer)) {
						player.getEquipmentInSlot(3).stackTagCompound.setFloat("x", (float) player.posX);
						player.getEquipmentInSlot(3).stackTagCompound.setFloat("z", (float) player.posZ);
						player.getEquipmentInSlot(3).stackTagCompound.setFloat("y", (float) player.posY);
						player.getEquipmentInSlot(3).stackTagCompound.setInteger("blockX", event.x);
						player.getEquipmentInSlot(3).stackTagCompound.setInteger("blockY", event.y);
						player.getEquipmentInSlot(3).stackTagCompound.setInteger("blockZ", event.z);

						player.getEquipmentInSlot(3).stackTagCompound.setBoolean("grappled", true);
						player.motionX = 0.0F;
						player.motionY = 0.0F;
						player.motionZ = 0.0F;
						player.fallDistance = 0.0F;
						SteamcraftClientPacketHandler.sendGrapplePacket(player, event.x, event.y, event.z);
					}
				} else {
					ItemExosuitArmor chest = (ItemExosuitArmor) player.getEquipmentInSlot(3).getItem();
					boolean canStick = false;
					ForgeDirection dir = ForgeDirection.getOrientation(event.face);
					List list = event.world.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(event.x - 0.5F, event.y + (dir.offsetY / 6F) - 0.4F, event.z - 0.20F, event.x + 0.5F + 1, event.y + (dir.offsetY / 6F) + 1, event.z + 0.5F + 1));
					for (Object obj : list) {
						if (obj == player) {
							canStick = true;
						}
					}
					if (canStick && event.world.isRemote && chest.hasUpgrade(player.getEquipmentInSlot(3), SteamcraftItems.pitonDeployer)) {
						player.getEquipmentInSlot(3).stackTagCompound.setFloat("x", (float) player.posX);
						player.getEquipmentInSlot(3).stackTagCompound.setFloat("z", (float) player.posZ);
						player.getEquipmentInSlot(3).stackTagCompound.setFloat("y", (float) player.posY);
						player.getEquipmentInSlot(3).stackTagCompound.setInteger("blockX", event.x);
						player.getEquipmentInSlot(3).stackTagCompound.setInteger("blockY", event.y);
						player.getEquipmentInSlot(3).stackTagCompound.setInteger("blockZ", event.z);
						player.getEquipmentInSlot(3).stackTagCompound.setBoolean("grappled", true);
						player.motionX = 0.0F;
						player.motionY = 0.0F;
						player.motionZ = 0.0F;
						player.fallDistance = 0.0F;
						SteamcraftClientPacketHandler.sendGrapplePacket(player, event.x, event.y, event.z);
					}
				}
			}

		}

		if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK &&
			event.entityPlayer != null &&
			event.world != null &&
			event.entityPlayer.isSneaking() &&
			((event.world.getTileEntity(event.x, event.y, event.z) != null &&
				event.world.getTileEntity(event.x, event.y, event.z) instanceof IDisguisableBlock) || event.world.getBlock(event.x, event.y, event.z) == SteamcraftBlocks.pipe) &&
			event.entityPlayer.getHeldItem() != null &&
			event.entityPlayer.getHeldItem().getItem() instanceof ItemBlock) {
			Block block = Block.getBlockFromItem(event.entityPlayer.getHeldItem().getItem());
			if (!(block instanceof BlockContainer) && !(block instanceof ITileEntityProvider) && (block.getRenderType() == 0 || block.getRenderType() == 39 || block.getRenderType() == 31) && (block.renderAsNormalBlock() || (block == Blocks.glass && event.world.getBlock(event.x, event.y, event.z) == SteamcraftBlocks.pipe))) {
				event.setCanceled(true);
			}
		}
		if (event.entityPlayer.worldObj.getTileEntity(event.x, event.y, event.z) != null && !event.entityPlayer.worldObj.isRemote) {
			if (event.entityPlayer.worldObj.getTileEntity(event.x, event.y, event.z) instanceof TileEntitySteamHeater) {
			}
			if (event.entityPlayer.worldObj.getTileEntity(event.x, event.y, event.z) instanceof ISteamTransporter) {
				ISteamTransporter trans = (ISteamTransporter) event.entityPlayer.worldObj.getTileEntity(event.x, event.y, event.z);
				if (event.entityPlayer.worldObj.isRemote) {
					//////Steamcraft.log.debug("I AM THE CLIENT");
				}
				//FMLRelaunchLog.info(trans.getSteam() + " " + trans.getPressure() + " " + trans.getNetworkName() + "; " + trans.getNetwork(), "Snap");
				//	log.debug("network: " + trans.getNetworkName() + "; net cap: "+trans.getNetwork().getCapacity()+"; net steam: " + trans.getNetwork().getSteam()+"; net press: "+trans.getNetwork().getPressure() +"; trans cap: "+trans.getCapacity()+" trans steam: "+trans.getSteam() + "; trans press: " + trans.getPressure() + ";");
			}

		}
	}

	@SubscribeEvent
	public void doubleExp(PlayerPickupXpEvent event) {
		EntityPlayer player = event.entityPlayer;
		for (int i = 1; i < 5; i++) {
			float multValu = 1;
			if (player.getEquipmentInSlot(i) != null) {
				ItemStack stack = player.getEquipmentInSlot(i);
				if (stack.getItem() instanceof ItemExosuitArmor) {
					if (((ItemExosuitArmor) stack.getItem()).hasPlates(stack) && UtilPlates.getPlate(stack.stackTagCompound.getString("plate")).getIdentifier() == "Gold") {
						multValu *= 1.25F;
					}
				}
			}
			event.orb.xpValue = MathHelper.ceiling_float_int(event.orb.xpValue * multValu);
		}
	}

	@SubscribeEvent
	public void useItem(PlayerUseItemEvent.Tick event) {
		if (event.item.getItem() instanceof ItemFirearm || event.item.getItem() instanceof ItemRocketLauncher) {
			use = event.duration;
		}
	}

	@SubscribeEvent
	public void useItemEnd(PlayerUseItemEvent.Finish event) {
		if (event.item.getItem() instanceof ItemFirearm || event.item.getItem() instanceof ItemRocketLauncher) {
			use = -1;
		}
	}

	@SubscribeEvent
	public void useItemEnd(PlayerUseItemEvent.Stop event) {
		if (event.item.getItem() instanceof ItemFirearm || event.item.getItem() instanceof ItemRocketLauncher) {
			use = -1;
		}
	}

	@SubscribeEvent
	public void rightClick(PlayerInteractEvent event) {
		if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
			if (event.entityPlayer.getHeldItem() != null) {
				if ((event.entityPlayer.getHeldItem().getItem() instanceof ItemSteamDrill || event.entityPlayer.getHeldItem().getItem() instanceof ItemSteamAxe || event.entityPlayer.getHeldItem().getItem() instanceof ItemSteamShovel) && (event.entityPlayer.worldObj.getBlock(event.x, event.y, event.z) == null || event.entityPlayer.worldObj.getBlock(event.x, event.y, event.z) != SteamcraftBlocks.charger)) {
					event.setCanceled(true);
				}
			}
		}

	}

	private boolean hasItemInHotbar(EntityPlayer player, Item item) {
		for (int i = 0; i < 10; i++) {
			if (player.inventory.getStackInSlot(i) != null && player.inventory.getStackInSlot(i).getItem() == item) {
				return true;
			}
		}
		return false;
	}
}
