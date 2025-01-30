package pl.eadventure.plugin.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.logging.Level;

public class print {
	private static boolean debugEnabled = false;

	public static void info(String text) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "[EAP] " + text);
	}

	public static void okRed(String text) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[EAP] " + text);
	}

	public static void ok(String text) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[EAP] " + text);
	}

	public static void error(String text) {
		Bukkit.getLogger().log(Level.SEVERE, "[EAP] " + text);
	}

	public static void raw(String text) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.RED + text);
	}

	public static void debug(String text) {
		if (debugEnabled) Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[DEBUG][EAP] " + text);
	}

	public static void setDebug(Boolean enabled) {
		debugEnabled = enabled;
	}

	public static boolean getDebug() {
		return debugEnabled;
	}
}
