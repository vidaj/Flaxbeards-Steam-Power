package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import flaxbeard.steamcraft.Config;
import flaxbeard.steamcraft.Steamcraft;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.api.exosuit.UtilPlates;
import flaxbeard.steamcraft.integration.BloodMagicIntegration;
import flaxbeard.steamcraft.integration.CrossMod;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.List;

public class LivingHandler extends HandlerUtils {
	@SubscribeEvent
	public void handleFirePunch(LivingAttackEvent event) {
		if (event.source.getSourceOfDamage() instanceof EntityLivingBase) {
			EntityLivingBase entity = (EntityLivingBase) event.source.getSourceOfDamage();
			boolean hasPower = hasPower(entity, Config.powerFistConsumption);
			if (hasPower && entity.getEquipmentInSlot(3) != null && entity.getHeldItem() == null) {
				ItemExosuitArmor chest = (ItemExosuitArmor) entity.getEquipmentInSlot(3).getItem();
				if (chest.hasUpgrade(entity.getEquipmentInSlot(3), SteamcraftItems.powerFist)) {
					entity.worldObj.playSoundEffect(entity.posX, entity.posY, entity.posZ, "random.explode", 4.0F, (1.0F + (entity.worldObj.rand.nextFloat() - entity.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);
					event.entityLiving.motionX += 3.0F * entity.getLookVec().normalize().xCoord;
					event.entityLiving.motionY += (entity.getLookVec().normalize().yCoord > 0.0F ? 2.0F * entity.getLookVec().normalize().yCoord : 0.0F) + 1.5F;
					event.entityLiving.motionZ += 3.0F * entity.getLookVec().normalize().zCoord;
					entity.motionX += -0.5F * entity.getLookVec().normalize().xCoord;
					entity.motionZ += -0.5F * entity.getLookVec().normalize().zCoord;
					drainSteam(event.entityLiving.getEquipmentInSlot(3), Config.powerFistConsumption);
				}
			}
		}
	}

	@SubscribeEvent
	public void handleSteamcraftArmor(LivingEvent.LivingUpdateEvent event) {
		boolean hasPower = hasPower(event.entityLiving, 1);
		int armor = getExoArmor(event.entityLiving);
		EntityLivingBase entity = event.entityLiving;
		ItemStack armor2 = entity.getEquipmentInSlot(1);
		//Steamcraft.proxy.extendRange(entity,1.0F);

		if (entity.worldObj.isRemote) {
			this.updateRangeClient(event);
		} else {
			boolean wearing = false;
			if (entity.getEquipmentInSlot(3) != null && entity.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor) {
				ItemExosuitArmor chest = (ItemExosuitArmor) entity.getEquipmentInSlot(3).getItem();
				if (chest.hasUpgrade(entity.getEquipmentInSlot(3), SteamcraftItems.extendoFist)) {
					if (!extendedRange.contains(entity.getEntityId())) {
						wearing = true;
						extendedRange.add(entity.getEntityId());
						Steamcraft.proxy.extendRange(entity, Config.extendedRange);
					}
				}
			}
			if (!wearing && extendedRange.contains(entity.getEntityId())) {
				Steamcraft.proxy.extendRange(entity, -Config.extendedRange);
				extendedRange.remove((Integer) entity.getEntityId());
			}
		}

		if (hasPower) {
			if (entity.isSneaking()) {
                /*
				if ((!event.entityLiving.isPotionActive(Steamcraft.semiInvisible) || event.entityLiving.getActivePotionEffect(Steamcraft.semiInvisible).getDuration() < 2)) {
					event.entityLiving.addPotionEffect(new PotionEffect(Steamcraft.semiInvisible.id, 2, 0, false));
				}
                */
			}

			if (!lastMotions.containsKey(entity.getEntityId())) {
				lastMotions.put(entity.getEntityId(), MutablePair.of(entity.posX, entity.posZ));
			}
			if (entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getModifier(uuid2) != null) {
				entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).removeModifier(exoBoostBad);
			}
			if (entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getModifier(uuid2) != null) {
				entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).removeModifier(exoBoostBad);
			}
			ItemStack stack = entity.getEquipmentInSlot(3);
			if (!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}
			if (!stack.stackTagCompound.hasKey("ticksUntilConsume")) {
				stack.stackTagCompound.setInteger("ticksUntilConsume", 2);
			}
			int ticksLeft = stack.stackTagCompound.getInteger("ticksUntilConsume");
			double lastX = lastMotions.get(entity.getEntityId()).left;
			double lastZ = lastMotions.get(entity.getEntityId()).right;
			if (ticksLeft <= 0) {
				if (Config.passiveDrain && (lastX != entity.posX || lastZ != entity.posZ)) {
					drainSteam(stack, 1);
				}
				ticksLeft = 2;
			}
			lastMotions.put(entity.getEntityId(), MutablePair.of(entity.posX, entity.posZ));

			ticksLeft--;
			stack.stackTagCompound.setInteger("ticksUntilConsume", ticksLeft);
			if (armor == 4) {
				if (entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getModifier(uuid) == null) {
					entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).applyModifier(exoBoost);
				}
				if (entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getModifier(uuid) == null) {
					entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).applyModifier(exoBoost);
				}
				if (!prevStep.containsKey(Integer.valueOf(entity.getEntityId()))) {
					prevStep.put(Integer.valueOf(entity.getEntityId()), Float.valueOf(entity.stepHeight));
				}
				entity.stepHeight = 1.0F;
			} else {
				removeGoodExoBoost(entity);
			}
		} else {
			removeGoodExoBoost(entity);
		}

		if (armor > 0 && !hasPower) {
			if (entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getModifier(uuid2) == null) {
				entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed).applyModifier(exoBoostBad);
			}
			if (entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getModifier(uuid2) == null) {
				entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).applyModifier(exoBoostBad);
			}
		} else {
			removeBadExoBoost(entity);
		}
	}

	/*
	@SubscribeEvent
	public void handleMobDrop(LivingDropsEvent event) {
		if (event.entityLiving instanceof EntityCreeper) {
			int gunpowder = 0;
			for (EntityItem drop : event.drops) {
				if (drop.getEntityItem().getItem() == Items.gunpowder) {
					gunpowder+=drop.getEntityItem().stackSize;
				}
			}
			if (gunpowder >= 2 && !event.entityLiving.worldObj.isRemote && event.entityLiving.worldObj.rand.nextBoolean()) {
				int dropsLeft = 2;
				ArrayList<EntityItem> dropsToRemove = new ArrayList<EntityItem>();
				EntityItem baseItem = null;
				for (EntityItem drop : event.drops) {
					if (baseItem == null && drop.getEntityItem().getItem() == Items.gunpowder) {
						baseItem = drop;
					}
					if (dropsLeft > 0 && drop.getEntityItem().getItem() == Items.gunpowder) {
						if (drop.getEntityItem().stackSize <= dropsLeft) {
							dropsLeft -= drop.getEntityItem().stackSize;
							dropsToRemove.add(drop);
						}
						else
						{
							drop.getEntityItem().stackSize -= dropsLeft;
							dropsLeft = 0;
						}
					}
				}
				for (EntityItem drop : dropsToRemove) {
					event.drops.remove(drop);
				}
				baseItem.setEntityItemStack(new ItemStack(SteamcraftItems.steamcraftCrafting,1,5));
                event.drops.add(baseItem);
			}
		}
	}
    */

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void handleFallDamage(LivingHurtEvent event) {
		if (CrossMod.BLOOD_MAGIC) {
			BloodMagicIntegration.handleAttack(event);
		}
		if (((event.entityLiving instanceof EntityPlayer)) && (event.source.damageType.equals("mob")) && (event.source.getEntity() != null) && (!event.entityLiving.worldObj.isRemote)) {
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			if (player.getHealth() <= 5.0F) {
				int vibrantLevel = 0;
				for (int i = 0; i < player.inventory.armorInventory.length; i++) {
					ItemStack armor = player.inventory.armorInventory[i];
					if (armor != null && armor.getItem() instanceof ItemExosuitArmor) {
						ItemExosuitArmor armorItem = (ItemExosuitArmor) armor.getItem();
						if (armorItem.hasPlates(armor) && UtilPlates.getPlate(armor.stackTagCompound.getString("plate")).getIdentifier() == "Vibrant") {
							vibrantLevel += 1;
						}
					}
				}

				if ((vibrantLevel > 0) && (player.worldObj.rand.nextInt(5 - vibrantLevel) == 0)) {
					int startRotation = player.worldObj.rand.nextInt(360);
					boolean foundSpot = false;
					int range = 14;
					int counter = 0;
					int yO = 2;
					int tX = 0;
					int tY = 0;
					int tZ = 0;
					int safeRange = 7;
					int safe = 0;
					while (!foundSpot && range < 28 && safe < 10000) {
						safe++;
						tX = (int) (player.posX + range * Math.sin(Math.toRadians(startRotation)));
						tZ = (int) (player.posZ + range * Math.cos(Math.toRadians(startRotation)));
						tY = (int) player.posY + yO;
						List mobs = player.worldObj.getEntitiesWithinAABB(EntityMob.class, AxisAlignedBB.getBoundingBox(tX + 0.5F - safeRange, tY + 0.5F - safeRange, tZ + 0.5F - safeRange, tX + 0.5F + safeRange, tY + 0.5F + safeRange, tZ + 0.5F + safeRange));
						if (mobs.size() == 0 && player.worldObj.isSideSolid(tX, tY - 1, tZ, ForgeDirection.UP) && !player.worldObj.isAnyLiquid(AxisAlignedBB.getBoundingBox(tX, tY - 1, tZ, tX, tY + 1, tZ)) && player.worldObj.isAirBlock(tX, tZ, tY) && player.worldObj.isAirBlock(tX, tZ, tY + 1)) {
							foundSpot = true;
						} else {
							if (counter >= 36) {
								if (yO > -2) {
									yO--;
									counter = 0;
								} else {
									counter = 0;
									yO = 2;
									range += 2;
								}
							} else {
								startRotation += 10;
								counter++;
							}
						}
					}

					if (foundSpot) {
						((EntityPlayerMP) player).playerNetServerHandler.setPlayerLocation(tX, tY, tZ, player.worldObj.rand.nextInt(360), player.rotationPitch);
					}
				}
			}
		}

		if (((event.entityLiving instanceof EntityPlayer)) && (event.source.damageType.equals("mob")) && (event.source.getEntity() != null) && (!event.entityLiving.worldObj.isRemote)) {
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			Entity mob = event.source.getEntity();
			int enderiumLevel = 0;
			for (int i = 0; i < player.inventory.armorInventory.length; i++) {
				ItemStack armor = player.inventory.armorInventory[i];
				if (armor != null && armor.getItem() instanceof ItemExosuitArmor) {
					ItemExosuitArmor armorItem = (ItemExosuitArmor) armor.getItem();
					if (armorItem.hasPlates(armor) && UtilPlates.getPlate(armor.stackTagCompound.getString("plate")).getIdentifier() == "Enderium") {
						enderiumLevel += 1;
					}
				}
			}
			if ((enderiumLevel > 0) && (player.worldObj.rand.nextFloat() < (enderiumLevel * 0.075F))) {
				int startRotation = player.worldObj.rand.nextInt(360);
				boolean foundSpot = false;
				int range = 8;
				int counter = 0;
				int yO = 2;
				int tX = 0;
				int tY = 0;
				int tZ = 0;
				int safe = 0;
				while (!foundSpot && range < 16 && safe < 10000) {
					safe++;
					tX = (int) (mob.posX + range * Math.sin(Math.toRadians(startRotation)));
					tZ = (int) (mob.posZ + range * Math.cos(Math.toRadians(startRotation)));
					tY = (int) mob.posY + yO;
					if (player.worldObj.isSideSolid(tX, tY - 1, tZ, ForgeDirection.UP) && !player.worldObj.isAnyLiquid(AxisAlignedBB.getBoundingBox(tX, tY - 1, tZ, tX, tY + 1, tZ)) && player.worldObj.isAirBlock(tX, tZ, tY) && player.worldObj.isAirBlock(tX, tZ, tY + 1)) {
						foundSpot = true;
					} else {
						if (counter >= 36) {
							if (yO > -2) {
								yO--;
								counter = 0;
							} else {
								counter = 0;
								yO = 2;
								range += 2;
							}
						} else {
							startRotation += 10;
							counter++;
						}
					}
				}

				if (foundSpot) {
					mob.setPositionAndRotation(tX, tY, tZ, mob.rotationYaw, mob.rotationPitch);
				}
			}
		}
		if (((event.entityLiving instanceof EntityPlayer)) && (event.source.damageType.equals("mob")) && (event.source.getEntity() != null)) {
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			int fireLevel = 0;
			for (int i = 0; i < player.inventory.armorInventory.length; i++) {
				ItemStack armor = player.inventory.armorInventory[i];
				if (armor != null && armor.getItem() instanceof ItemExosuitArmor) {
					ItemExosuitArmor armorItem = (ItemExosuitArmor) armor.getItem();
					if (armorItem.hasPlates(armor) && UtilPlates.getPlate(armor.stackTagCompound.getString("plate")).getIdentifier() == "Fiery") {
						fireLevel += 3;
					}
				}
			}
			if ((fireLevel > 0) && (player.worldObj.rand.nextInt(25) < fireLevel)) {
				event.source.getEntity().setFire(fireLevel / 2);
			}
		}
		if (((event.entityLiving instanceof EntityPlayer)) && (event.source.damageType.equals("mob")) &&
			(event.source.getEntity() != null) && ((event.source.getEntity() instanceof EntityLivingBase))) {
			EntityPlayer player = (EntityPlayer) event.entityLiving;
			int chillLevel = 0;
			for (int i = 0; i < player.inventory.armorInventory.length; i++) {
				ItemStack armor = player.inventory.armorInventory[i];
				if (armor != null && armor.getItem() instanceof ItemExosuitArmor) {
					ItemExosuitArmor armorItem = (ItemExosuitArmor) armor.getItem();
					if (armorItem.hasPlates(armor) && UtilPlates.getPlate(armor.stackTagCompound.getString("plate")).getIdentifier() == "Yeti") {
						chillLevel += 1;
					}
				}
			}
			if (chillLevel > 0) {
				((EntityLivingBase) event.source.getEntity()).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, chillLevel * 3 + 5, MathHelper.ceiling_float_int((float) chillLevel / 2F)));
			}
		}
		if (event.source == DamageSource.fall) {
			boolean hasPower = hasPower(event.entityLiving, (int) (event.ammount / Config.fallAssistDivisor));
			int armor = getExoArmor(event.entityLiving);
			EntityLivingBase entity = event.entityLiving;
			if (hasPower && entity.getEquipmentInSlot(3) != null && entity.getEquipmentInSlot(1) != null && entity.getEquipmentInSlot(1).getItem() instanceof ItemExosuitArmor) {
				ItemExosuitArmor boots = (ItemExosuitArmor) entity.getEquipmentInSlot(1).getItem();
				if (boots.hasUpgrade(entity.getEquipmentInSlot(1), SteamcraftItems.fallAssist)) {
					if (event.ammount <= 6.0F) {
						event.ammount = 0.0F;
					}
					event.ammount = event.ammount / 3.0F;
					drainSteam(entity.getEquipmentInSlot(3), (int) (event.ammount / Config.fallAssistDivisor));
					if (event.ammount == 0.0F) {
						event.setResult(Event.Result.DENY);
						event.setCanceled(true);
					}
				}
			}
		}
		if (((event.entity instanceof EntityPlayer)) && (((EntityPlayer) event.entity).inventory.armorItemInSlot(1) != null) && (((EntityPlayer) event.entity).inventory.armorItemInSlot(1).getItem() instanceof ItemExosuitArmor)) {
			ItemStack stack = ((EntityPlayer) event.entity).inventory.armorItemInSlot(1);
			ItemExosuitArmor item = (ItemExosuitArmor) stack.getItem();
			//if (item.hasUpgrade(stack, SteamcraftItems.doubleJump)) {
			float amount = event.ammount;
			EntityPlayer player = ((EntityPlayer) event.entity);
			DamageSource src = event.source;
			if (!player.isEntityInvulnerable()) {
				if (amount <= 0) return;
				if (!src.isUnblockable() && player.isBlocking() && amount > 0.0F) {
					amount = (1.0F + amount) * 0.5F;
				}

				amount = ISpecialArmor.ArmorProperties.ApplyArmor(player, player.inventory.armorInventory, src, amount);
				if (amount <= 0) return;
				float f1 = amount;
				amount = Math.max(amount - player.getAbsorptionAmount(), 0.0F);
			}
			if (amount > 0.0F) {
				//				stack.stackTagCompound.setFloat("damageAmount", amount);
				//				stack.stackTagCompound.setInteger("aidTicks", 100);

			}
			//}
		}
	}

	@SubscribeEvent
	public void handleFlippers(LivingEvent.LivingUpdateEvent event) {
		int armor = getExoArmor(event.entityLiving);
		EntityLivingBase entity = event.entityLiving;
		boolean hasPower = hasPower(entity, 1);

		if (entity.getEquipmentInSlot(3) != null && entity.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor) {
			ItemExosuitArmor chest = (ItemExosuitArmor) entity.getEquipmentInSlot(3).getItem();
			if (chest.hasUpgrade(entity.getEquipmentInSlot(3), SteamcraftItems.pitonDeployer)) {
				if (entity.getEquipmentInSlot(3).stackTagCompound.hasKey("grappled") && entity.getEquipmentInSlot(3).stackTagCompound.getBoolean("grappled")) {

					double lastX = entity.getEquipmentInSlot(3).stackTagCompound.getFloat("x");
					double lastY = entity.getEquipmentInSlot(3).stackTagCompound.getFloat("y");
					double lastZ = entity.getEquipmentInSlot(3).stackTagCompound.getFloat("z");
					int blockX = entity.getEquipmentInSlot(3).stackTagCompound.getInteger("blockX");
					int blockY = entity.getEquipmentInSlot(3).stackTagCompound.getInteger("blockY");
					int blockZ = entity.getEquipmentInSlot(3).stackTagCompound.getInteger("blockZ");

					if ((Math.abs(lastX - entity.posX) > 0.1F || Math.abs(lastZ - entity.posZ) > 0.1F || entity.isSneaking() || entity.worldObj.isAirBlock(blockX, blockY, blockZ))) {
						entity.getEquipmentInSlot(3).stackTagCompound.setBoolean("grappled", false);
					} else {
						entity.motionX = 0.0F;
						entity.motionY = (entity.motionY > 0) ? entity.motionY : 0.0F;
						entity.motionZ = 0.0F;
					}
				}
			}
		}

		if (((event.entity instanceof EntityPlayer)) && (((EntityPlayer) event.entity).inventory.armorItemInSlot(1) != null) && (((EntityPlayer) event.entity).inventory.armorItemInSlot(1).getItem() instanceof ItemExosuitArmor)) {
			ItemStack stack = ((EntityPlayer) event.entity).inventory.armorItemInSlot(1);
			ItemExosuitArmor item = (ItemExosuitArmor) stack.getItem();
            /*
            if (item.hasUpgrade(stack, SteamcraftItems.doubleJump)) {
				if (!stack.stackTagCompound.hasKey("aidTicks")) {
					stack.stackTagCompound.setInteger("aidTicks", -1);
				}
				int aidTicks = stack.stackTagCompound.getInteger("aidTicks");

				if (aidTicks > 0) {
					aidTicks--;
				}
				if (aidTicks == 0) {
					if (!stack.stackTagCompound.hasKey("ticksNextHeal")) {
						stack.stackTagCompound.setInteger("ticksNextHeal", 0);
					}
					float damageAmount = stack.stackTagCompound.getInteger("damageAmount");
					int ticksNextHeal = stack.stackTagCompound.getInteger("ticksNextHeal");
					if (ticksNextHeal > 0) {
						ticksNextHeal--;
					}
					if (ticksNextHeal == 0) {
						//event.entityLiving.heal(1.0F);
						damageAmount -=1.0F;
						stack.stackTagCompound.setFloat("damageAmount", damageAmount);
						ticksNextHeal=5;
					}
					if (damageAmount == 0.0F) {
						aidTicks = -1;
					}
					stack.stackTagCompound.setInteger("ticksNextHeal", ticksNextHeal);
       			}
				stack.stackTagCompound.setInteger("aidTicks", aidTicks);
            }
            */
		}

		if (!event.entity.worldObj.isRemote && ((event.entity instanceof EntityPlayer)) && (((EntityPlayer) event.entity).onGround)) {
			if (isJumping.containsKey(event.entity.getEntityId())) {
				isJumping.put(event.entity.getEntityId(), Math.max(0, isJumping.get(event.entity.getEntityId()) - 1));
				((EntityPlayer) entity).fallDistance = 0.1F;
			}
		} else if (!event.entity.worldObj.isRemote && ((event.entity instanceof EntityPlayer)) && (((EntityPlayer) event.entity).fallDistance == 0.0F)) {
			if (isJumping.containsKey(event.entity.getEntityId())) {
				isJumping.put(event.entity.getEntityId(), Math.max(0, isJumping.get(event.entity.getEntityId()) - 1));
				((EntityPlayer) entity).fallDistance = 0.1F;
			}
		}

		if (((event.entity instanceof EntityPlayer)) && (((EntityPlayer) event.entity).inventory.armorItemInSlot(0) != null) && (((EntityPlayer) event.entity).inventory.armorItemInSlot(0).getItem() instanceof ItemExosuitArmor)) {
			ItemStack stack = ((EntityPlayer) event.entity).inventory.armorItemInSlot(0);
			ItemExosuitArmor item = (ItemExosuitArmor) stack.getItem();
			if (item.hasUpgrade(stack, SteamcraftItems.doubleJump) && event.entity.onGround) {
				stack.stackTagCompound.setBoolean("usedJump", false);
			}
		}
		if (entity.getEquipmentInSlot(3) != null && entity.getEquipmentInSlot(3).getItem() instanceof ItemExosuitArmor) {
			ItemExosuitArmor chest = (ItemExosuitArmor) entity.getEquipmentInSlot(3).getItem();
			if (chest.hasUpgrade(entity.getEquipmentInSlot(3), SteamcraftItems.wings)) {
				if (entity.fallDistance > 1.5F && !entity.isSneaking()) {
					entity.fallDistance = 1.5F;
					entity.motionY = Math.max(entity.motionY, -0.1F);
					entity.moveEntity(entity.motionX, 0, entity.motionZ);
				}
			}
		}

		if (hasPower && entity.getEquipmentInSlot(2) != null && entity.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
			ItemExosuitArmor leggings = (ItemExosuitArmor) entity.getEquipmentInSlot(2).getItem();
			if (leggings.hasUpgrade(entity.getEquipmentInSlot(2), SteamcraftItems.thrusters)) {
				if (!lastMotions.containsKey(entity.getEntityId())) {
					lastMotions.put(entity.getEntityId(), MutablePair.of(entity.posX, entity.posZ));
				}
				double lastX = lastMotions.get(entity.getEntityId()).left;
				double lastZ = lastMotions.get(entity.getEntityId()).right;
				if ((lastX != entity.posX || lastZ != entity.posZ) && !entity.onGround && !entity.isInWater() && (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isFlying)) {
					entity.moveEntity(entity.motionX, 0, entity.motionZ);
					if (!event.entityLiving.getEquipmentInSlot(3).stackTagCompound.hasKey("ticksUntilConsume")) {
						event.entityLiving.getEquipmentInSlot(3).stackTagCompound.setInteger("ticksUntilConsume", 2);
					}
					if (event.entityLiving.getEquipmentInSlot(3).stackTagCompound.getInteger("ticksUntilConsume") <= 0) {
						drainSteam(event.entityLiving.getEquipmentInSlot(3), Config.thrusterConsumption);
					}
				}
			}
		}

		if (hasPower && entity.getEquipmentInSlot(2) != null && entity.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
			ItemExosuitArmor leggings = (ItemExosuitArmor) entity.getEquipmentInSlot(2).getItem();
			if (leggings.hasUpgrade(entity.getEquipmentInSlot(2), SteamcraftItems.runAssist)) {
				if (!lastMotions.containsKey(entity.getEntityId())) {
					lastMotions.put(entity.getEntityId(), MutablePair.of(entity.posX, entity.posZ));
				}
				double lastX = lastMotions.get(entity.getEntityId()).left;
				double lastZ = lastMotions.get(entity.getEntityId()).right;
				if ((entity.moveForward > 0.0F) && (lastX != entity.posX || lastZ != entity.posZ) && entity.onGround && !entity.isInWater()) {
					entity.moveFlying(0.0F, 1.0F, 0.075F);
					if (!event.entityLiving.getEquipmentInSlot(3).stackTagCompound.hasKey("ticksUntilConsume")) {
						event.entityLiving.getEquipmentInSlot(3).stackTagCompound.setInteger("ticksUntilConsume", 2);
					}
					if (event.entityLiving.getEquipmentInSlot(3).stackTagCompound.getInteger("ticksUntilConsume") <= 0) {
						drainSteam(event.entityLiving.getEquipmentInSlot(3), Config.runAssistConsumption);
					}
				}
			}
		}
        /*
		if (hasPower(entity,100) && entity.getEquipmentInSlot(2) != null && entity.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor && !entity.worldObj.isRemote) {
			ItemExosuitArmor leggings = (ItemExosuitArmor) entity.getEquipmentInSlot(2).getItem();
			if (leggings.hasUpgrade(entity.getEquipmentInSlot(2), SteamcraftItems.antiFire)) {
				if (entity.isBurning()) {

					event.entityLiving.getEquipmentInSlot(3).damageItem(10, event.entityLiving);
					if (entity.worldObj.isAirBlock((int)entity.posX, (int)entity.posY, (int)entity.posZ) || entity.worldObj.getBlock((int)entity.posX, (int)entity.posY, (int)entity.posZ).isReplaceable(entity.worldObj, (int)entity.posX, (int)entity.posY, (int)entity.posZ) || entity.worldObj.getBlock((int)entity.posX, (int)entity.posY, (int)entity.posZ) == Blocks.fire) {

						entity.worldObj.setBlock((int)entity.posX, (int)entity.posY, (int)entity.posZ, Blocks.water, 1, 1);
					}
				}
			}
		}
        */
	}

	@SuppressWarnings("unchecked")
	@SubscribeEvent
	public void updateVillagers(LivingEvent.LivingUpdateEvent event) {
		if (event.entityLiving instanceof EntityVillager) {
			EntityVillager villager = (EntityVillager) event.entityLiving;
			Integer timeUntilReset = ReflectionHelper.getPrivateValue(EntityVillager.class, villager, 6);
			String lastBuyingPlayer = ReflectionHelper.getPrivateValue(EntityVillager.class, villager, 9);
			if (!villager.isTrading() && timeUntilReset == 39 && lastBuyingPlayer != null) {
				EntityPlayer player = villager.worldObj.getPlayerEntityByName(lastBuyingPlayer);
				if (player != null) {
					if (player.inventory.armorInventory[3] != null && (player.inventory.armorInventory[3].getItem() == SteamcraftItems.tophat)) {
						ItemStack hat = player.inventory.armorInventory[3];
						if (!hat.hasTagCompound()) {
							hat.setTagCompound(new NBTTagCompound());
						}
						if (!hat.stackTagCompound.hasKey("level")) {
							hat.stackTagCompound.setInteger("level", 0);
						}
						int level = hat.stackTagCompound.getInteger("level");
						level++;
						hat.stackTagCompound.setInteger("level", level);
					} else if (player.inventory.armorInventory[3] != null && player.inventory.armorInventory[3].getItem() == SteamcraftItems.exoArmorHead && ((ItemExosuitArmor) player.inventory.armorInventory[3].getItem()).hasUpgrade(player.inventory.armorInventory[3], SteamcraftItems.tophat)) {
						ItemStack hat = ((ItemExosuitArmor) player.inventory.armorInventory[3].getItem()).getStackInSlot(player.inventory.armorInventory[3], 3);
						if (!hat.hasTagCompound()) {
							hat.setTagCompound(new NBTTagCompound());
						}
						if (!hat.stackTagCompound.hasKey("level")) {
							hat.stackTagCompound.setInteger("level", 0);
						}
						int level = hat.stackTagCompound.getInteger("level");
						level++;
						hat.stackTagCompound.setInteger("level", level);
						((ItemExosuitArmor) player.inventory.armorInventory[3].getItem()).setInventorySlotContents(player.inventory.armorInventory[3], 3, hat);
					}
				}
			}
		}
		if (event.entityLiving instanceof EntityVillager && event.entityLiving.worldObj.isRemote == false) {
			EntityVillager villager = (EntityVillager) event.entityLiving;
			if (!lastHadCustomer.containsKey(villager.getEntityId())) {
				lastHadCustomer.put(villager.getEntityId(), false);
			}
			boolean hadCustomer = lastHadCustomer.get(villager.getEntityId());
			boolean hasCustomer = false;
			if (villager.getCustomer() != null && villager.getCustomer().inventory.armorInventory[3] != null && (villager.getCustomer().inventory.armorInventory[3].getItem() == SteamcraftItems.tophat
				|| (villager.getCustomer().inventory.armorInventory[3].getItem() == SteamcraftItems.exoArmorHead && ((ItemExosuitArmor) villager.getCustomer().inventory.armorInventory[3].getItem()).hasUpgrade(villager.getCustomer().inventory.armorInventory[3], SteamcraftItems.tophat)))) {
				EntityPlayer customer = villager.getCustomer();
				hasCustomer = true;

				if (!hadCustomer) {
					MerchantRecipeList recipeList = ReflectionHelper.getPrivateValue(EntityVillager.class, villager, 5);
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
					ReflectionHelper.setPrivateValue(EntityVillager.class, villager, recipeList, 5);
					//customer.closeScreen();
					//customer.displayGUIMerchant(villager, villager.getCustomNameTag());
				}
			}

			if (!hasCustomer && hadCustomer) {
				MerchantRecipeList recipeList = ReflectionHelper.getPrivateValue(EntityVillager.class, villager, 5);
				if (recipeList != null) {
					for (Object obj : recipeList) {
						MerchantRecipe recipe = (MerchantRecipe) obj;
						if (recipe.getItemToSell().stackSize > 1 && recipe.getItemToSell().stackSize != MathHelper.ceiling_float_int(recipe.getItemToSell().stackSize / 1.25F)) {
							recipe.getItemToSell().stackSize = MathHelper.ceiling_float_int(recipe.getItemToSell().stackSize / 1.25F);
						} else if (recipe.getItemToBuy().stackSize > 1 && recipe.getItemToBuy().stackSize != MathHelper.floor_float(recipe.getItemToBuy().stackSize * 1.25F)) {
							recipe.getItemToBuy().stackSize = MathHelper.floor_float(recipe.getItemToBuy().stackSize * 1.25F);
						} else if (recipe.getSecondItemToBuy() != null && recipe.getSecondItemToBuy().stackSize > 1 && recipe.getSecondItemToBuy().stackSize != MathHelper.floor_float(recipe.getSecondItemToBuy().stackSize * 1.25F)) {
							recipe.getSecondItemToBuy().stackSize = MathHelper.floor_float(recipe.getSecondItemToBuy().stackSize * 1.25F);
						}
					}
				}
				ReflectionHelper.setPrivateValue(EntityVillager.class, villager, recipeList, 5);
			}

			lastHadCustomer.remove(villager.getEntityId());
			lastHadCustomer.put(villager.getEntityId(), hasCustomer);
		}
	}

	@SubscribeEvent
	public void hideCloakedPlayers(LivingSetAttackTargetEvent event) {
		if (event.entityLiving instanceof EntityLiving) {
			EntityLiving entity = (EntityLiving) event.entityLiving;
			if (event.target != null) {
				if (event.target.getEquipmentInSlot(2) != null && event.target.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
					ItemExosuitArmor leggings = (ItemExosuitArmor) event.target.getEquipmentInSlot(2).getItem();
					if (leggings.hasUpgrade(event.target.getEquipmentInSlot(2), SteamcraftItems.stealthUpgrade)) {
						IAttributeInstance iattributeinstance = entity.getEntityAttribute(SharedMonsterAttributes.followRange);
						double d0 = iattributeinstance == null ? 16.0D : iattributeinstance.getAttributeValue();
						d0 = d0 / 1.5D;
						List list = entity.worldObj.getEntitiesWithinAABB(Entity.class, entity.boundingBox.expand(d0, 4.0D, d0));
						boolean foundPlayer = false;
						for (Object mob : list) {
							Entity ent = (Entity) mob;
							if (ent == event.target) {
								foundPlayer = true;
							}
						}
						if (!foundPlayer) {
							entity.setAttackTarget(null);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void hideCloakedPlayers(LivingEvent.LivingUpdateEvent event) {
		if (event.entityLiving instanceof EntityLiving) {
			EntityLiving entity = (EntityLiving) event.entityLiving;
			if (entity.getAttackTarget() != null) {
				if (entity.getAttackTarget().getEquipmentInSlot(2) != null && entity.getAttackTarget().getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
					ItemExosuitArmor leggings = (ItemExosuitArmor) entity.getAttackTarget().getEquipmentInSlot(2).getItem();
					if (leggings.hasUpgrade(entity.getAttackTarget().getEquipmentInSlot(2), SteamcraftItems.stealthUpgrade)) {
						IAttributeInstance iattributeinstance = entity.getEntityAttribute(SharedMonsterAttributes.followRange);
						double d0 = iattributeinstance == null ? 16.0D : iattributeinstance.getAttributeValue();
						d0 = d0 / 1.5D;
						List list = entity.worldObj.getEntitiesWithinAABB(Entity.class, entity.boundingBox.expand(d0, 4.0D, d0));
						boolean foundPlayer = false;
						for (Object mob : list) {
							Entity ent = (Entity) mob;
							if (ent == entity.getAttackTarget()) {
								foundPlayer = true;
							}
						}
						if (!foundPlayer) {
							entity.setAttackTarget(null);
						}
					}

				}
			}
		}
	}
}
