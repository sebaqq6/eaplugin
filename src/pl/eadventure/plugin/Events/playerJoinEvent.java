package pl.eadventure.plugin.Events;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.Modules.ServerLogManager;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

public class playerJoinEvent implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		print.debug("Gracz: " + player.getName() + " dołączył na serwer.");
		PlayerData pd = PlayerData.get(player);

		new BukkitRunnable() {
			@Override
			public void run() {
				// String clientBrand = player.getClientBrandName()
				String clientBrand = PlaceholderAPI.setPlaceholders(player, "%vulcan_client_brand%");
				String logIP = player.getAddress().getAddress().getHostAddress();
				String clientBrandPaper = player.getClientBrandName();
				if (player.getName().equalsIgnoreCase("JrDesmond")
						|| player.getName().equalsIgnoreCase("JrRequeim")
						|| player.getName().equalsIgnoreCase("MsKarolsa")) {
					logIP = "Ukryte";
				}
				if (clientBrand.equalsIgnoreCase("Unresolved")) {
					pd.clientBrand = clientBrandPaper;
				}
				String strLog = String.format("%s dołączył/a do serwera. Klient: %s | IP: %s", player.getName(), clientBrand, logIP);
				ServerLogManager.log(strLog, ServerLogManager.LogType.JoinLeave);
				print.debug(strLog);
				if (gVar.antiBot) {
					if (clientBrand.equalsIgnoreCase("Unresolved") || clientBrand.equalsIgnoreCase("Fabric")) {//Bad client brand, bot?
						print.error(String.format("'%s' by Vulcan. '%s' by paper.", clientBrand, clientBrandPaper));
						if (player.isOnline()) {
							if (pd.onlineHours < 1) {//Temp fix
								//PunishmentSystem.notifyMessage(PunishmentSystem.LogType.BAN, player.getName(), "AntiBot", "BOT -s", -1);
								PunishmentSystem.banPlayer(player.getName(), "AntiBot", 1, PunishmentSystem.BanType.NICK_UUID_IP, "Podejrzenie bota. Spróbuj za minutę.");
							}
						}
					}
				}

				/*else if(clientBrand.length() < 2) {
					if(player.isOnline()) {
						player.kickPlayer("Twoje połączenie nie zostało poprawnie zweryfikowane.\nDołącz ponownie!");
					}
				}*/
			}
		}.runTaskLater(EternalAdventurePlugin.getInstance(), 20L * 3);

		if (!pd.creativeMode && player.getGameMode() == GameMode.CREATIVE) {
			player.getInventory().clear();
			player.getInventory().setHelmet(null);
			player.getInventory().setChestplate(null);
			player.getInventory().setLeggings(null);
			player.getInventory().setBoots(null);
			player.setGameMode(GameMode.SURVIVAL);
		}
		//JrDesmond fast login on localhost for debug
		if (player.isOp()) {
			String ip = player.getAddress().getAddress().getHostAddress();
			if (player.getName().equals("JrDesmond") && ip.equalsIgnoreCase("127.0.0.1")) {
				new BukkitRunnable() {
					@Override
					public void run() {
						player.sendMessage("Próba zalogowania na lokalnym hoście...");
						player.performCommand("vconsole vsudo JrDesmond /login 123456");
					}
				}.runTaskLater(EternalAdventurePlugin.getInstance(), 20L);
			}
		}

		// Variables
		if (player.isOp() == true) {
			print.error("Gracz: " + player.getName() + " ma OP.");
		} else {
			print.debug("Gracz: " + player.getName() + " BRAK OP.");
		}
	}
}
