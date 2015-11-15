package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.steamcraft.Steamcraft;
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
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

public class EntityHandler extends HandlerUtils {
	@SubscribeEvent
	public void handleCanningMachine(EntityItemPickupEvent event) {
		if (event.entityLiving instanceof EntityPlayer && !event.entityLiving.worldObj.isRemote) {
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			if (hasPower(player, 10) && player.getEquipmentInSlot(2) != null && player.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
				ItemExosuitArmor leggings = (ItemExosuitArmor) player.getEquipmentInSlot(2).getItem();
				if (leggings.hasUpgrade(player.getEquipmentInSlot(2), SteamcraftItems.canner)) {

					ItemStack item = event.item.getEntityItem().copy();

					if (item.hasTagCompound() && item.stackTagCompound.hasKey("canned")) {
						return;
					}

					int[] oreIds = OreDictionary.getOreIDs(item);
					boolean isCannable = Arrays.asList(ArrayUtils.toObject(oreIds))
						.stream()
					 	.map(OreDictionary::getOreName)
						.map(String::toLowerCase)
						.anyMatch(str -> str.contains("ingot")
								  || str.contains("gem")
							      || str.contains("ore"));

					if (item.getItem().getUnlocalizedName(item).toLowerCase().contains("ingot")
						|| item.getItem().getUnlocalizedName(item).toLowerCase().contains("gem")
						|| item.getItem().getUnlocalizedName(item).toLowerCase().contains("ore")) {
						isCannable = true;
					}

					if (isCannable) {
						int numCans = IntStream.range(0, player.inventory.getSizeInventory())
							.mapToObj(player.inventory::getStackInSlot)
						    .filter(stack -> stack != null
								  && stack.getItem() == SteamcraftItems.canister)
						    .mapToInt(stack -> stack.stackSize)
						    .sum();
						if (numCans >= item.stackSize) {
							if (!item.hasTagCompound()) {
								item.setTagCompound(new NBTTagCompound());
							}
							item.stackTagCompound.setInteger("canned", 0);
							event.item.setEntityItemStack(item);
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
						}
						IntStream.range(0, numCans)
						.forEach(i -> {
							player.inventory.consumeInventoryItem(SteamcraftItems.canister);
							player.inventoryContainer.detectAndSendChanges();
						});
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
