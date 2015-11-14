package flaxbeard.steamcraft.handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import flaxbeard.steamcraft.api.steamnet.SteamNetworkRegistry;
import flaxbeard.steamcraft.api.steamnet.data.SteamNetworkData;
import net.minecraftforge.event.world.WorldEvent;

public class WorldHandler {
	@SubscribeEvent
	public void handleWorldLoad(WorldEvent.Load event) {
		if (!event.world.isRemote) {
			SteamNetworkData.get(event.world);
			SteamNetworkRegistry.initialize();
		}

	}
}
