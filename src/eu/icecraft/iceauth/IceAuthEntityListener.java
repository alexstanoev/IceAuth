package eu.icecraft.iceauth;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;

import eu.icecraft.iceauth.IceAuth.NLIData;

public class IceAuthEntityListener extends EntityListener {

	private final IceAuth plugin;

	public IceAuthEntityListener(final IceAuth plugin) {
		this.plugin = plugin;
	}

	@Override
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

	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if(!(entity instanceof Player)) {
			return;
		}

		Player player = (Player) entity;
		if(!plugin.checkAuth(player)) {
			// pweease work

			NLIData nli = plugin.notLoggedIn.get(player.getName());
			Location pos = nli.getLoc();

			List<ItemStack> inv = event.getDrops();
			player.getInventory().setContents((ItemStack[])inv.toArray());
			inv.clear();
			player.teleport(pos);
		}
	}

	@Override
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