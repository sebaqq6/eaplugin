package pl.eadventure.plugin.Commands;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MagicGUI;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

record ChunkInfo(Chunk chunk, int chunkEntities) { }

public class Command_chunkhunt implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			new BukkitRunnable() {
				@Override
				public void run() {// ASYNC THREAD
					//get player world
					World world = player.getWorld();
					//load all chunks
					List<ChunkInfo> chunkInfos = new ArrayList<>();
					for (Chunk chunk : world.getLoadedChunks()) {
						int countEntities = chunk.getEntities().length;
						chunkInfos.add(new ChunkInfo(chunk, countEntities));
					}
					chunkInfos.sort(Comparator.comparingInt(ChunkInfo::chunkEntities).reversed());
					new BukkitRunnable() {// MAIN THREAD
						@Override
						public void run() {
							MagicGUI enitityHuntGUI = MagicGUI.create(Utils.color("&4&lPolowanie na chunka"), 54);
							enitityHuntGUI.setAutoRemove(true);
							int guiItemsCount = 0;
							for (ChunkInfo chunkInfo : chunkInfos) {
								guiItemsCount++;
								Chunk chunk = chunkInfo.chunk();
								int chunkEntities = chunkInfo.chunkEntities();
								ArrayList<String> description = new ArrayList<>();
								description.add(Utils.color(String.format("&r&7Ilość Entities: &3%d", chunkEntities)));
								description.add(" ");
								description.add(Utils.color("&r&c&lSHIFT+PPM &7- aby się teleportować"));
								description.add(Utils.color("&7na środek chunka."));
								String itemName = String.format("&r&4&l[Chunk: %d %d]", chunk.getX(), chunk.getZ());
								ItemStack chunkItem = Utils.itemWithDisplayName(new ItemStack(Material.GRASS_BLOCK), Utils.color(itemName), description);
								enitityHuntGUI.addItem(chunkItem, (p, gui, slot, type) -> {
									if (type == ClickType.SHIFT_RIGHT) {
										p.sendMessage("Teleportowałeś się na środek: " + Utils.color(itemName));
										// Get the highest block within the chunk
										int highestY = chunk.getWorld().getHighestBlockYAt(chunk.getBlock(0, 0, 0).getLocation());

										// Calculate the center coordinates of the chunk
										int centerX = chunk.getBlock(8, highestY, 0).getX();
										int centerZ = chunk.getBlock(0, highestY, 8).getZ();

										// Teleport the player to the center of the chunk, avoiding being underground
										player.teleport(chunk.getWorld().getHighestBlockAt(centerX, centerZ).getLocation().add(0.5, 1, 0.5));
									}
								});
								if (guiItemsCount > 53) break;
							}
							enitityHuntGUI.open(player);
						}
					}.runTask(EternalAdventurePlugin.getInstance());
				}
			}.runTaskAsynchronously(EternalAdventurePlugin.getInstance());
		}
		return true;
	}
}
