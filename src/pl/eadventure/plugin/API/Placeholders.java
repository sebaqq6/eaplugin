package pl.eadventure.plugin.API;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Commands.Command_redflag;
import pl.eadventure.plugin.Commands.Command_uczestnictwolive;
import pl.eadventure.plugin.EternalAdventurePlugin;
import pl.eadventure.plugin.Modules.Top.TopTimePlayerPlayed;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Locale;


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
		Player player = null;
		if (offlinePlayer != null) {
			player = offlinePlayer.getPlayer();
		}
		//print.debug("Placeholders->onRequest: "+params);
		//%eaplugin_test%
		if (params.equalsIgnoreCase("test")) {
			return "Placeholdery EternalAdventurePlugin działają poprawnie!";
		}
		//%eaplugin_blueflag_status%
		if (params.equalsIgnoreCase("blueflag_status")) {
			if (gVar.whiteList.isEmpty()) return "off";
			return "on";
		}
		//%eaplugin_redflag_status%
		if (params.equalsIgnoreCase("redflag_status")) {
			return Command_redflag.placeholderStatus;
		}
		//%topbb_x_name/count%
		if (params.startsWith("topbb_")) {
			String[] args = params.split("_");
			//print.debug(String.valueOf(args.length));
			if (args.length == 3) {
				//return name
				if (args[2].equalsIgnoreCase("name")) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) {
						return "errorminplacenumber:1";
					}
					return gVar.topBreakBlocks.getNickNameFromPlace(place);
				}
				//return count blocks
				if (args[2].equalsIgnoreCase("count")) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) {
						return "errorminplacenumber:1";
					}
					int count = gVar.topBreakBlocks.getCountFromPlace(place);
					if (count == -1) return "---";
					return String.valueOf(count);
				}
			}
		}
		//%topgs_x_name/count%
		if (params.startsWith("topgs_")) {
			String[] args = params.split("_");
			//print.debug(String.valueOf(args.length));
			if (args.length == 3) {
				//return name
				if (args[2].equalsIgnoreCase("name")) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) {
						return "errorminplacenumber:1";
					}
					return gVar.topGearScore.getNickNameFromPlace(place);
				}
				//return count
				if (args[2].equalsIgnoreCase("count")) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) {
						return "errorminplacenumber:1";
					}
					int count = gVar.topGearScore.getCountFromPlace(place);
					if (count == -1) return "---";
					return String.valueOf(count);
				}
			}
		}
		//%topdonate_x_name/count%
		if (params.startsWith("topdonate_")) {
			String[] args = params.split("_");
			//print.debug(String.valueOf(args.length));
			if (args.length == 3) {
				//return name
				if (args[2].equalsIgnoreCase("name")) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) {
						return "errorminplacenumber:1";
					}
					return gVar.topDonate.getNickNameFromPlace(place);
				}
				//return count
				if (args[2].equalsIgnoreCase("count")) {
					int place = 0;
					try {
						place = Integer.valueOf(args[1]);
					} catch (NumberFormatException e) {
						return "errorplacenumber";
					}
					if (place < 1) {
						return "errorminplacenumber:1";
					}
					int count = gVar.topDonate.getCountFromPlace(place);
					if (count == -1) return "---";
					return String.valueOf(count);
				}
			}
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
		//uczestnictwolive
		else if (params.equalsIgnoreCase("uczestnictwolive")) {
			if (player != null) {
				return Command_uczestnictwolive.placeholder(player);
			} else
				return "nie";
		}
		// money
		else if (params.equalsIgnoreCase("money")) {
			if (player != null) {
				double money = EternalAdventurePlugin.getEconomy().getBalance(player);
				NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
				currencyFormatter.setMaximumFractionDigits(0);
				return currencyFormatter.format(money);
			} else return "0";
		}
		// gs
		else if (params.equalsIgnoreCase("gs")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				return pd.gearScore;
			} else return "0";
		} else if (params.equalsIgnoreCase("gs_raw")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				return pd.gsRaw;
			} else return "0";
		}
		// breakblocks
		else if (params.equalsIgnoreCase("breakblocks")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				return String.valueOf(pd.breakBlocksCount);
			} else return "0";
		}
		//spec status
		else if (params.equalsIgnoreCase("spec")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				if (pd.lastSpec != null) {
					Timestamp now = Timestamp.from(Instant.now());
					if (now.getTime() < pd.lastSpec.getTime() + 3 * 1000) {//3 sec
						return "yes";
					}
				}
				return "no";
			} else return "no";
		}
		//live placeholder
		else if (params.equalsIgnoreCase("live")) {
			if (player != null) {
				PlayerData pd = PlayerData.get(player);
				if (pd.onLiveStream) {
					return gVar.liveStream.getLivePlaceholder();
				} else {
					return "";
				}
			} else return null;
		}
		//glow color placeholder
		else if (params.equalsIgnoreCase("glowcolor")) {
			if (player != null) {
				return GlowAPI.getGlowColor(player);
			} else return null;
		}
		return null; //
	}
}