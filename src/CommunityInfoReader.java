package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CommunityInfoReader extends HTMLReader {
	private final static String urlDir = "https://ideascale.com/index/";
	private StatisticReader statsReader = null;
	
	public CommunityInfoReader() {
		super();
		prepareUserAgent();
		statsReader = new StatisticReader();
	}
	
	public List<HashMap<String,String>> getCommunitiesInfo() 
	throws Exception
	{
		List<HashMap<String,String>> comunities = new ArrayList<HashMap<String,String>>();
		List<String> letterDir = new ArrayList<String>(
					Arrays.asList("a","b","c","d","e","f","g","h","i","j","k",
							      "l","m","n","o","p","q","r","s","t","u","v",
							      "w","x","y","z"));
		for (String letter : letterDir) {
			System.out.println("Starting with communities of cat: " + letter.toUpperCase() + ".");
			String content = getUrlContent(urlDir+letter+".html");
	        Document doc = Jsoup.parse(content);	        
	        Elements liElements = doc.getElementsByTag("li");
	        for (Element li : liElements)
	        	if (li.children().size() > 0) {
	        		String link = li.child(0).attr("href");
	        		String name = li.text();
	        		if (link.contains("http://") && !inBlackList(link)) {
	        			HashMap<String,String> comunity = new HashMap<String,String>();
	        			comunity.put("name", name);
	        			comunity.put("url", link);
	        			System.out.println("Community: " + name);
	        			HashMap<String,String> comStatistics = statsReader.getCommunityStatistic(link);
	        			comunity.put("ideas", comStatistics.get("ideas"));
	        			comunity.put("members", comStatistics.get("members"));
	        			comunity.put("votes", comStatistics.get("votes"));
	        			comunity.put("comments", comStatistics.get("comments"));
	        			/*System.out.println(" - members: " + comStatistics.get("members") 
	        											  + ", ideas: " + comStatistics.get("ideas")
	        											  + ", votes: " + comStatistics.get("votes")
	        											  + ", comments: " + comStatistics.get("comments"));*/
	        			comunities.add(comunity);
	        		}
	        	}
	        System.out.println("Communities cat: " + letter.toUpperCase() + " finished.");
		}
		
		return comunities;
	}
	
	private Boolean inBlackList(String link) {
		List<String> blackList = new ArrayList<String>(
								 Arrays.asList("support.ideascale",
								 			   "blog.ideascale",
								 			   "twitter.com/ideascale",
								 			   "facebook.com/ideascale",
								 			   "www.surveyanalytics.com/?utm_source=IdeaScale",
								 			   "www.questionpro.com/?utm_source=IdeaScale",
								 			   "www.micropoll.com/?utm_source=IdeaScale",
								 			   "www.researchaccess.com/?utm_source=IdeaScale"));
		for (String item : blackList)
			if (link.contains(item))
				return true;
		return false;
	}
}
