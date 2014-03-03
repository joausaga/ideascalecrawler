package web;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import src.Crawler;
import src.Util;


public class StatisticReader extends HTMLReader {
	private final static Logger logger = Logger.getLogger(Crawler.class .getName());
	private final static String IDEAS_STATS = "ideas-stats";
	private final static String OTHER_STATS = "other-stats";
	private final static String IDEAS_IN_REVIEW = "tab-review";
	private final static String IDEAS_IN_PROGRESS = "tab-progress";
	private final static String IDEAS_COMPLETED = "tab-complete";
	private final static String FRAME_TAG = "iframe";
	private final static String FACEBOOK_STATS = "u_0_2";
	private final static String TWITTER_URL_P = "https://cdn.api.twitter.com/1/urls/count.json?url=";
	private final static String TWITTER_URL_S = "&callback=twttr.receiveCount";
	private final static String LOGO = "logo";
	private final static String EXPLANATION_TEXT = "client-txt";
	private final static String TABS = "listing-nav";
	private final static String IDEA_VOTES = "vote-number";
	private final static String IDEA_COMMENTS_TAG = "h2";
	private final static String IDEA_COMMENTS_ID = "comments";
	private final static String IDEA_DESCRIPTION_CLASS = "entry-content";
	private final static String IDEA_HREF_TAGS = "/a/ideas/tag/tags/";
	private final static String IDEA_TAGS_ATTR = "href";
	private final static String IDEA_SIMILAR_ID = "similar-idea-list";
	
	public StatisticReader() {
		super();
		prepareUserAgent();
	}
	
	public HashMap<String,Object> getCommunityStatistic(String url)  
	{
		HashMap<String,Object> statistics = new HashMap<String,Object>();
		statistics.put("ideas", null);
		statistics.put("ideas_in_review", null);
		statistics.put("ideas_in_progress", null);
		statistics.put("ideas_completed", null);
		statistics.put("comments", null);
		statistics.put("votes", null);
		statistics.put("members", null);
		statistics.put("facebook", null);
		statistics.put("twitter", null);
		statistics.put("logo", null);
		statistics.put("explanation_text", null);
		statistics.put("tabs", null);
		
		String content;
		String textElement;
		try {
			content = getUrlContent(Util.toURI(url));
			Document doc = Jsoup.parse(content);		       
			
			Element ideasStats = doc.getElementById(IDEAS_STATS);
			if (ideasStats != null) {
				textElement = ideasStats.child(0).text();
			    statistics.put("ideas", replaceThounsandSymbol(textElement));
			}
	        	
			Element otherStats = doc.getElementById(OTHER_STATS);
			if (otherStats != null) {
				Elements childrenStats = otherStats.children();
			    statistics.put("comments", replaceThounsandSymbol(childrenStats.get(0).text()));
			    statistics.put("votes",replaceThounsandSymbol(childrenStats.get(1).text()));
			    statistics.put("members", replaceThounsandSymbol(childrenStats.get(2).text()));
			}
			
			Element ideasInReview = doc.getElementById(IDEAS_IN_REVIEW);
			if (ideasInReview != null) {
				textElement = ideasInReview.child(0).text();
				textElement = textElement.replaceAll("[^0-9]+", " ");
				statistics.put("ideas_in_review",textElement.trim());
			}
			
			Element ideasInProgress = doc.getElementById(IDEAS_IN_PROGRESS);
			if (ideasInProgress != null) {
				textElement = ideasInProgress.child(0).text();
				textElement = textElement.replaceAll("[^0-9]+", " ");
				statistics.put("ideas_in_progress",textElement.trim());
			}
			
			Element ideasCompleted = doc.getElementById(IDEAS_COMPLETED);
			if (ideasCompleted != null) {
				textElement = ideasCompleted.child(0).text();
				textElement = textElement.replaceAll("[^0-9]+", " ");
				statistics.put("ideas_completed",textElement.trim());
			}
			
			/*Element logo = doc.getElementById(LOGO);
			if (logo != null)
				statistics.put("logo", "yes");*/
			
			Element explanation = doc.getElementById(EXPLANATION_TEXT);
			if (explanation != null) {
				textElement = explanation.text();
				if (!textElement.isEmpty())
					statistics.put("explanation_text", "yes");
			}
			
			statistics.put("tabs", getTabsURL(doc));
			
			HashMap<String,Object> auxStats = getSNCounters(doc,url);
			statistics.put("facebook",auxStats.get("facebook"));
			statistics.put("twitter",auxStats.get("twitter"));
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
		}
		
		return statistics;
	}
	
	public HashMap<String,Object> getIdeaStatistics(String communityURL,
													String ideaURL) 
	throws Exception {
		HashMap<String,Object> statistics = new HashMap<String,Object>();
		String ideaURLEncoded = URLEncoder.encode(ideaURL, "utf-8");
		String fullURL = communityURL+ideaURLEncoded;
		statistics.put("description", null);
		statistics.put("tags", null);
		statistics.put("facebook", null);
		statistics.put("twitter", null);
		statistics.put("comments", null);
		statistics.put("score", null);
		
		String content = getUrlContent(Util.toURI(communityURL+ideaURL));
		Document doc = Jsoup.parse(content);	
		
		//Description
		Elements desc = doc.getElementsByClass(IDEA_DESCRIPTION_CLASS);
		String ideaDescription = "";
		for (Element paragraph : desc) ideaDescription += paragraph.text();
		statistics.put("description", ideaDescription);
		//Tags
		Elements tags = doc.getElementsByAttributeValueMatching(IDEA_TAGS_ATTR, IDEA_HREF_TAGS);
		if (tags != null) {
			String ideaTags = "";
			int numTags = tags.size();
			for (int i = 0; i < numTags; i++) {
				if (i != (numTags - 1))
					ideaTags += tags.get(i).text() + ", ";
				else
					ideaTags += tags.get(i).text();
			}
			statistics.put("tags", ideaTags);
		}
		else {
			statistics.put("tags", null);
		}
		//Social Networks
		HashMap<String,Object> auxStats = getSNCounters(doc,fullURL);
		statistics.put("facebook", auxStats.get("facebook"));
		statistics.put("twitter",auxStats.get("twitter"));
		
		Element commentElem = doc.getElementById(IDEA_COMMENTS_ID);
		if (commentElem != null) {
			if (commentElem.children().size() > 0) {
				for (Element e : commentElem.children()) {
					if (e.tagName() == IDEA_COMMENTS_TAG && e.hasClass(IDEA_COMMENTS_TAG))
						statistics.put("comments", e.child(0).text().replaceAll("[()]",""));
				}
			}
			else {
				statistics.put("comments", "0");
			}
		}
		else {
			statistics.put("comments", null);
		}
		
		Elements scoreElem = doc.getElementsByClass(IDEA_VOTES);
		if (scoreElem != null) {
			if (scoreElem.text().isEmpty())
				statistics.put("score", "0");
			else
				statistics.put("score", scoreElem.text());
		}
		else {
			statistics.put("score", null);
		}
		
		Element similarIdeas = doc.getElementById(IDEA_SIMILAR_ID);
		if (similarIdeas != null)
				statistics.put("similar", similarIdeas.children().size());	
		else
			statistics.put("similar", 0);
		
		return statistics;
	}
	
	public ArrayList<HashMap<String,String>> getTabsURL(Document doc) throws Exception {
		ArrayList<HashMap<String,String>> tabs = new ArrayList<HashMap<String,String>>();
		
		String numIdeas = "";
		Element navTabs = doc.getElementById(TABS);
		if (navTabs != null) {
			for (Element li : navTabs.children()) {
				Element aLink = li.child(0);
				numIdeas = aLink.text().replaceAll("[^0-9]+", " ").trim();
				if (Integer.parseInt(numIdeas) != 0) {   //Save only tabs whose list of ideas is not empty
					HashMap<String,String> tab = new HashMap<String,String>();
					tab.put("url", aLink.attr("href"));
					tab.put("ideas", numIdeas);
					tabs.add(tab);
				}
			}
		}
		else {
			throw new Exception ("Error, page without tabs.");
		}
		return tabs;
	}
	
    private String replaceThounsandSymbol(String str) {
    	return str.replace("K","000");
    }
	
    public HashMap<String,Object> getSNCounters(Document doc, String url) 
    throws Exception  
	{
    	HashMap<String,Object> snCounters = new HashMap<String,Object>();
		snCounters.put("facebook", null);
		snCounters.put("twitter", null);
    	
		Elements frameTag = doc.getElementsByTag(FRAME_TAG);
		if (!frameTag.isEmpty()) {
			Element facebookTag = frameTag.first();   //Should be the Facebook one
			String content = getUrlContent(Util.toURI(facebookTag.attr("src")));
			doc = Jsoup.parse(content);
			Element facebookStats = doc.getElementById(FACEBOOK_STATS);
			if (facebookStats != null) {
				String shared = facebookStats.child(0).text();
				shared = shared.replaceAll("[^0-9]+", " ");
				shared = shared.trim();
				if (shared.isEmpty())
					snCounters.put("facebook","0");						
				else
					snCounters.put("facebook",shared);
			}
			
			//Get Twitter counter
			String twURL = TWITTER_URL_P + URLEncoder.encode(url, "utf-8") + 
				    	   TWITTER_URL_S;
			content = getUrlContent(Util.toURI(twURL));
			doc = Jsoup.parse(content);
			String textElement = doc.getElementsByTag("body").text();
			char twCounter = textElement.charAt(textElement.indexOf(":") + 1);
			snCounters.put("twitter", Character.toString(twCounter));
		}
		
    	return snCounters;
	}
    
}
