package pl.eadventure.plugin.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ChatInputCapture {
	Listener listeners;
	Plugin plugin;

	public ChatInputCapture(Plugin plugin) {
		this.plugin = plugin;
		listeners = new ChatInputCaptureListeners();
		Bukkit.getPluginManager().registerEvents(listeners, plugin);
	}


	public interface ReceiveCallback {
		void onReceiveMessage(Player player, String message);
	}

	public void receiveInput(Player player, List<String> infoMessages, ReceiveCallback callback) {

	}

	public static class ChatInputCaptureListeners implements Listener {
		@EventHandler
		public void onPlayerChat(AsyncPlayerChatEvent e) {
		}
	}
}
