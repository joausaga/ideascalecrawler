package src;

import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class StatisticReader extends HTMLReader {
	private final static String IDEAS_STATS = "ideas-stats";
	private final static String OTHER_STATS = "other-stats";
	
	public StatisticReader() {
		super();
		prepareUserAgent();
	}
	
	public HashMap<String,String> getCommunityStatistic(String url)  
	{
		HashMap<String,String> statistics = new HashMap<String,String>();
		statistics.put("ideas", "unknown");
		statistics.put("comments", "unknown");
		statistics.put("votes", "unknown");
		statistics.put("members", "unknown");
		
		String content;
		try {
			content = getUrlContent(url);
			Document doc = Jsoup.parse(content);
		        
			Element ideasStats = doc.getElementById(IDEAS_STATS).child(0);
			if (ideasStats != null)
			    statistics.put("ideas", replaceThounsandSymbol(ideasStats.text()));
	        	
			Elements otherStats = doc.getElementById(OTHER_STATS).children();
			if (otherStats != null) {
			    statistics.put("comments", replaceThounsandSymbol(otherStats.get(0).text()));
			    statistics.put("votes",replaceThounsandSymbol(otherStats.get(1).text()));
			    statistics.put("members", replaceThounsandSymbol(otherStats.get(2).text()));
			}
			return statistics;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return statistics;
		}
       
        
        
	}

    private String replaceThounsandSymbol(String str) {
	return str.replace("K","000");
    }
	
}
