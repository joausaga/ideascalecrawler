package web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import src.Util;

public class TweetRepliesReader extends HTMLReader {
	private static String IDREPLIESLIST = "stream-items-id";
	
	
	public TweetRepliesReader() {
		super();
		prepareUserAgent();
	}
	
	public int getReplies(String twURL) throws Exception  {
    	
		String content = getUrlContent(Util.toURI(twURL));
		Document doc = Jsoup.parse(content);
		int replies_counter = 0;
    	
		Element repliesList = doc.getElementById(IDREPLIESLIST);
		if (repliesList != null) {
			Elements replies = repliesList.children();
			replies_counter = replies.size();
		}
		
    	return replies_counter;
    }
	
}
