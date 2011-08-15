package eu.icecraft.iceauth;

public class PlayerThread implements Runnable {
	private IceAuth parent;
	private int threadRuns = 0;
	
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
			
			threadRuns++;
			if(threadRuns == 11) {
				threadRuns = 0;
				this.parent.tpPlayers(true);
			} else {
				this.parent.tpPlayers(false);
			}
		}
	}
}