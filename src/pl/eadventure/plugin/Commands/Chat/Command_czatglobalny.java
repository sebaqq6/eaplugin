package pl.eadventure.plugin.Commands.Chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Modules.Chat.Channel;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;

public class Command_czatglobalny implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			Channel channel = Channel.getChannelByName("Globalny");
			PlayerData pd = PlayerData.get(player);
			if (args.length == 0) {//get target nick args[0]
				if (pd.chatChannel != channel) {
					pd.chatChannel = channel;
					sender.sendMessage(Utils.mm("<green>Pomyślnie przełączono czat na <dark_green>" + channel.getChannelName() + "<green>."));
				} else {
					sender.sendMessage(Utils.mm("<green>Twój czat jest już ustawiony na <dark_green>" + channel.getChannelName() + "<green>."));
				}
				return true;
			}
			StringBuilder messageBuilder = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				messageBuilder.append(args[i]).append(" ");
			}
			String message = messageBuilder.toString().trim();
			pd.chatChannel = channel;
			player.chat(message);
			pd.chatChannel = null;
		} else {
			sender.sendMessage("Komenda dostępna tylko z poziomu gry.");
		}
		return true;
	}
}
