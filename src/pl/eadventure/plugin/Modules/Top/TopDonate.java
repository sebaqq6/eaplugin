package pl.eadventure.plugin.Modules.Top;

import org.bukkit.Bukkit;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.print;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class TopDonate {
	MySQLStorage storage;
	int topCount = 0;
	private ArrayList<String> nickName = new ArrayList<>();
	private ArrayList<Integer> count = new ArrayList<>();

	public TopDonate(MySQLStorage storage, int topCount) {
		this.storage = storage;
		this.topCount = topCount;
		getDataFromMySQL();
		Bukkit.getScheduler().runTaskTimer(EternalAdventurePlugin.getInstance(), this::getDataFromMySQL, 120L, 4800L);//4 min
	}


	private void getDataFromMySQL() {
		if (storage.isConnect()) {
			String sql = "SELECT p.nick AS player_nick, SUM(d.count) AS total_donates FROM donate d JOIN players p ON d.playerid = p.id GROUP BY d.playerid ORDER BY total_donates DESC LIMIT " + topCount + ";";
			storage.query(sql, queryResult -> {
				if (queryResult != null) {
					int numRows = (int) queryResult.get("num_rows");
					@SuppressWarnings("unchecked")
					ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
					if (numRows >= 1) {
						nickName.clear();
						count.clear();
						for (int i = 0; i < numRows; i++) {
							nickName.add(i, (String) rows.get(i).get("player_nick"));
							BigDecimal bigDecimalValue = new BigDecimal(String.valueOf(rows.get(i).get("total_donates")));
							count.add(i, bigDecimalValue.intValue());
							//print.debug(String.format("i: %d, n: %s, %d", i, nickName.get(i), count.get(i)));
						}
					} else {
						nickName.clear();
						count.clear();
					}
				} else {
					print.error("TopDonate->getDataFromMySQL - błąd zapytania:");
					print.error(sql);
				}
			});
		} else {
			print.error("TopDonate->getDataFromMySQL - nie udało się ustanowić połączenia!");
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
