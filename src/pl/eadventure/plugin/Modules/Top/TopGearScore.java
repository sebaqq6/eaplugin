package pl.eadventure.plugin.Modules.Top;

import org.bukkit.Bukkit;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.HashMap;

public class TopGearScore {
	MySQLStorage storage;
	int topCount = 0;
	private ArrayList<String> nickName = new ArrayList<>();
	private ArrayList<Integer> count = new ArrayList<>();

	public TopGearScore(MySQLStorage storage, int topCount) {
		this.storage = storage;
		this.topCount = topCount;
		getDataFromMySQL();
		Bukkit.getScheduler().runTaskTimer(EternalAdventurePlugin.getInstance(), this::getDataFromMySQL, 120L, 4800L);//4 min
	}


	private void getDataFromMySQL() {
		if (storage.isConnect()) {
			String sql = "SELECT nick, lastgs FROM players WHERE lastgs > 0 ORDER BY lastgs DESC LIMIT " + topCount + ";";
			storage.query(sql, queryResult -> {
				if (queryResult != null) {
					int numRows = (int) queryResult.get("num_rows");
					@SuppressWarnings("unchecked")
					ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
					if (numRows >= 1) {
						nickName.clear();
						count.clear();
						for (int i = 0; i < numRows; i++) {
							nickName.add(i, (String) rows.get(i).get("nick"));
							count.add(i, (int) rows.get(i).get("lastgs"));
							//print.debug(String.format("i: %d, n: %s, %d", i, nickName.get(i), count.get(i)));
						}
					} else {
						nickName.clear();
						count.clear();
					}
				} else {
					print.error("TopGearScore->getDataFromMySQL - błąd zapytania:");
					print.error(sql);
				}
			});
		} else {
			print.error("TopGearScore->getDataFromMySQL - nie udało się ustanowić połączenia!");
		}
	}

	//get data
	public String getNickNameFromPlace(int place) {
		if (place >= 1 && place <= nickName.size()) {
			return nickName.get(place - 1);
		}
		return "---";
	}

	public int getCountFromPlace(int place) {
		if (place >= 1 && place <= count.size()) {
			return count.get(place - 1);
		}
		return -1;
	}
}
