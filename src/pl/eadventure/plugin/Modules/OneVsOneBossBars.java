package pl.eadventure.plugin.Modules;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.API.EiAPI;
import pl.eadventure.plugin.Utils.wgAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// new Location(world_utility, 90.29, 107.00, -349.45, -4.65F, 29.55F);
public class OneVsOneBossBars {
	public static OneVsOneBossBars instance;
	private Plugin plugin;
	private Player playerRed;
	private Player playerPurple;
	private ItemStack redItem;
	private ItemStack purpleItem;
	private BossBar bossBarRed;
	private BossBar bossBarPurple;
	private String spectatorArea = "_igrzyska_widownia_safe_";
	private String spectatorAreaLobby = "_igrzyska_eternal_lobby_";
	private String fightArea = "_igrzyska_eternal_";
	private Set<Player> spectatorsList = new HashSet<>();
	private Set<Player> warriorList = new HashSet<>();
	private Listener listener;

	public OneVsOneBossBars(Plugin plugin) {
		instance = this;
		this.plugin = plugin;
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::scheduler, 20L, 20L);
		this.bossBarRed = Bukkit.createBossBar("---", BarColor.RED, BarStyle.SOLID);
		this.bossBarPurple = Bukkit.createBossBar("---", BarColor.PURPLE, BarStyle.SOLID);
		this.bossBarRed.setVisible(true);
		this.bossBarPurple.setVisible(true);
		this.listener = new Listeners();
		Bukkit.getPluginManager().registerEvents(listener, plugin);
	}

	//one second scheduler async
	private void scheduler() {
		detectPlayers();
		updateBossBars();
		showHideBossBars();
	}

	private void detectPlayers() {
		spectatorsList.clear();
		warriorList.clear();
		playerRed = null;
		playerPurple = null;
		redItem = null;
		purpleItem = null;
		for (Player p : Bukkit.getOnlinePlayers()) {
			//detect spectators
			if (wgAPI.isOnRegion(p, spectatorArea) || wgAPI.isOnRegion(p, spectatorAreaLobby)/* || (AutoSpectator.isLiveOperator(p) && wgAPI.isOnRegion(p, fightArea))*/) {
				spectatorsList.add(p);
			} else if (wgAPI.isOnRegion(p, fightArea)) {//detect warriors
				warriorList.add(p);
				ItemStack[] inventory = p.getInventory().getContents();
				for (ItemStack item : inventory) {
					if (item == null) continue;
					if (item.getType().equals(Material.AIR)) continue;
					ItemMeta itemMeta = item.getItemMeta();
					if (itemMeta != null) {
						PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
						NamespacedKey eiKey = NamespacedKey.fromString("executableitems:ei-id");
						if (pdc != null && eiKey != null) {
							String eiItemID = pdc.get(eiKey, PersistentDataType.STRING);
							if (eiItemID != null) {
								if (eiItemID.equalsIgnoreCase("czerwony_znacznik_igrzyska")) {
									playerRed = p;
									redItem = item;
								} else if (eiItemID.equalsIgnoreCase("fioletowy_znacznik_igrzyska")) {
									playerPurple = p;
									purpleItem = item;
								}
							}
						}
					}
				}
			}
		}
	}

	private void updateBossBars() {
		for (Player p : warriorList) {
			int frags = 0;
			double maxHealth = p.getMaxHealth();
			double health = p.getHealth() / maxHealth;
			String itemName = PlaceholderAPI.setPlaceholders(p, "%player_item_in_hand_name%");
			//get held item power
			int itemPower = 0;
			ItemStack itemInHand = p.getItemInHand();
			if (itemInHand != null) {
				int itemUsage = EiAPI.getItemUsage(itemInHand);
				if (itemUsage > 0) {
					itemPower = itemUsage - 1;
				}
			}
			//red
			if (p.equals(playerRed)) {
				//frags
				if (redItem != null) {
					frags = EiAPI.getItemUsage(redItem);
					frags--;
				}
				String itemInfo = String.format("&c- %s &b&o⚡&8&l(&b%d&8&l)", itemName, itemPower);
				if (itemName.isEmpty() || ((itemInHand.equals(redItem) || itemInHand.equals(purpleItem)) && redItem != null && purpleItem != null)) {
					itemInfo = " ";
				}
				String title = String.format("&4&lᴄᴢᴇʀᴡᴏɴʏ &8&l" +
						"(&e&l%d&f&l/&7&l5 &f\uD83C\uDFC1&8&l) " +
						"&c- &c%s &c- &4&lHP &c&l%.1f&f&l/&7&l%.1f &c❤ " +
						"%s", frags, p.getName(), p.getHealth(), maxHealth, itemInfo);
				bossBarRed.setTitle(ChatColor.translateAlternateColorCodes('&', title));
				bossBarRed.setProgress(health);
			}
			//purple
			else if (p.equals(playerPurple)) {
				//frags
				if (purpleItem != null) {
					frags = EiAPI.getItemUsage(purpleItem);
					frags--;
				}
				String itemInfo = String.format("&d- %s &b&o⚡&8&l(&b%d&8&l)", itemName, itemPower);
				if (itemName.isEmpty() || ((itemInHand.equals(redItem) || itemInHand.equals(purpleItem)) && redItem != null && purpleItem != null)) {
					itemInfo = " ";
				}
				String title = String.format("&d&lꜰɪᴏʟᴇᴛᴏᴡʏ &8&l" +
						"(&e&l%d&f&l/&7&l5 &f\uD83C\uDFC1&8&l) " +
						"&d- &5%s &d- &d&lHP &c&l%.1f&f&l/&7&l%.1f &5❤ " +
						"%s", frags, p.getName(), p.getHealth(), maxHealth, itemInfo);
				bossBarPurple.setTitle(ChatColor.translateAlternateColorCodes('&', title));
				bossBarPurple.setProgress(health);
			}
		}
	}

	private void showHideBossBars() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (spectatorsList.contains(p)) {
				//red
				if (playerRed != null) {
					bossBarRed.addPlayer(p);
				} else {
					bossBarRed.removePlayer(p);
				}
				//purple
				if (playerPurple != null) {
					bossBarPurple.addPlayer(p);
				} else {
					bossBarPurple.removePlayer(p);
				}
			} else {
				bossBarRed.removePlayer(p);
				bossBarPurple.removePlayer(p);
			}
		}
	}

	public static List<Player> getActiveWarriors() {
		List<Player> warriors = new ArrayList<>();
		if (instance.playerRed != null && instance.playerPurple != null) {
			warriors.add(instance.playerRed);
			warriors.add(instance.playerPurple);
		}
		return warriors;
	}

	public static class Listeners implements Listener {
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
			if (event.getEntity() instanceof Player victim) {
				if (victim.equals(instance.playerRed) || victim.equals(instance.playerPurple)) {
					instance.updateBossBars();
				}
			}
		}
	}
}
