package pl.eadventure.plugin.Modules.Chat;

import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.print;

import java.util.ArrayList;
import java.util.List;

public class Channel {
	public static List<Channel> channelList = new ArrayList<>();
	String channelName;
	String channelPrefix;
	String alias;
	String format;
	String channelPermission;
	int distance;
	boolean filter;
	boolean autoJoin;

	public Channel(String channelName, String alias, String channelPermission, boolean autoJoin) {
		this.channelPrefix = "[" + channelName + "]";
		this.channelName = channelName;
		this.alias = alias;
		this.channelPermission = channelPermission;
		this.autoJoin = autoJoin;
		this.distance = 0;
		channelList.add(this);
	}

	public static Channel getChannelByName(String channelName) {
		for (Channel channel : channelList) {
			if (channel.getChannelName().equalsIgnoreCase(channelName)) {
				return channel;
			}
		}
		return null;
	}

	public List<Player> getViewers(Player sourcePlayer) {
		List<Player> listViewers = new ArrayList<>();
		PlayerData sourcePlayerData = PlayerData.get(sourcePlayer);
		for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
			PlayerData targetPlayerData = PlayerData.get(targetPlayer);
			if (targetPlayerData.joinedChatChannels.contains(sourcePlayerData.chatChannel)) {
				if (distance == 0) {
					listViewers.add(targetPlayer);
				} else if (sourcePlayer.getLocation().distance(targetPlayer.getLocation()) < distance) {
					listViewers.add(targetPlayer);
				}
			}
		}
		return listViewers;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}


	public String getChannelPrefix() {
		return channelPrefix;
	}

	public void setChannelPrefix(String channelPrefix) {
		this.channelPrefix = channelPrefix;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public boolean isFilter() {
		return filter;
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	public String getChannelPermission() {
		return channelPermission;
	}
}
