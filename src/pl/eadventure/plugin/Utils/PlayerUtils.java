package pl.eadventure.plugin.Utils;

import fr.skytasul.glowingentities.GlowingBlocks;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerUtils {
	//other
	static Map<UUID, ItemStack[]> items = new HashMap<UUID, ItemStack[]>();
	static Map<UUID, ItemStack[]> armor = new HashMap<UUID, ItemStack[]>();

	public static void sendColorMessage(Player player, String message) {
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	public static void sendColorMessageToAll(String message) {
		Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	public static int getInventoryItemsCount(Player player) {
		int count = 0;
		for (ItemStack item : player.getInventory().getContents()) {
			// if (item == null || item.getType() != Material.RAW_CHICKEN) continue;
			if (item == null)
				continue;
			count += item.getAmount();
		}
		return count;
	}

	public static int getItemCount(Player player, ItemStack item) {
		int count = 0;
		ItemStack[] contents = player.getInventory().getContents();
		for (ItemStack stack : contents) {
			if (stack != null && stack.isSimilar(item)) {
				count += stack.getAmount();
			}
		}
		return count;
	}

	public static int getArmorSetCount(Player player) {
		int count = 0;
		for (ItemStack item : player.getInventory().getArmorContents()) {
			if (item == null)
				continue;
			count += item.getAmount();
		}
		return count;
	}

	public static void storeAndClearInventory(Player player) {
		UUID uuid = player.getUniqueId();

		ItemStack[] contents = player.getInventory().getContents();
		ItemStack[] armorContents = player.getInventory().getArmorContents();

		items.put(uuid, contents);
		armor.put(uuid, armorContents);

		player.getInventory().clear();

		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
	}

	public static void restoreInventory(Player player) {
		UUID uuid = player.getUniqueId();

		ItemStack[] contents = items.get(uuid);
		ItemStack[] armorContents = armor.get(uuid);

		if (contents != null) {
			player.getInventory().setContents(contents);
		} else {// if the player has no inventory contents, clear their inventory
			player.getInventory().clear();
		}

		if (armorContents != null) {
			player.getInventory().setArmorContents(armorContents);
		} else {// if the player has no armor, set the armor to null
			player.getInventory().setHelmet(null);
			player.getInventory().setChestplate(null);
			player.getInventory().setLeggings(null);
			player.getInventory().setBoots(null);
		}
	}

	//return array 3 ints 0 - hours, 1 - minutes, 3-secons
	public static int[] getTimePlayedFromStatistic(Player player) {
		int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
		return Utils.convertTicksToTime(ticks);
	}

	//Causes lag, use only in async thread
	public static boolean hasOfflinePlayerPermission(UUID playerUUID, String permission) {
		LuckPerms luckPerms = EternalAdventurePlugin.getLuckPerms();
		User user = luckPerms.getUserManager().loadUser(playerUUID).join();//lag...
		if (user != null) {
			return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
		} else {
			return false;
		}
	}

	public static boolean isAdminPermissionHasHigher(String playerName, String targetName) {
		Player player = Bukkit.getPlayer(playerName);
		if (player != null) {
			String[] permissionsToCheck = {"eadventureplugin.apanel", "eadventureplugin.mpanel", "eadventureplugin.gpanel", "eadventureplugin.spanel"};

			String playerPermission = null;
			for (String permission : permissionsToCheck) {
				if (player.hasPermission(permission)) {
					playerPermission = permission;
					break;
				}
			}

			String targetPermission = null;
			Player targetOnline = Bukkit.getPlayer(targetName);
			if (targetOnline == null) {//if player is offline
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
				if(!offlinePlayer.hasPlayedBefore()) return false;
				for (String permission : permissionsToCheck) {
					if (hasOfflinePlayerPermission(offlinePlayer.getUniqueId(), permission)) {
						targetPermission = permission;
						break;
					}
				}
			} else {//if player is online
				for (String permission : permissionsToCheck) {
					if (targetOnline.hasPermission(permission)) {
						targetPermission = permission;
						break;
					}
				}
			}
			return playerPermission != null && targetPermission != null && Utils.isAdminPermissionHigherThan(playerPermission, targetPermission);
		}
		return false;
	}

	public static void glowBlock(Block block, Player receiver, ChatColor color, long timeTick) {
		GlowingBlocks gb = EternalAdventurePlugin.getGlowningBlockAPI();
		try {
			gb.setGlowing(block, receiver, color);
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						gb.unsetGlowing(block, receiver);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}
			}.runTaskLater(EternalAdventurePlugin.getInstance(), timeTick);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
