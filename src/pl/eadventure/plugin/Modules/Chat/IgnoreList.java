package pl.eadventure.plugin.Modules.Chat;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.PunishmentSystem;
import pl.eadventure.plugin.Utils.MagicGUI;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;
import pl.eadventure.plugin.gVar;

import java.util.*;

public class IgnoreList {
	public static MySQLStorage storage = EternalAdventurePlugin.getMySQL();
	List<Entry> list = new ArrayList<>();
	int dbid;

	public interface EntryType {
		int GLOBAL_CHAT = 1;
		int PRIVATE = 2;
		int ALL = 3;
	}

	public static class Entry {
		protected int dbid;
		protected String ignoredName;
		protected int type;

		public Entry(int dbid, String ignoredName, int type) {
			this.dbid = dbid;
			this.ignoredName = ignoredName;
			this.type = type;
		}

		public int getDbid() {
			return dbid;
		}

		public void setDbid(int dbid) {
			this.dbid = dbid;
		}

		public String getIgnoredName() {
			return ignoredName;
		}

		public void setIgnoredName(String ignoredName) {
			this.ignoredName = ignoredName;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public ArrayList<String> getDescriptionGui() {
			ArrayList<String> description = new ArrayList<>();
			String strFormat = String.format("&r&7Gracz: &3%s", this.ignoredName);
			description.add("");
			description.add(Utils.color(strFormat));
			description.add(Utils.color(String.format("&r%s&7 Czat globalny", (this.type == EntryType.GLOBAL_CHAT || this.type == EntryType.ALL) ? "&a☑" : "&c☒")));
			description.add(Utils.color(String.format("&r%s&7 Czat prywatny (/msg)", (this.type == EntryType.PRIVATE || this.type == EntryType.ALL) ? "&a☑" : "&c☒")));
			description.add("");
			description.add(Utils.color("&r&c&lSHIFT+PPM &7- aby usunąć."));
			description.add(Utils.color("&r&c&lPPM &7- aby zmienić typ ignorowania."));
			return description;
		}
	}

	public IgnoreList(int dbid) {
		this.dbid = dbid;
		loadDataFromDb();
	}

	public void loadDataFromDb() {
		//print.debug("IgnoreList.loadDataFromDb");
		list.clear();
		String sql = "SELECT " +
				"i.id," +
				"i.uid," +
				"p.nick AS target_name," +
				"i.type " +
				"FROM ignorelist i " +
				"JOIN players p ON i.target_uid = p.id " +
				"WHERE i.uid = ?; ";
		ArrayList<Object> params = new ArrayList<>();
		params.add(dbid);
		storage.querySafe(sql, params, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			if (numRows > 0) {
				int id;
				String ignoredName;
				int type;
				for (int i = 0; i < numRows; i++) {
					id = (int) rows.get(i).get("id");
					ignoredName = (String) rows.get(i).get("target_name");
					type = (int) rows.get(i).get("type");
					list.add(new Entry(id, ignoredName, type));
				}
			}
		});
	}

	public List<Entry> getList() {
		return list;
	}

	public int isIgnored(String nick) {
		for (Entry entry : list) {
			if (nick.contains(entry.getIgnoredName())) return entry.getType();
		}
		return 0;
	}

	public boolean add(String nick, int type) {
		if (isIgnored(nick) != 0) return false;
		if (!PunishmentSystem.getListPlayersAll().contains(nick)) return false;
		String sql = "INSERT INTO ignorelist (uid, target_uid, type) " +
				"VALUES (?, (SELECT id FROM players WHERE nick = ? LIMIT 1), ?);";
		ArrayList<Object> params = new ArrayList<>();
		params.add(dbid);
		params.add(nick);
		params.add(type);
		storage.executeSafe(sql, params);
		loadDataFromDb();//reload data
		return true;
	}

	public boolean remove(String nick) {
		if (!PunishmentSystem.getListPlayersAll().contains(nick)) return false;
		boolean found = false;
		for (Entry entry : list) {
			if (entry.ignoredName.contains(nick)) {
				found = true;
				String sql = "DELETE FROM ignorelist WHERE id=" + entry.dbid;
				storage.execute(sql);
				break;
			}
		}
		if (found) {
			loadDataFromDb();
		}
		return found;
	}

	public void switchType(int dbid) {
		for (Entry entry : list) {
			if (entry.dbid == dbid) {
				if (entry.getType() == EntryType.GLOBAL_CHAT) {
					entry.setType(EntryType.PRIVATE);
				} else if (entry.getType() == EntryType.PRIVATE) {
					entry.setType(EntryType.ALL);
				} else if (entry.getType() == EntryType.ALL) {
					entry.setType(EntryType.GLOBAL_CHAT);
				}
				String sql = "UPDATE ignorelist SET type=" + entry.type + " WHERE id=" + entry.dbid;
				storage.execute(sql);
				break;
			}
		}
	}

	//GUI
	public void showGui(Player p, int page) {
		int itemsPerPage = 45;
		int startIndex = (page - 1) * itemsPerPage;
		int endIndex = Math.min(startIndex + itemsPerPage, list.size());
		int totalPages = (int) Math.ceil((double) list.size() / itemsPerPage);
		String guiTitle = String.format(Utils.color("&4&lIgnorowani %d/%d"), page, Math.max(totalPages, 1));
		MagicGUI ignoreGui = MagicGUI.create(guiTitle, 54);
		ignoreGui.setAutoRemove(true);
		for (int i = startIndex; i < endIndex; i++) {
			Entry entry = list.get(i);
			ItemStack playerHead = Utils.getPlayerHead(entry.getIgnoredName(), Utils.color(String.format("&c&l%s", entry.getIgnoredName())), entry.getDescriptionGui());
			ignoreGui.addItem(playerHead, (player, gui, slot, type) -> {
				if (type == ClickType.SHIFT_RIGHT) {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					remove(entry.getIgnoredName());
					ignoreGui.setItem(slot, new ItemStack(Material.AIR));
				} else if (type == ClickType.RIGHT) {
					switchType(entry.dbid);
					showGui(player, page);
				}
			});
		}

		ignoreGui.open(p);
		// Generate navigation
		for (int x = 45; x < 54; x++) {
			if (x == 47) {
				if (page > 1) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowLeft"), Utils.color("&r&7&lPoprzednia strona"), null);
					ignoreGui.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int previousPage = page - 1;
						showGui(player, previousPage);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					ignoreGui.setItem(x, navButton);
				}
			} else if (x == 49) {
				ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hBlackX"), Utils.color("&r&7&lZamknij"), null);
				ignoreGui.setItem(x, navButton, (player, gui, slot, type) -> {
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
					ignoreGui.close(p);
				});
			} else if (x == 51) {
				if (endIndex < list.size()) {
					ItemStack navButton = Utils.itemWithDisplayName(gVar.customItems.get("hArrowRight"), Utils.color("&r&7&lNastępna strona"), null);
					ignoreGui.setItem(x, navButton, (player, gui, slot, type) -> {
						player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
						int nextPage = page + 1;
						showGui(player, nextPage);
					});
				} else {
					ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
					ignoreGui.setItem(x, navButton);
				}
			} else {
				ItemStack navButton = Utils.itemWithDisplayName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ", null);
				ignoreGui.setItem(x, navButton);
			}
		}
	}
}
