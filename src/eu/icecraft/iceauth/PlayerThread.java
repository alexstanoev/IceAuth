package eu.icecraft.iceauth;

public class PlayerThread implements Runnable {
	private IceAuth parent;

	public PlayerThread(IceAuth parent)
	{
		this.parent = parent;
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ex) {
				return;
			}
			this.parent.tpPlayers();
		}
	}
}