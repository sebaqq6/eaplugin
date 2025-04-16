package pl.eadventure.plugin.API;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import pl.eadventure.plugin.Modules.GearScoreCalculator;
import pl.eadventure.plugin.Modules.MobFixer;
import pl.eadventure.plugin.PlayerData;
import pl.eadventure.plugin.Utils.Utils;
import pl.eadventure.plugin.Utils.print;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public class ProtocolLibAPI {
	ProtocolManager protocolManager;
	Plugin plugin;

	public ProtocolLibAPI(ProtocolManager protocolManager, Plugin plugin) {
		this.protocolManager = protocolManager;
		this.plugin = plugin;
		//fabricPacketListener();
		initGearScoreLore();
		disableAnimationWhenMobfixerWork();
		registerKeepAlive();
		disableVulcanAlert();
	}

	//==================================================================================================================
	private void disableVulcanAlert() {
		final List<String> BLOCKED_TEXTS = Arrays.asList(
				"{\"text\":\"ALERT\",\"italic\":false,\"color\":\"dark_red\",\"bold\":true}",
				"{\"text\":\"AC\",\"italic\":false,\"color\":\"dark_red\",\"bold\":true}"
		);

		protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SYSTEM_CHAT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				WrappedChatComponent chatComponent = packet.getChatComponents().read(0);

				if (chatComponent != null) {
					String json = chatComponent.getJson();
					for (String blocked : BLOCKED_TEXTS) {
						if (json.contains(blocked)) {
							PlayerData pd = PlayerData.get(event.getPlayer());
							if (pd.onLiveStream) {
								event.setCancelled(true);
							} else if (json.contains("MgrDesmond") || json.contains("JrRequeim")) {
								event.setCancelled(true);
							}
							break;
						}
					}
				}
			}
		});
	}


	//==================================================================================================================
	private static HashMap<UUID, Timestamp> lastOnline = new HashMap<>();

	private void registerKeepAlive() {
		protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.POSITION) {
			@Override
			public void onPacketReceiving(PacketEvent e) {
				Player player = e.getPlayer();
				Timestamp now = Timestamp.from(Instant.now());
				lastOnline.put(player.getUniqueId(), now);
				//print.info(player.getName() + " keep alive: " + now.getNanos());
			}
		});
	}

	private static final long TIMEOUT_MS = 1000;

	public static boolean isOnline(UUID playerUUID) {
		Timestamp lastSeen = lastOnline.get(playerUUID);
		if (lastSeen == null) {
			return false;
		}
		long now = System.currentTimeMillis();
		return (now - lastSeen.getTime()) <= TIMEOUT_MS;
	}

	public static long getLastSeen(UUID playerUUID) {
		Timestamp lastSeen = lastOnline.get(playerUUID);
		if (lastSeen == null) {
			return -1; // Gracz nigdy nie był online
		}
		long now = System.currentTimeMillis();
		return (now - lastSeen.getTime()) / 1000; // Konwersja z milisekund na sekundy
	}

	//==================================================================================================================
	public void fabricPacketListener() {
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
	}

	//-----------------------------------------GERA SCORE
	private void initGearScoreLore() {
		protocolManager.addPacketListener(new PacketAdapter(
				plugin,
				ListenerPriority.NORMAL,
				PacketType.Play.Server.SET_SLOT,
				PacketType.Play.Server.WINDOW_ITEMS
		) {

			@Override
			public void onPacketSending(PacketEvent event) {
				if (GearScoreCalculator.disableGs >= 2) return;
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

	//------------------------------------------------------MobFixer
	private void disableAnimationWhenMobfixerWork() {
		protocolManager.addPacketListener(new PacketAdapter(plugin,
				ListenerPriority.NORMAL,
				PacketType.Play.Server.DAMAGE_EVENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (MobFixer.isWorking()) {
					event.setCancelled(true);
				}
			}
		});
	}
}
