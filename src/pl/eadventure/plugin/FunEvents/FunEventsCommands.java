package pl.eadventure.plugin.FunEvents;

import org.bukkit.command.CommandSender;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

public class FunEventsCommands {
	private static final String mainCmd = "/fe";
	public void onFunEventCommand(CommandSender sender, String[] args) {//need permission eadventureplugin.cmd.fe
		FunEventsManager fem = gVar.funEventsManager;
		if(args.length == 0) {
			Utils.commandUsageMessage(sender, String.format("/%s [zapisy-start, zapisy-stop]", mainCmd));
			return;
		}
		String command = args[0];
		//next args >= 1
		switch (command) {
			//---------------------------------------------------------------------------------Fun Events Commands
			case "zapisy-start" -> {//----------------------------------------------------------------/fe zapisy-start
				if(fem.startRecord("parkour", 30)) {
					sender.sendMessage("Startuje!");
				} else {
					sender.sendMessage("Tylko jedne zapisy mogą trwać w tym samym momencie.");
				}
			}
			case "zapisy-stop" -> {
				if(fem.stopRecord()) {
					sender.sendMessage("Stopuje!");
				} else {
					sender.sendMessage("W tym momencie nie trwają zapisy.");
				}

			}
		}
	}
}
