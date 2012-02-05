package eu.icecraft.iceauth;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class IceAuthEntityListener implements Listener {
	private final IceAuth plugin;

	public IceAuthEntityListener(final IceAuth plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageEvent event) {
		if(event.isCancelled()) {
			return;
		}
		Entity entity = event.getEntity();
		if(!(entity instanceof Player)) {
			return;
		}

		Player player = (Player) entity;
		if(!plugin.checkAuth(player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityTarget(EntityTargetEvent event) {
		if(event.isCancelled()) {
			return;
		}
		Entity entity = event.getEntity();
		if(entity instanceof Player) {
			return;
		}

		Entity target = event.getTarget();
		if(!(target instanceof Player)) {
			return;
		}
		Player targetPlayer = (Player) target;

		if(!plugin.checkAuth(targetPlayer)) {
			event.setCancelled(true);
		}
	}
}