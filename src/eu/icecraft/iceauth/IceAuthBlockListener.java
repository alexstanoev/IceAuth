package eu.icecraft.iceauth;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

public class IceAuthBlockListener extends BlockListener {

	private final IceAuth plugin;

	public IceAuthBlockListener(final IceAuth plugin) {
		this.plugin = plugin;
	}

	@Override
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

	@Override
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
