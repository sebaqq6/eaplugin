package pl.eadventure.plugin.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.Modules.HomesInterface;
import pl.eadventure.plugin.PlayerData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatInputCapture {
	private final Map<Player, ReceiveCallback> activeCallbacks = new HashMap<>();
	private final Map<Player, List<Component>> infoMessages = new HashMap<>();
	private final Map<Player, Integer> titleQueue = new HashMap<>();
	Listener listeners;
	Plugin plugin;

	public ChatInputCapture(Plugin plugin) {
		this.plugin = plugin;
		this.listeners = new ChatInputCaptureListeners();
		Bukkit.getPluginManager().registerEvents(listeners, plugin);
		new BukkitRunnable() {
			@Override
			public void run() {
				fiveSecondTimer();
			}
		}.runTaskTimerAsynchronously(plugin, 20L, 100L);
	}

	public interface ReceiveCallback {
		int onReceiveMessage(Player player, String message);
	}

	public void receiveInput(Player player, List<Component> infoMessages, ReceiveCallback callback) {
		this.titleQueue.put(player, 0);
		this.infoMessages.put(player, infoMessages);
		sendInfoMessages(player);
		activeCallbacks.put(player, callback);
	}

	private void removePlayer(Player player) {
		this.activeCallbacks.remove(player);
		this.titleQueue.remove(player);
		this.infoMessages.remove(player);
		print.debug("RemovePlayer");
	}

	public void sendInfoMessages(Player player) {
		player.sendMessage(Utils.mm("<gradient:#a500d3:#440057><strikethrough>-------------------------------------------------</gradient>"));
		for (Component infoMessage : infoMessages.get(player)) {
			player.sendMessage(infoMessage);
		}
		player.sendMessage(Utils.mm("<gradient:#a500d3:#440057><strikethrough>-------------------------------------------------</gradient>"));
	}

	public class ChatInputCaptureListeners implements Listener {
		@EventHandler
		public void onPlayerChat(AsyncPlayerChatEvent e) {
			Player player = e.getPlayer();
			if (activeCallbacks.containsKey(player)) {
				String message = e.getMessage();
				new BukkitRunnable() {
					@Override
					public void run() {
						ReceiveCallback callback = activeCallbacks.get(player);
						int callBackReturn = callback.onReceiveMessage(player, message);
						if (callBackReturn == 1) {
							removePlayer(player);
						}
					}
				}.runTask(plugin);
				e.setCancelled(true);
			}
		}

		//---------------------------------------block commands while player typing value
		@EventHandler
		public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
			Player player = e.getPlayer();
			if (activeCallbacks.containsKey(player) && infoMessages.containsKey(player)) {
				sendInfoMessages(player);
				e.setCancelled(true);
			}
		}
	}

	//Timers
	private void fiveSecondTimer() {
		removeOfflinePlayers();
		refreshTitle();
	}

	// Remove offline players
	private void removeOfflinePlayers() {
		for (Player player : activeCallbacks.keySet()) {
			if (!player.isOnline()) {
				removePlayer(player);
			}
		}
	}

	// Refresh middle text on the screen
	private void refreshTitle() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!activeCallbacks.containsKey(player)) continue;
			int queue = titleQueue.get(player);
			List<Component> messages = infoMessages.get(player);
			Component title = messages.get(queue);
			player.showTitle(Title.title(title, Component.text("")));
			queue++;
			if (queue >= messages.size()) {
				queue = 0;
			}
			titleQueue.put(player, queue);
		}
	}
}
