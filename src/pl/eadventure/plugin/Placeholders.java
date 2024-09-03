package pl.eadventure.plugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Utils.Utils;


public class Placeholders extends PlaceholderExpansion {

	private final EternalAdventurePlugin plugin; //

	public Placeholders(EternalAdventurePlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getAuthor() {
		return String.join(", ", plugin.getDescription().getAuthors()); //
	}

	@Override
	public String getIdentifier() {
		return "eaplugin";
	}

	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion(); //
	}

	@Override
	public boolean persist() {
		return true; //
	}

	@Override
	public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
		Player player = offlinePlayer.getPlayer();
		//print.debug("Placeholders->onRequest: "+params);
		//%eaplugin_test%
		if (params.equalsIgnoreCase("test")) {
			return "Placeholdery EternalAdventurePlugin działają poprawnie!";
		}
		//%toponline_x_name/(time_h/m/s)
		if (params.startsWith("toponline_")) {
			String[] args = params.split("_");
			if (args.length >= 3 && args.length <= 4) {
				if (args[2].equalsIgnoreCase("name") && args.length == 3) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) return "errorminplacenumber:1";
					return TopTimePlayerPlayed.getNickNameFromPlace(place);
				} else if (args[2].equalsIgnoreCase("time")) {//time
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1)
						return "errorminplacenumber:1";
					if (args.length < 4)
						if (TopTimePlayerPlayed.getHoursFromPlace(place) != -1)
							return String.format("%d:%02d:%02d", TopTimePlayerPlayed.getHoursFromPlace(place), TopTimePlayerPlayed.getMinutesFromPlace(place), TopTimePlayerPlayed.getSecondsFromPlace(place));
						else
							return "--:--:--";
					else {
						if (args.length == 4) {
							if (args[3].equalsIgnoreCase("h")) {
								if (TopTimePlayerPlayed.getHoursFromPlace(place) != -1)
									return String.format("%d", TopTimePlayerPlayed.getHoursFromPlace(place));
								else
									return "---";
							} else if (args[3].equalsIgnoreCase("m")) {
								if (TopTimePlayerPlayed.getMinutesFromPlace(place) != -1)
									return String.format("%d", TopTimePlayerPlayed.getMinutesFromPlace(place));
								else
									return "---";
							} else if (args[3].equalsIgnoreCase("s")) {
								if (TopTimePlayerPlayed.getSecondsFromPlace(place) != -1)
									return String.format("%d", TopTimePlayerPlayed.getSecondsFromPlace(place));
								else
									return "---";
							}
						}
					}
				}
			}
			return null;
		}
		//totalonline_h
		if (params.equalsIgnoreCase("totalonline_h")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				return String.format("%02d", pd.onlineHours);
			} else
				return "00";
		}
		//totalonline_m
		else if (params.equalsIgnoreCase("totalonline_m")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				return String.format("%02d", pd.onlineMinutes);
			} else
				return "00";
		}
		//totalonline_s
		else if (params.equalsIgnoreCase("totalonline_s")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				return String.format("%02d", pd.onlineSeconds);
			} else
				return "00";
		}
		//totalonline_formated
		else if (params.equalsIgnoreCase("totalonline_formated")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				if (pd.onlineHours > 0) return String.format("%dh %dm", pd.onlineHours, pd.onlineMinutes);
				else return String.format("%dm", pd.onlineMinutes);
			} else
				return "0m";
			//sessiontime_formated
		} else if (params.equalsIgnoreCase("sessiontime_formated")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				int[] time = Utils.convertSecondsToTime(pd.sessionOnlineSeconds);
				if (time[0] > 0) return String.format("%dh %dm", time[0], time[1]);
				else return String.format("%dm", time[1]);
			} else return "0m";
		}
		// maxsessiontime_formated
		else if (params.equalsIgnoreCase("maxsessiontime_formated")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				int[] time = Utils.convertSecondsToTime(pd.maxSessionOnlineSeconds);
				if (time[0] > 0) return String.format("%dh %dm", time[0], time[1]);
				else return String.format("%dm", time[1]);
			} else return "0m";
		}
		// maxsessiontime_formated
		else if (params.equalsIgnoreCase("gs")) {
			if (player != null) {
				return "99999";
			} else return "0";
		}
		return null; //
	}
}