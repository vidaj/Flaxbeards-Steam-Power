package flaxbeard.steamcraft.handler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import flaxbeard.steamcraft.Config;
import flaxbeard.steamcraft.Steamcraft;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.api.util.SPLog;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.IntStream;

public class HandlerUtils {
	protected static final UUID uuid = UUID.fromString("bbd786a9-611f-4c31-88ad-36dc9da3e15c");
	protected static final AttributeModifier exoBoost = new AttributeModifier(uuid, "EXOMOD", 0.2D, 2).setSaved(true);
	protected static final UUID uuid2 = UUID.fromString("33235dc2-bf3d-40e4-ae0e-78037c7535e6");
	protected static final AttributeModifier exoBoostBad = new AttributeModifier(uuid2, "EXOMODBAD", -0.2D, 2).setSaved(true);
	protected static final UUID uuid3 = UUID.fromString("33235dc2-bf3d-40e4-ae0e-78037c7535e7");
	protected static final AttributeModifier exoSwimBoost = new AttributeModifier(uuid3, "EXOSWIMBOOST", 1.0D, 2).setSaved(true);
	protected static ArrayList<Integer> extendedRange = new ArrayList<Integer>();

	protected static final ResourceLocation icons = new ResourceLocation("steamcraft:textures/gui/icons.png");
	protected static boolean lastViewVillagerGui = false;
	public static int use = -1;
	protected static HashMap<Integer, MutablePair<Double, Double>> lastMotions = new HashMap<Integer, MutablePair<Double, Double>>();
	public static HashMap<Integer, Integer> isJumping = new HashMap<Integer, Integer>();
	protected SPLog log = Steamcraft.log;
	protected HashMap<Integer, Boolean> lastHadCustomer = new HashMap<Integer, Boolean>();
	protected static boolean isShiftDown;
	HashMap<Integer, Float> prevStep = new HashMap();
	boolean lastWearing = false;
	boolean worldStartUpdate = false;

	protected void removeGoodExoBoost(EntityLivingBase entity) {
		if (entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getModifier(uuid) != null) {
			entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).removeModifier(exoBoost);
		}
		if (entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getModifier(uuid) != null) {
			entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).removeModifier(exoBoost);
		}
		if (this.prevStep.containsKey(entity.getEntityId())) {
			entity.stepHeight = this.prevStep.get(entity.getEntityId());
			this.prevStep.remove(entity.getEntityId());
		}
	}

	protected void removeBadExoBoost(EntityLivingBase entity) {
		if (entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getModifier(uuid2) != null) {
			entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).removeModifier(exoBoostBad);
		}
		if (entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getModifier(uuid2) != null) {
			entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).removeModifier(exoBoostBad);
		}
	}


	public static boolean hasPower(EntityLivingBase entityLiving, int i) {
		if (entityLiving.getEquipmentInSlot(3) != null) {
			ItemStack chestStack = entityLiving.getEquipmentInSlot(3);
			if (chestStack.getItem() instanceof ItemExosuitArmor) {
				return ((ItemExosuitArmor) chestStack.getItem()).hasPower(chestStack, i);
			}
		}
		return false;
	}

	protected int getExoArmor(EntityLivingBase entityLiving) {
		return (int)
			IntStream.rangeClosed(1, 4)
			.filter(i -> entityLiving.getEquipmentInSlot(i) != null
					&& (entityLiving.getEquipmentInSlot(i).getItem() instanceof ItemExosuitArmor))
			.count();
	}

	@SideOnly(Side.CLIENT)
	protected void updateRangeClient(LivingEvent.LivingUpdateEvent event) {
		EntityLivingBase entity = event.entityLiving;
		if (entity == Minecraft.getMinecraft().thePlayer) {
            /*
			if (!worldStartUpdate && entity.getEquipmentInSlot(3) != null && entity.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor) {
				ItemExosuitArmor chest = (ItemExosuitArmor) entity.getEquipmentInSlot(3).getItem();
				if (chest.hasUpgrade(entity.getEquipmentInSlot(3), SteamcraftItems.extendoFist)) {

					Steamcraft.proxy.extendRange(entity,Config.extendedRange);
				}
			}
            */
			worldStartUpdate = true;

			//Steamcraft.proxy.extendRange(entity,1.0F);
			boolean wearing = false;
			if (entity.getEquipmentInSlot(3) != null && entity.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor) {
				ItemExosuitArmor chest = (ItemExosuitArmor) entity.getEquipmentInSlot(3).getItem();
				if (chest.hasUpgrade(entity.getEquipmentInSlot(3), SteamcraftItems.extendoFist)) {
					Steamcraft.proxy.checkRange(entity);

					wearing = true;
				}
			}
			//			if (wearing && !lastWearing && entity.worldObj.isRemote) {
			//				Steamcraft.proxy.extendRange(entity,Config.extendedRange);
			//			}
			if (!wearing && lastWearing && entity.worldObj.isRemote) {
				Steamcraft.proxy.extendRange(entity, -Config.extendedRange);
			}
			lastWearing = wearing;
		}
	}

	public static void drainSteam(ItemStack stack, int amount) {
		if (stack != null) {
			if (!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}
			if (!stack.stackTagCompound.hasKey("steamFill")) {
				stack.stackTagCompound.setInteger("steamFill", 0);
			}
			int fill = stack.stackTagCompound.getInteger("steamFill");
			fill = Math.max(0, fill - amount);
			stack.stackTagCompound.setInteger("steamFill", fill);
		}
	}

	public static boolean isJumping(EntityPlayer player) {
		return isJumping.containsKey(player.getEntityId()) && isJumping.get(player.getEntityId()) > 0;
	}

	/*
	public boolean isMoving(EntityLivingBase entity) {
		return (entity.isp)
	}
	*/
}
