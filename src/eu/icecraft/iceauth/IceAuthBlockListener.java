package eu.icecraft.iceauth;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class IceAuthBlockListener implements Listener {
	private final IceAuth plugin;

	public IceAuthBlockListener(final IceAuth plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if(event.isCancelled()) {
			return;
		}
		Player players = event.getPlayer();
		if(!plugin.checkAuth(players)) {
			plugin.msgPlayerLogin(players);
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event) {
		if(event.isCancelled()) {
			return;
		}
		Player players = event.getPlayer();

		if(!plugin.checkAuth(players)) {
			plugin.msgPlayerLogin(players);
			event.setCancelled(true);
		}
	}

}
