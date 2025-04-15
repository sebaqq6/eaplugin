package pl.eadventure.plugin.API;

import fr.skytasul.glowingentities.GlowingBlocks;
import fr.skytasul.glowingentities.GlowingEntities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.print;

import java.util.HashMap;
import java.util.Map;

public class GlowAPI {
	private static GlowAPI instance = null;
	private Plugin plugin;
	private GlowingEntities glowingEntities;
	private GlowingBlocks glowingBlocks;
	private boolean loaded = false;
	private final HashMap<Player, ChatColor> glowColor = new HashMap<>();
	private Listener listener;
	private static final Map<ChatColor, String> colorHexMap = new HashMap<>();

	static {
		colorHexMap.put(ChatColor.BLACK, "#000000");
		colorHexMap.put(ChatColor.DARK_BLUE, "#0000AA");
		colorHexMap.put(ChatColor.DARK_GREEN, "#00AA00");
		colorHexMap.put(ChatColor.DARK_AQUA, "#00AAAA");
		colorHexMap.put(ChatColor.DARK_RED, "#AA0000");
		colorHexMap.put(ChatColor.DARK_PURPLE, "#AA00AA");
		colorHexMap.put(ChatColor.GOLD, "#FFAA00");
		colorHexMap.put(ChatColor.GRAY, "#AAAAAA");
		colorHexMap.put(ChatColor.DARK_GRAY, "#555555");
		colorHexMap.put(ChatColor.BLUE, "#5555FF");
		colorHexMap.put(ChatColor.GREEN, "#55FF55");
		colorHexMap.put(ChatColor.AQUA, "#55FFFF");
		colorHexMap.put(ChatColor.RED, "#FF5555");
		colorHexMap.put(ChatColor.LIGHT_PURPLE, "#FF55FF");
		colorHexMap.put(ChatColor.YELLOW, "#FFFF55");
		colorHexMap.put(ChatColor.WHITE, "#FFFFFF");
	}

	public GlowAPI(Plugin plugin) {
		if (instance != null) {
			print.error("Tylko jedna instancja GlowAPI może zostać stworzona.");
			return;
		}
		print.info("Ładowanie GlowAPI...");
		this.plugin = plugin;
		this.glowingEntities = new GlowingEntities(plugin);
		this.glowingBlocks = new GlowingBlocks(plugin);
		this.loaded = true;
		this.listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(listener, plugin);
		instance = this;
		print.ok("GlowAPI - załadowane!");
	}

	public void unload() {
		if (loaded) {
			glowingEntities.disable();
			glowingBlocks.disable();
			loaded = false;
			instance = null;
		}
	}

	public static GlowingBlocks getBlocksAPI() {
		return instance.glowingBlocks;
	}

	public static GlowingEntities getEntitiesAPI() {
		return instance.glowingEntities;
	}

	public static GlowAPI getInstance() {
		return instance;
	}

	public static void glowBlock(Block block, Player receiver, ChatColor color, long timeTick) {
		GlowingBlocks gb = getBlocksAPI();
		try {
			gb.setGlowing(block, receiver, color);
			if (timeTick > 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						unGlowBlock(block, receiver);
					}
				}.runTaskLater(GlowAPI.getInstance().plugin, timeTick);
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void unGlowBlock(Block block, Player receiver) {
		GlowingBlocks gb = getBlocksAPI();
		try {
			gb.unsetGlowing(block, receiver);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void glowPlayer(Player player, Player receiver, ChatColor color, long timeTick) {
		GlowingEntities ge = getEntitiesAPI();
		try {
			if (player.equals(receiver)) {
				return;
			}
			ge.unsetGlowing(player, receiver);//fix bug with?
			ge.setGlowing(player, receiver, color);
			GlowAPI.getInstance().glowColor.put(player, color);
			if (timeTick > 0) {
				new BukkitRunnable() {
					@Override
					public void run() {
						unGlowPlayer(player, receiver);
					}
				}.runTaskLater(GlowAPI.getInstance().plugin, timeTick);
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void unGlowPlayer(Player player, Player receiver) {
		GlowingEntities ge = GlowAPI.getEntitiesAPI();
		try {
			if (player.equals(receiver)) {
				return;
			}
			ge.unsetGlowing(player, receiver);
			GlowAPI.getInstance().glowColor.remove(player);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getGlowColor(Player player) {
		if (GlowAPI.getInstance().glowColor.containsKey(player)) {
			ChatColor color = GlowAPI.getInstance().glowColor.get(player);
			return colorHexMap.getOrDefault(color, "#DDDDDD");
		}
		return "#DDDDDD";
	}

	public class Listeners implements Listener {
		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent e) {
			glowColor.remove(e.getPlayer());
		}
	}
}
