package pl.eadventure.plugin.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.eadventure.plugin.Utils.print;

public class Command_a implements CommandExecutor {
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (sender instanceof Player) {
			String target = "";
			if (args.length > 0) target = args[0];
			Player player = (Player) sender;
			print.debug("Komenda /aa wpisana przez "+player.getName());
			if(player.hasPermission("eadventureplugin.apanel")) {
				print.debug("Komenda /aa wpisana przez "+player.getName()+" przekierowano na /"+"apanel "+target);
				//player.performCommand("apanel "+target);//NIE DZIAŁA
				player.chat("/apanel "+target);
			}
			else if(player.hasPermission("eadventureplugin.mpanel")) {
				print.debug("Komenda /aa wpisana przez "+player.getName()+" przekierowano na /"+"mpanel "+target);
				player.chat("/mpanel "+target);
			}
			else if(player.hasPermission("eadventureplugin.gpanel")) {
				print.debug("Komenda /aa wpisana przez "+player.getName()+" przekierowano na /"+"gpanel "+target);
				player.chat("/gpanel "+target);
			}
			else if(player.hasPermission("eadventureplugin.spanel")) {
				print.debug("Komenda /aa wpisana przez "+player.getName()+" przekierowano na /"+"spanel "+target);
				player.chat("/spanel "+target);
			}
			else player.sendMessage("Brak uprawnień");
		} else sender.sendMessage("Komenda dostępna tylko z poziomu gry.");
	 return true;
	}
}
