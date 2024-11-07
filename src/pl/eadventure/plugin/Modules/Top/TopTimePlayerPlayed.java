package pl.eadventure.plugin.Modules.Top;

import org.bukkit.Bukkit;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Utils.MySQLStorage;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.HashMap;

public class TopTimePlayerPlayed {
	private static boolean loaded = false;
	private static MySQLStorage storage;
	private static int topCount = 0;
	private static ArrayList<String> nickName = new ArrayList<>();
	private static ArrayList<Integer> timeHours = new ArrayList<>();
	private static ArrayList<Integer> timeMinutes = new ArrayList<>();
	private static ArrayList<Integer> timeSeconds = new ArrayList<>();

	static public void load(MySQLStorage lStorage, int lTopCount) {
		if (loaded == true) return;
		storage = lStorage;
		topCount = lTopCount;
		getDataFromMySQL();

		Bukkit.getScheduler().runTaskTimer(EternalAdventurePlugin.getInstance(), () -> {
			getDataFromMySQL();
		}, 120L, 4800L);//4 min
		loaded = true;
	}

	private static void getDataFromMySQL() {
		if (storage.isConnect()) {
			String sql = "SELECT *, onlineHours * 3600 + onlineMinutes * 60 + onlineSeconds AS totalSeconds FROM players ORDER BY totalSeconds DESC LIMIT " + topCount + ";";

			storage.query(sql, queryResult -> {
				if (queryResult != null) {
					int numRows = (int) queryResult.get("num_rows");
					@SuppressWarnings("unchecked")
					ArrayList<HashMap<?, ?>> rows = (ArrayList<HashMap<?, ?>>) queryResult.get("rows");
					if (numRows >= 1) {
						nickName.clear();
						timeHours.clear();
						timeMinutes.clear();
						timeSeconds.clear();
						for (int i = 0; i < numRows; i++) {
							nickName.add(i, (String) rows.get(i).get("nick"));
							timeHours.add(i, (int) rows.get(i).get("onlineHours"));
							timeMinutes.add(i, (int) rows.get(i).get("onlineMinutes"));
							timeSeconds.add(i, (int) rows.get(i).get("onlineSeconds"));
							//print.debug(String.format("i: %d, n: %s, %d:%d:%d", i, nickName.get(i), timeHours.get(i), timeMinutes.get(i), timeSeconds.get(i)));
						}
					} else {
						nickName.clear();
						timeHours.clear();
						timeMinutes.clear();
						timeSeconds.clear();
					}
				} else {
					print.error("TopTimePlayerPlayed->getDataFromMySQL - błąd zapytania:");
					print.error(sql);
				}
			});
		} else {
			print.error("TopTimePlayerPlayed->getDataFromMySQL - nie udało się ustanowić połączenia!");
		}
	}

	public static String getNickNameFromPlace(int place) {
		if (place >= 1 && place <= nickName.size()) {
			return nickName.get(place - 1);
		}
		return "---";
	}

	public static int getHoursFromPlace(int place) {
		if (place >= 1 && place <= timeHours.size()) {
			return timeHours.get(place - 1);
		}
		return -1;
	}

	public static int getMinutesFromPlace(int place) {
		if (place >= 1 && place <= timeMinutes.size()) {
			return timeMinutes.get(place - 1);
		}
		return -1;
	}

	public static int getSecondsFromPlace(int place) {
		if (place >= 1 && place <= timeSeconds.size()) {
			return timeSeconds.get(place - 1);
		}
		return -1;
	}
}
