package pl.eadventure.plugin.API;

import fr.skytasul.glowingentities.GlowingBlocks;
import fr.skytasul.glowingentities.GlowingEntities;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.print;

public class GlowAPI {
	private static GlowAPI instance = null;
	private Plugin plugin;
	private GlowingEntities glowingEntities;
	private GlowingBlocks glowingBlocks;
	private boolean loaded = false;

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
			ge.setGlowing(player, receiver, color);
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
			ge.unsetGlowing(player, receiver);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
