package src;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CrawlLogger {
	
	static private FileHandler fileTxt;
	static private SimpleFormatter formatterTxt;

	static public void setup() throws IOException {
		Calendar cal = Calendar.getInstance();
		Date today = cal.getTime();
		DateFormat timeFormat = new SimpleDateFormat("dd-MM-yyyy");
		
		// Get the global logger to configure it
		Logger logger = Logger.getLogger("");

		logger.setLevel(Level.INFO);
		fileTxt = new FileHandler("logs/crawler_"+timeFormat.format(today)+".txt");

		// create txt Formatter
		formatterTxt = new SimpleFormatter();
		fileTxt.setFormatter(formatterTxt);
		logger.addHandler(fileTxt);
	}
}
