package pl.eadventure.plugin.FunEvents;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.gVar;

public class FunEventsCommands {
	private static final String mainCmd = "/fe";

	public static void onFunEventCommand(CommandSender sender, String[] args) {//need permission eadventureplugin.cmd.fe
		FunEventsManager fem = gVar.funEventsManager;
		if (args.length == 0) {
			Utils.commandUsageMessage(sender, String.format("/%s [start, stop, list, world]", mainCmd));
			return;
		}
		String command = args[0];
		//next args >= 1
		//komendy:
		/*
		/reload
		start nazwa_eventu
		stop

		 */
		switch (command) {
			//---------------------------------------------------------------------------------Fun Events Commands
			case "start" -> {//----------------------------------------------------------------/fe start nazwa_eventu
				if (args.length < 2) {
					Utils.commandUsageMessage(sender, "/fe start [event]");
					return;
				}
				String eventName = args[1];
				if (fem.startRecord(eventName, 60 * 5)) {
					sender.sendMessage(Utils.mm("<#00FF00>Otworzyłeś/aś zapisy na: <grey><bold>" + fem.getEvent(eventName).getEventName()));
				} else {
					sender.sendMessage(Utils.mm("<#FF0000>Błąd: Albo już trwają zapisy, albo zabawa już trwa."));
				}
			}
			case "stop" -> {
				FunEvent fe = fem.getActualFunEvent();
				if (fem.stopRecord()) {
					fe.setStatus(FunEvent.Status.FREE);
					sender.sendMessage(Utils.mm("<#FF0000><bold>Anulowano</bold> aktualnie trwające zapisy."));
				} else {
					sender.sendMessage(Utils.mm("<grey>W tym momencie nie trwają żadne zapisy."));
				}
			}
			case "forcefinish" -> {//----------------------------------------------------------------/fe forcefinish nazwa_eventu
				if (args.length < 2) {
					Utils.commandUsageMessage(sender, "/fe forcefinish [event]");
					return;
				}
				String eventName = args[1];
				if (fem.getEvent(eventName).finishEvent()) {
					sender.sendMessage(Utils.mm("<#FF0000><bold>Wymuszono</bold> zakończenie eventu: " + fem.getEvent(eventName).getEventName()));
				} else {
					sender.sendMessage(Utils.mm("<grey>Ten event obecnie nie trwa."));
				}
			}
			case "world" -> {
				if (sender instanceof Player player) {
					player.teleport(new Location(Bukkit.getWorld("world_utility"), -1, 65, 0));
				}
			}
			case "reload" -> {
				fem.registerEvents();
				sender.sendMessage(Utils.mm("<#FF0000>Przeładowano eventy."));
			}
			case "list" -> {
				sender.sendMessage("WIP, gui może here?");
			}
		}
	}
}
