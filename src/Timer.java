package src;

import java.util.logging.Level;
import java.util.logging.Logger;

import api.TweetSearch;

public class Timer extends Thread {
	public static final Logger logger = Logger.getLogger(Timer.class .getName()); 
	private boolean running = true;
	private long startTime;
	private long endTime;
	private long elapsedTime;
	
	TweetSearch ts;
	
	public Timer(TweetSearch ts) {
		this.ts = ts;
	}
	
	public void run() {
		startTime = System.currentTimeMillis();
		
		while (running) {
			try {
				Thread.sleep(1000);
				endTime = System.currentTimeMillis();
				elapsedTime = (endTime - startTime) / (60 * 1000) % 60;
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.log(Level.SEVERE,e.getMessage(),e);
			}
		}
	}
	
	public void setStartingTime() { 
		startTime = System.currentTimeMillis();
	}
	
	public void terminate() {
		running = false;
	}
	
	public long getElapsedTime() {
		return elapsedTime;
	}
}
