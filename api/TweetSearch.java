package api;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import src.Util;
import web.TweetRepliesReader;

public class TweetSearch extends TwitterApp {
	private final static Logger logger = Logger.getLogger(TweetSearch.class.getName());
	private static String REQUESTURL = "https://api.twitter.com/1.1/search/tweets.json?q=";
	private static String ARRAYTWS = "statuses";
	private static String TWRTCOUNTER = "retweet_count";
	private static String TWFVCOUNTER = "favorite_count";
	private static String TWTXT = "text";
	private static String TWDATE = "created_at";
	private static String TWAUTHOR = "user";
	private static String TWAUTHORHANDLE = "screen_name";
	private static String TWID = "id_str";
	private static String TWITTERURL = "http://twitter.com/";
	private TweetRepliesReader twRepReader = null;
    private static Integer MAX_ATTEMPTS = 10;
	
	
	public TweetSearch() {
		super();
		twRepReader = new TweetRepliesReader();;
	}
	
	public ArrayList<HashMap<String,Object>> GetTweets(String url) 
	throws Exception 
	{
		int attemptCounter = 0;
        
        String fullURL = REQUESTURL+URLEncoder.encode(url, "utf-8");
		
		Util.printMessage("Remaining requests  " + remainingRequests + " in this time window","info",logger);
			
		
		SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy",Locale.ENGLISH);
		ArrayList<HashMap<String,Object>> tweets = new ArrayList<HashMap<String,Object>>();
		
		Util.printMessage("Searching tweets for: " + url,"info",logger);
		
		UrlValidator urlValidator = new UrlValidator();
		if (urlValidator.isValid(url)) {
			HttpGet httpGet = new HttpGet(fullURL);
			consumer.sign(httpGet);
			
			HttpResponse response = null;
			if (remainingRequests == 0) {
				response = pause(httpGet);
			}
			else {
				response = doRequest(httpGet);
			}
			
			while (response.getStatusLine().getStatusCode() != 200 || attemptCounter <= MAX_ATTEMPTS) {
				if (response.getStatusLine().getStatusCode() == 401 ||
		        	response.getStatusLine().getStatusCode() == 406) {
		        	Util.printMessage("Ingnoring invalid URL: " + fullURL, "severe",logger);
		        	return tweets;
		        }
				else {
                    attemptCounter += 1;
					Util.printMessage("Wrong Twitter API response code, got: " + 
							  		  response.getStatusLine().getStatusCode() + 
							  		  " expected 200","info",logger);
					Thread.sleep(30000); //Wait for 30 seconds and try again
					response = doRequest(httpGet);
				}
			}
			
            if (attemptCounter <= MAX_ATTEMPTS) {
                ResponseHandler<String> handler = new BasicResponseHandler();
                String body = handler.handleResponse(response);
                
                Object obj = parser.parse(body);
                JSONObject jsonObj = (JSONObject) obj;
                JSONArray statuses = (JSONArray) jsonObj.get(ARRAYTWS);
                Iterator<JSONObject> iterator = statuses.iterator();
                while (iterator.hasNext()) {
                    JSONObject status = (JSONObject) iterator.next();
                    HashMap<String,Object> tweet = new HashMap<String,Object>();
                    tweet.put("retweets", status.get(TWRTCOUNTER));
                    tweet.put("favorites", status.get(TWFVCOUNTER));
                    Date tweetDateTime = formatter.parse((String) status.get(TWDATE));
                    tweet.put("datetime", tweetDateTime);
                    tweet.put("text", status.get(TWTXT));
                    JSONObject authorObj = (JSONObject) status.get(TWAUTHOR);
                    String author = (String) authorObj.get(TWAUTHORHANDLE);
                    tweet.put("author", author);
                    String statusURL = TWITTERURL+author+"/status/"+status.get(TWID);
                    tweet.put("id", status.get(TWID));
                    tweet.put("url", statusURL);
                    int replies = twRepReader.getReplies(statusURL);
                    tweet.put("replies", replies);
                    tweets.add(tweet);
                }
                if (tweets.size() > 0) {
                    Util.printMessage("Found " + tweets.size() + " tweets related to the community.","info",logger);
                }
            }
            else {
                Util.printMessage("Couldn't find the tweet related to: " + fullURL,"info",logger);
            }
		}
        else {
        	Util.printMessage("Ingnoring invalid URL: " + fullURL, "severe",logger);
        }
		
		return tweets;
	}
}
