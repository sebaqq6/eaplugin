package pl.eadventure.plugin.Events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.print;

public class playerPlaceBlock implements Listener {
	@EventHandler
	public void onPlayerPlaceBlock(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		PlayerData pd = PlayerData.get(e.getPlayer());
		if (pd.creativeMode) {
			Location blockLocation = e.getBlockPlaced().getLocation();
			int bPosX = blockLocation.getBlockX();
			int bPosY = blockLocation.getBlockY();
			int bPosZ = blockLocation.getBlockZ();
			World bWorld = blockLocation.getWorld();
			String blockType = e.getBlockPlaced().getType().toString();
			if (pd.creativeLastPlacedPos != null) {
				if (!pd.creativeLastPlacedPos.getWorld().equals(blockLocation.getWorld())) {//placed on other world
					String info = String.format("[ADMIN] %s postawił/a blok creative w innym świecie niż ostatnio" +
							": (%s) %d, %d, %d, %s.", player.getName(), blockType, bPosX, bPosY, bPosZ, bWorld.getName());
					Bukkit.getConsoleSender().sendMessage(info);
				} else {//same world
					if (pd.creativeLastPlacedPos.distance(e.getBlockPlaced().getLocation()) > 20.0) {
						String info = String.format("[ADMIN] %s postawił/a blok creative dość daleko od ostatniego " +
								"bloku: (%s) %d, %d, %d, %s.", player.getName(), blockType, bPosX, bPosY, bPosZ, bWorld.getName());
						Bukkit.getConsoleSender().sendMessage(info);
					}
				}

			} else {//first block
				String info = String.format("[ADMIN] %s postawił/a blok creative (pierwszy blok): " +
						"(%s) %d, %d, %d, %s.", player.getName(), blockType, bPosX, bPosY, bPosZ, bWorld.getName());
				Bukkit.getConsoleSender().sendMessage(info);
			}
			pd.creativeLastPlacedPos = blockLocation;
		}
	}
}
