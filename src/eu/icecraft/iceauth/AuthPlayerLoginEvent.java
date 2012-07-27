package eu.icecraft.iceauth;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuthPlayerLoginEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private boolean onRegister;

	public AuthPlayerLoginEvent(Player target, boolean registerFlag) {
		player = target;
		onRegister = registerFlag;
	}

	public Player getPlayer() {
		return player;
	}

	public boolean isOnRegister() {
		return onRegister;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}