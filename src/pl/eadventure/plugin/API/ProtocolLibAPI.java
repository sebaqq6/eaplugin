package pl.eadventure.plugin.API;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.Modules.GearScoreCalculator;
import pl.eadventure.plugin.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class ProtocolLibAPI {
	ProtocolManager protocolManager;
	Plugin plugin;

	public ProtocolLibAPI(ProtocolManager protocolManager, Plugin plugin) {
		this.protocolManager = protocolManager;
		this.plugin = plugin;
		//fabricPacketListener();
		initGearScoreLore();
	}

	/*public void fabricPacketListener() {
		protocolManager.addPacketListener(new PacketAdapter(
				plugin,
				ListenerPriority.NORMAL,
				PacketType.Play.Client.getInstance().values()
		) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				if (!print.getDebug()) return;
				Player player = e.getPlayer();
				PlayerData pd = PlayerData.get(player);
				if (pd.onlineHours < 1) {
					if (pd.clientBrand.equalsIgnoreCase("Fabric")) {
						PacketContainer packet = e.getPacket(); // Uzyskaj pakiet z wydarzenia
						String packetName = packet.getType().name(); // Nazwa pakietu
						if (packetName.contains("Dynamic") || packetName.contains("POSITION") || packetName.contains("LOOK")) {
							return;
						}
						StringBuilder packetDetails = new StringBuilder();
						// Pobierz uniwersalny modyfikator do iteracji przez wszystkie dane
						StructureModifier<Object> modifier = packet.getModifier();

						// Iteruj przez wszystkie wartości w pakiecie
						for (int i = 0; i < modifier.size(); i++) {
							Object value = modifier.readSafely(i);
							if (value != null) {
								packetDetails.append(value.getClass().getSimpleName())
										.append(": ")
										.append(value.toString())
										.append("\n");
							}
						}


						print.debug(String.format("[pakiet] %s - %s ->\n%s", player.getName(), packetName, packetDetails));
						print.debug("---------------------------------------------------------------------------");
					}
				}
			}
		});
	}*/

	private void initGearScoreLore() {
		protocolManager.addPacketListener(new PacketAdapter(
				plugin,
				ListenerPriority.NORMAL,
				PacketType.Play.Server.SET_SLOT,
				PacketType.Play.Server.WINDOW_ITEMS
		) {

			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;//disable for creative
				PacketContainer packet = event.getPacket();
				if (packet.getType() == PacketType.Play.Server.SET_SLOT || packet.getType() == PacketType.Play.Server.WINDOW_ITEMS) {
					//----------------------------------------SEND PACKET WINDOW ITEMSd
					if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
						//print.debug(packet.getType().toString());
						List<ItemStack> itemStacks = packet.getItemListModifier().read(0);
						for (ItemStack itemStack : itemStacks) {
							if (itemStack.getType() == Material.AIR) continue;
							//print.debug(itemStack.toString());
							GearScoreCalculator gsc = new GearScoreCalculator(itemStack);
							int gs = gsc.calcGearScore();
							if (gs == 0) continue;
							if (itemStack != null) {
								ItemMeta meta = itemStack.getItemMeta();
								if (meta != null && meta.lore() != null) {//with lore
									List<Component> loreLines = new ArrayList<>();
									loreLines.addAll(meta.lore());
									ListIterator<Component> iterator = loreLines.listIterator();
									while (iterator.hasNext()) {
										Component loreLine = iterator.next();
										Pattern pattern = Pattern.compile("\\{gs\\}");
										Component newLore = loreLine.replaceText(builder -> builder.match(pattern).replacement(Utils.mm("<!i>" + gsc.getGsValueColored(gs))));
										iterator.set(newLore);
									}
									meta.lore(loreLines);
									itemStack.setItemMeta(meta);
									packet.getItemListModifier().write(0, itemStacks);
								} else if (meta != null) {//without lore
									//print.debug(itemStack.getType().toString());
									meta.lore(List.of(gsc.getFormatedGsStock()));
									itemStack.setItemMeta(meta);
									packet.getItemListModifier().write(0, itemStacks);
								}
							}
						}
					}
					//------------------------------------------------------PACKET UPDATE SLOT ITEMS (pickup, change slot etc)
					else if (packet.getType() == PacketType.Play.Server.SET_SLOT) {
						//print.debug(packet.getType().toString());
						StructureModifier<ItemStack> itemStackStructureModifier = packet.getItemModifier();
						for (int i = 0; i < itemStackStructureModifier.size(); i++) {
							ItemStack itemStack = itemStackStructureModifier.read(i);
							if (itemStack.getType() == Material.AIR) continue;
							//print.debug(itemStack.toString());
							GearScoreCalculator gsc = new GearScoreCalculator(itemStack);
							int gs = gsc.calcGearScore();
							if (gs == 0) continue;
							if (itemStack != null) {
								ItemMeta meta = itemStack.getItemMeta();
								if (meta != null && meta.lore() != null) {
									List<Component> loreLines = new ArrayList<>();
									loreLines.addAll(meta.lore());
									ListIterator<Component> iterator = loreLines.listIterator();
									while (iterator.hasNext()) {
										Component loreLine = iterator.next();
										Pattern pattern = Pattern.compile("\\{gs\\}");
										Component newLore = loreLine.replaceText(builder -> builder.match(pattern).replacement(Utils.mm("<!i>" + gsc.getGsValueColored(gs))));
										iterator.set(newLore);
									}
									meta.lore(loreLines);
									itemStack.setItemMeta(meta);
									itemStackStructureModifier.write(i, itemStack);
								} else if (meta != null) {//without lore
									//print.debug(itemStack.getType().toString());
									meta.lore(List.of(gsc.getFormatedGsStock()));
									itemStack.setItemMeta(meta);
									itemStackStructureModifier.write(i, itemStack);
								}
							}
						}
					}
				}
			}
		});
	}
}
