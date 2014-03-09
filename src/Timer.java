package src;

import java.util.logging.Level;
import java.util.logging.Logger;

import api.TweetSearch;

public class Timer extends Thread {
	public static final Logger logger = Logger.getLogger(Timer.class .getName()); 
	private final Integer TIME_WINDOW = 15; //For the Twitter API the time window is 15 minutes
	private boolean running = true;
	private long startTime;
	private long endTime;
	private long elapsedTime;
	
	TweetSearch ts;
	
	public Timer(TweetSearch ts) {
		this.ts = ts;
	}
	
	public void run() {
		startTime = 0;
		endTime = 0;
		
		while (running) {
			try {
				Thread.sleep(1000);
				endTime += 1;
				elapsedTime = (endTime - startTime) % 60;
				if (elapsedTime >= TIME_WINDOW)
					reset();
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.log(Level.SEVERE,e.getMessage(),e);
			}
		}
	}
	
	public void reset() { 
		startTime = 0;
		endTime = 0;
	}
	
	public String toString() {
		return "Start Counter: " + startTime + " End Counter: " + endTime;
	}
	
	public void terminate() {
		running = false;
	}
	
	public long getElapsedTime() {
		return elapsedTime;
	}
}
