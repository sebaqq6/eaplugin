package pl.eadventure.plugin.API;

import me.NoChance.PvPManager.PvPManager;
import me.NoChance.PvPManager.PvPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.Utils.print;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PvpManagerAPI {
	public static PvpManagerAPI instance;
	Plugin plugin;
	Listener listener;
	PvPManager pvpManager;
	List<Player> temporaryTakenNewbie = new ArrayList<>();


	public PvpManagerAPI(Plugin plugin) {
		if (instance != null) return;
		if (Bukkit.getPluginManager().isPluginEnabled("PvPManager")) {
			pvpManager = (PvPManager) Bukkit.getPluginManager().getPlugin("PvPManager");
		}
		if (pvpManager == null)
			return;
		this.plugin = plugin;
		listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(listener, plugin);
		instance = this;
	}

	public static void takeNewbie(Player p) {
		if (instance.temporaryTakenNewbie.contains(p)) return;
		PvPlayer pvplayer = PvPlayer.get(p);
		if (!pvplayer.isNewbie()) return;
		try {
			Field field = pvplayer.getClass().getDeclaredField("c");
			field.setAccessible(true);
			field.set(pvplayer, false);
			instance.temporaryTakenNewbie.add(p);
			print.info("[PvpManagerAPI] Zabrano tymczasowo newbie: " + p.getName());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void restoreNewbie(Player p) {
		if (!instance.temporaryTakenNewbie.contains(p)) return;
		if (getTimeLeftNewbie(p) < 10000) return;
		PvPlayer pvplayer = PvPlayer.get(p);
		if (pvplayer.isNewbie()) return;
		try {
			Field field = pvplayer.getClass().getDeclaredField("c");
			field.setAccessible(true);
			field.set(pvplayer, true);
			instance.temporaryTakenNewbie.remove(p);
			print.info("[PvpManagerAPI] Przywrócono zabranego tymczasowo newbie: " + p.getName());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static long getTimeLeftNewbie(Player player) {
		PvPlayer pvplayer = PvPlayer.get(player);
		return pvplayer.getNewbieTimeLeft();
	}

	public static boolean isNewbie(Player player) {
		PvPlayer pvplayer = PvPlayer.get(player);
		return pvplayer.isNewbie();
	}

	static class Listeners implements Listener {
		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent e) {
			Player player = e.getPlayer();
			PvPlayer pvplayer = PvPlayer.get(player);
			if (!pvplayer.isNewbie() && instance.temporaryTakenNewbie.contains(player) && getTimeLeftNewbie(player) > 10000) {
				print.info("[PvpManagerAPI] Przywracam graczowi " + player.getName() + " status newbie...");
				restoreNewbie(player);
			}
		}
	}
}