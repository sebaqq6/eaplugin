package pl.eadventure.plugin.API;

import com.ssomar.score.api.executableitems.ExecutableItemsAPI;
import com.ssomar.score.api.executableitems.config.ExecutableItemsManagerInterface;
import org.bukkit.inventory.ItemStack;

public class EiAPI {
	public static int getItemUsage(ItemStack itemStack) {
		return ExecutableItemsAPI.getExecutableItemObject(itemStack).getUsage();
	}
}
