package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import flaxbeard.steamcraft.Steamcraft;
import flaxbeard.steamcraft.SteamcraftItems;
import flaxbeard.steamcraft.api.ISteamChargable;
import flaxbeard.steamcraft.api.SteamcraftRegistry;
import flaxbeard.steamcraft.api.exosuit.UtilPlates;
import flaxbeard.steamcraft.gui.GuiSteamcraftBook;
import flaxbeard.steamcraft.integration.CrossMod;
import flaxbeard.steamcraft.integration.EnchiridionIntegration;
import flaxbeard.steamcraft.item.ItemExosuitArmor;
import flaxbeard.steamcraft.item.ItemSteamcraftBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GeneralHandler {
    public static final HashMap<Object, EventBus> handlers = new HashMap<>();
    static {
        addForgeBus(new CanisterHandler());
        addForgeBus(new EntityHandler());
        addForgeBus(new GeneralHandler());
        addForgeBus(new GuiEventHandler());
        addForgeBus(new LivingHandler());
        addForgeBus(new PlayerHandler());
        addForgeBus(new RenderHandler());
        addForgeBus(new WorldHandler());

        addFMLBus(new TickHandler());
    }

    private static void addForgeBus(Object o) {
        handlers.put(o, MinecraftForge.EVENT_BUS);
    }

    private static void addFMLBus(Object o) {
        handlers.put(o, FMLCommonHandler.instance().bus());
    }

    /*
    public void renderTexture(int screenX, int screenY, int screenEndX, int screenEndY, double startU, double startV, double endU, double endV) {
        int zLevel = 1;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV((double) (screenX + 0), (double) (screenY + screenEndY), (double) zLevel, startU,
          endV);
        tessellator.addVertexWithUV((double) (screenX + screenEndX), (double) (screenY + screenEndY), (double) zLevel,
          endU, endV);
        tessellator.addVertexWithUV((double) (screenX + screenEndX), (double) (screenY + 0), (double) zLevel, endU,
          startV);
        tessellator.addVertexWithUV((double) (screenX + 0), (double) (screenY + 0), (double) zLevel, startU, startV);
        tessellator.draw();
    }
    */

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public void plateTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.itemStack;
        if (UtilPlates.getPlate(stack) != null) {
            event.toolTip.add(EnumChatFormatting.BLUE + StatCollector.translateToLocal("steamcraft.plate.bonus") + UtilPlates.getPlate(stack).effect());
        }
        if (stack.hasTagCompound()) {
            if (stack.stackTagCompound.hasKey("canned")) {
                event.toolTip.add(EnumChatFormatting.GOLD + StatCollector.translateToLocal("steamcraft.canned"));
            }
        }
        if (stack.getItem() instanceof ItemExosuitArmor || stack.getItem() instanceof ISteamChargable) {
            ArrayList<String> linesToRemove = new ArrayList<String>();
            for (String str : event.toolTip) {
                if (str == "") {
                    linesToRemove.add(str);
                }
                if (str.contains("+")) {
                    linesToRemove.add(str);
                }
                if (str.contains("/") && !str.contains("SU")) {
                    linesToRemove.add(str);
                }
            }
            for (String str : linesToRemove) {
                if (str.contains("+") && !str.contains("+0.25")) {
                    event.toolTip.remove(str);
                    event.toolTip.add(1, str);
                } else {
                    event.toolTip.remove(str);
                }
            }
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiContainer) {

            for (ItemStack stack2 : SteamcraftRegistry.bookRecipes.keySet()) {
                if (stack2.getItem() == stack.getItem() && (stack2.getItemDamage() == stack.getItemDamage() || stack.getItem() instanceof ItemArmor || stack.getItem() instanceof ItemTool)) {
                    boolean foundBook = CrossMod.ENCHIRIDION ? EnchiridionIntegration.hasBook(SteamcraftItems.book, player) : false;
                    for (int p = 0; p < player.inventory.getSizeInventory(); p++) {
                        if (player.inventory.getStackInSlot(p) != null && player.inventory.getStackInSlot(p).getItem() instanceof ItemSteamcraftBook) {
                            foundBook = true;
                            break;
                        }
                    }
                    if (foundBook) {
                        event.toolTip.add(EnumChatFormatting.ITALIC + "" + EnumChatFormatting.GRAY + StatCollector.translateToLocal("steamcraft.book.shiftright"));
                        if (Mouse.isButtonDown(0) && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                            player.openGui(Steamcraft.instance, 1, player.worldObj, 0, 0, 0);
                            GuiSteamcraftBook.viewing = SteamcraftRegistry.bookRecipes.get(stack2).left;
                            GuiSteamcraftBook.currPage = MathHelper.floor_float((float) SteamcraftRegistry.bookRecipes.get(stack2).right / 2.0F);
                            GuiSteamcraftBook.lastIndexPage = 1;
                            GuiSteamcraftBook.bookTotalPages = MathHelper.ceiling_float_int(SteamcraftRegistry.researchPages.get(GuiSteamcraftBook.viewing).length / 2F);
                            ((GuiSteamcraftBook) Minecraft.getMinecraft().currentScreen).updateButtons();
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void handleEnhancement(AnvilUpdateEvent event) {
        /*
		if (event.right.getItem() instanceof IEnhancement) {
			IEnhancement enhancement = (IEnhancement) event.right.getItem();
			if (enhancement.canApplyTo(event.left) && UtilEnhancements.canEnhance(event.left)) {
				event.cost = enhancement.cost(event.left);
				event.output = UtilEnhancements.getEnhancedItem(event.left, event.right);
			}
		}
        */
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void muffleSounds(PlaySoundEvent17 event) {
        if (event.name.contains("step")) {
            float x = event.sound.getXPosF();
            float y = event.sound.getYPosF();
            float z = event.sound.getZPosF();
            List entities = Minecraft.getMinecraft().thePlayer.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(x - 0.5F, y - 0.5F, z - 0.5F, x + 0.5F, y + 0.5F, z + 0.5F));
            for (Object obj : entities) {
                EntityLivingBase entity = (EntityLivingBase) obj;
                if (entity.getEquipmentInSlot(2) != null && entity.getEquipmentInSlot(2).getItem() instanceof ItemExosuitArmor) {
                    ItemExosuitArmor leggings = (ItemExosuitArmor) entity.getEquipmentInSlot(2).getItem();
                    if (leggings.hasUpgrade(entity.getEquipmentInSlot(2), SteamcraftItems.stealthUpgrade)) {
                        event.result = null;
                    }
                }
            }
        }
    }
}
