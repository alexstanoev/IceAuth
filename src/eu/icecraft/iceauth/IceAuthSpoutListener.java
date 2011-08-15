package eu.icecraft.iceauth;

import org.getspout.spoutapi.event.inventory.InventoryListener;
import org.getspout.spoutapi.event.inventory.InventoryOpenEvent;

public class IceAuthSpoutListener extends InventoryListener {

	private IceAuth plugin;

	public IceAuthSpoutListener(IceAuth instance) {
		plugin = instance;
	}
	
	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {
		
        if(event.isCancelled()) {
            return;
        }
		
		if(!plugin.checkAuth(event.getPlayer())) {
			event.setCancelled(true);
		}
		
	}

}
