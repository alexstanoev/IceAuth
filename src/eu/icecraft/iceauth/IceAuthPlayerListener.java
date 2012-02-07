package eu.icecraft.iceauth;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class IceAuthPlayerListener implements Listener {
	private IceAuth plugin;
	public static Map<String, String> shouldBeCancelled = new HashMap<String, String>();

	public IceAuthPlayerListener(IceAuth instance) {
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLoginEarly(PlayerLoginEvent event) {
		if(event.getResult() != Result.ALLOWED || event.getPlayer() == null) {
			return;
		}

		Player player = event.getPlayer();
		String playername = player.getName();

		for(Player p : plugin.getServer().getOnlinePlayers()) {
			if(p.getName().toLowerCase().equals(event.getPlayer().getName().toLowerCase()) && plugin.checkAuth(p)) {
				event.disallow(Result.KICK_OTHER, "There's an user logged in with that name!");
				shouldBeCancelled.put(playername, "There's an user logged in with that name!");
			}
		}

		if((!playername.matches("[a-zA-Z0-9_?]*"))
				|| (playername.length() > 16)
				|| (playername.length() < 3)
				|| (playername.equalsIgnoreCase("Notch")) // Enough Notch'es already!
				|| (playername.equalsIgnoreCase("Player"))) {
			event.disallow(Result.KICK_OTHER, "Name contained disallowed characters or was Player/Notch");
			shouldBeCancelled.put(playername, "Name contained disallowed characters or was Player/Notch");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLoginLate(PlayerLoginEvent event) { // That event should really be cancelled to prevent serious exploits
		if(event.getPlayer() == null) return;

		if(event.getResult() == Result.ALLOWED && shouldBeCancelled.containsKey(event.getPlayer().getName())) {
			System.out.println("[IceAuth] Some plugin allowed a cancelled login event! Disabling at MONITOR level.");
			event.disallow(Result.KICK_OTHER, shouldBeCancelled.get(event.getPlayer().getName()));
		}

		shouldBeCancelled.remove(event.getPlayer().getName());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerKick(PlayerKickEvent event) {
		if(event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();

		if(event.getReason().contains("logged in from another location")) {
			if(plugin.checkAuth(player)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if(event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();

		boolean regged = plugin.isRegistered(player.getName());

		if(player.getHealth() <= 0) {
			player.teleport(player.getWorld().getSpawnLocation());
		} else {
			if(plugin.checkInvEmpty(player.getInventory().getContents()) && !plugin.isInvCacheEmpty(player.getName())) {
				plugin.restoreInv(player, true);
			}
		}

		if(!regged) {
			if(plugin.firstSpawn != null) player.teleport(plugin.firstSpawn);
			if(plugin.giveKits) plugin.giveKits(player);
		}

		plugin.addPlayerNotLoggedIn(player, player.getLocation(), regged);

		plugin.msgPlayerLogin(player);

		player.setGameMode(GameMode.SURVIVAL);

		player.getInventory().clear();
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if(event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();

		plugin.removePlayerCache(player);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(PlayerChatEvent event) {
		if(event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();

		if(!plugin.checkAuth(player)) {
			plugin.msgPlayerLogin(player);
			event.setMessage("");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if(event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();
		String commandLabel = event.getMessage().split(" ")[0];

		if(plugin.checkAuth(player)) {
			return;
		}
		if(commandLabel.equalsIgnoreCase("/register")) {
			return;
		}
		if(commandLabel.equalsIgnoreCase("/login")) {
			return;
		}
		if(commandLabel.equalsIgnoreCase("/l")) {
			return;
		}

		plugin.msgPlayerLogin(player);

		event.setMessage("/notloggedin");
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if(event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();
		if(!plugin.checkAuth(player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if(event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();
		if(!plugin.checkAuth(player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.isCancelled() || event.getPlayer() == null) {
			return;
		}
		Player player = event.getPlayer();
		if(!plugin.checkAuth(player)) {
			event.setCancelled(true);
		}
	}
}