package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.entity.EntityCanisterItem;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.oredict.OreDictionary;

public class EntityHandler extends HandlerUtils {
	@SubscribeEvent
	public void handleCanningMachine(EntityItemPickupEvent event) {
		if (event.entityLiving instanceof EntityPlayer && !event.entityLiving.worldObj.isRemote) {
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			if (hasPower(player, 10) && player.getEquipmentInSlot(2) != null && player.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
				ItemExosuitArmor leggings = (ItemExosuitArmor) player.getEquipmentInSlot(2).getItem();
				if (leggings.hasUpgrade(player.getEquipmentInSlot(2), SteamcraftItems.canner)) {

					boolean isCannable = false;
					ItemStack item = event.item.getEntityItem().copy();
					if (item.hasTagCompound() && item.stackTagCompound.hasKey("canned")) {
						return;
					}

					if (item.getItem().getUnlocalizedName(item).toLowerCase().contains("ingot")
						|| item.getItem().getUnlocalizedName(item).toLowerCase().contains("gem")
						|| item.getItem().getUnlocalizedName(item).toLowerCase().contains("ore")) {
						isCannable = true;
					}
					for (int id : OreDictionary.getOreIDs(item)) {
						String str = OreDictionary.getOreName(id);
						if (str.toLowerCase().contains("ingot")
							|| str.toLowerCase().contains("gem")
							|| str.toLowerCase().contains("ore")) {
							isCannable = true;
						}
					}
					if (isCannable) {
						int numCans = 0;
						for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
							if (player.inventory.getStackInSlot(i) != null) {
								if (player.inventory.getStackInSlot(i).getItem() == SteamcraftItems.canister) {
									numCans += player.inventory.getStackInSlot(i).stackSize;
								}
							}
						}
						if (numCans >= item.stackSize) {
							if (!item.hasTagCompound()) {
								item.setTagCompound(new NBTTagCompound());
							}
							item.stackTagCompound.setInteger("canned", 0);
							event.item.setEntityItemStack(item);
							for (int i = 0; i < item.stackSize; i++) {
								player.inventory.consumeInventoryItem(SteamcraftItems.canister);
								player.inventoryContainer.detectAndSendChanges();
							}
						} else if (numCans != 0) {
							item.stackSize -= numCans;
							event.item.setEntityItemStack(item);
							ItemStack item2 = item.copy();
							item2.stackSize = numCans;
							if (!item2.hasTagCompound()) {
								item2.setTagCompound(new NBTTagCompound());
							}
							item2.stackTagCompound.setInteger("canned", 0);
							EntityItem entityItem = new EntityItem(player.worldObj, player.posX, player.posY, player.posZ, item2);
							player.worldObj.spawnEntityInWorld(entityItem);
							for (int i = 0; i < numCans; i++) {
								player.inventory.consumeInventoryItem(SteamcraftItems.canister);
								player.inventoryContainer.detectAndSendChanges();
							}
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void handleCans(EntityJoinWorldEvent event) {
		if (event.entity instanceof EntityItem && !(event.entity instanceof EntityCanisterItem)) {
			EntityItem item = (EntityItem) event.entity;
			if (item.getEntityItem().hasTagCompound() && item.getEntityItem().stackTagCompound.hasKey("canned")) {
				if (!event.world.isRemote) {
					EntityCanisterItem item2 = new EntityCanisterItem(item.worldObj, item.posX, item.posY, item.posZ, item);
					item2.motionX = item.motionX;
					item2.motionY = item.motionY;
					item2.motionZ = item.motionZ;
					item2.delayBeforeCanPickup = item.delayBeforeCanPickup;
					item.worldObj.spawnEntityInWorld(item2);
				}
				item.setDead();
			}
		}
	}
}
