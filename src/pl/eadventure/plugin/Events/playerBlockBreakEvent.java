package pl.eadventure.plugin.Events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import pl.eadventure.plugin.PlayerData;

public class playerBlockBreakEvent implements Listener {
	@EventHandler
	public void onPlayerBreakBlock(BlockBreakEvent e) {
		Player player = e.getPlayer();
		PlayerData pd = PlayerData.get(e.getPlayer());
		if (pd.creativeMode) {
			Location blockLocation = e.getBlock().getLocation();
			int bPosX = blockLocation.getBlockX();
			int bPosY = blockLocation.getBlockY();
			int bPosZ = blockLocation.getBlockZ();
			World bWorld = blockLocation.getWorld();
			String blockType = e.getBlock().getType().toString();
			if (pd.creativeLastBreakPos != null) {
				if (!pd.creativeLastBreakPos.getWorld().equals(blockLocation.getWorld())) {//placed on other world
					String info = String.format("[ADMIN] %s zniszczył/a blok creative w innym świecie niż ostatnio" +
							": (%s) %d, %d, %d, %s.", player.getName(), blockType, bPosX, bPosY, bPosZ, bWorld.getName());
					Bukkit.getConsoleSender().sendMessage(info);
				} else {//same world
					if (pd.creativeLastBreakPos.distance(e.getBlock().getLocation()) > 20.0) {
						String info = String.format("[ADMIN] %s zniszczył/a blok creative dość daleko od ostatniego " +
								"bloku: (%s) %d, %d, %d, %s.", player.getName(), blockType, bPosX, bPosY, bPosZ, bWorld.getName());
						Bukkit.getConsoleSender().sendMessage(info);
					}
				}

			} else {//first block
				String info = String.format("[ADMIN] %s zniszczył/a blok creative (pierwszy blok): " +
						"(%s) %d, %d, %d, %s.", player.getName(), blockType, bPosX, bPosY, bPosZ, bWorld.getName());
				Bukkit.getConsoleSender().sendMessage(info);
			}
			pd.creativeLastBreakPos = blockLocation;
		}
	}
}
