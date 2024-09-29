package pl.eadventure.plugin.Modules;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.Utils.MagicGUI;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnnounceManager {
	Plugin plugin;
	MySQLStorage storage;
	List<Announce> announceList = new ArrayList<>();
	ItemStack hGreenPlus;
	ItemStack hBlackX;

	//table: announcements
	record Announce(int id, String authorName, Timestamp created, Timestamp expire, String text) {
	}

	//constructor
	public AnnounceManager(Plugin plugin, MySQLStorage storage) {
		this.plugin = plugin;
		this.storage = storage;
		hGreenPlus = gVar.customItems.get("hGreenPlus");
		hBlackX = gVar.customItems.get("hBlackX");
		load();
	}

	//load data from database
	public void load() {
		String sql = "SELECT " +
				"announcements.id, nick AS author, created, expire, text " +
				"FROM " +
				"announcements " +
				"LEFT JOIN players ON announcements.author=players.id " +
				"ORDER BY expire DESC;";
		storage.query(sql, queryResult -> {
			int numRows = (int) queryResult.get("num_rows");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
			if (numRows > 0) {
				announceList.clear();
				int id;
				String author;
				Timestamp created;
				Timestamp expire;
				String text;
				for (int i = 0; i < numRows; i++) {
					id = (int) rows.get(i).get("id");
					author = (String) rows.get(i).get("author");
					created = (Timestamp) rows.get(i).get("created");
					expire = (Timestamp) rows.get(i).get("expire");
					text = (String) rows.get(i).get("text");
					announceList.add(new Announce(id, author, created, expire, text));
				}
			}
		});
	}

	//show main menu
	public void showMainMenuGUI(Player p) {
		boolean canManage = p.hasPermission("eadventureplugin.annuance.manage");
		MagicGUI mainGui = MagicGUI.create(Utils.mm("<bold><gradient:#BF00FF:#7d00a7>Ogłoszenia</bold>"), 54);
		mainGui.setAutoRemove(true);
		ItemStack close = Utils.itemWithDisplayName(hBlackX, Utils.mm("<!i><bold><#999999>Zamknij</#999999></bold>"), null);
		mainGui.setItem(49, hBlackX, ((player, gui, slot, type) -> {
			mainGui.close(player);
		}));
		mainGui.open(p);
	}
}
