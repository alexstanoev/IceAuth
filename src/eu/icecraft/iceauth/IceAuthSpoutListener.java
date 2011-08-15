package eu.icecraft.iceauth;

import org.getspout.spoutapi.event.inventory.InventoryListener;
import org.getspout.spoutapi.event.inventory.InventoryOpenEvent;
//import org.getspout.spoutapi.event.inventory.InventoryPlayerClickEvent;

public class IceAuthSpoutListener extends InventoryListener {

	private IceAuth plugin;

	public IceAuthSpoutListener(IceAuth instance) {
		plugin = instance;
	}
	
	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {
		
		System.out.println("inv open "+ event.getPlayer().getName());
		
        if(event.isCancelled()) {
            return;
        }
		
		if(plugin.checkAuth(event.getPlayer())) {
			event.setCancelled(true);
		}
		
	}
	
	/*
	@Override
	public void onInventoryCraft(InventoryOpenEvent event) {
	
	}
	
	@Override
	public void onInventoryPlayerClick(InventoryPlayerClickEvent event) {
		
		
	}
	*/
}
