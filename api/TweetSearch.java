package api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import src.Timer;
import src.Util;
import web.TweetRepliesReader;

public class TweetSearch {
	private final static Logger logger = Logger.getLogger(TweetSearch.class.getName());
	private static String ACCESSTOKEN = "156641445-h2WruI5Ha5Z6gyBWeyRrH1zp9VwBKJdPz5sCVzVK";
	private static String ACCESSSECRET = "iH5VgiHOLEo6JYyZPPJZ4X94yFZDb2N9r7h8MN7R4PRVU";
	private static String APIKEY = "qL2ne3j89Y6wbFbGq1FaA";
	private static String APISECRET= "7ERg6sHqf7AaWAv4te8ZsDcQusx7Q27Nw93ZyU6k";
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
	private OAuthConsumer consumer = null;
	private CloseableHttpClient client = null;
	private JSONParser parser = null;
	private TweetRepliesReader twRepReader = null;
	private Integer requestCounter = 0;
	private boolean firstRequest;
	
	
	public TweetSearch() {
		consumer = new CommonsHttpOAuthConsumer(APIKEY,APISECRET);
		consumer.setTokenWithSecret(ACCESSTOKEN, ACCESSSECRET);
		client = HttpClients.createDefault();
		parser = new JSONParser();
		twRepReader = new TweetRepliesReader();
		firstRequest = true;
	}
	
	public ArrayList<HashMap<String,Object>> GetTweets(String url, Timer timer) 
	throws Exception 
	{
		String fullURL = REQUESTURL+URLEncoder.encode(url, "utf-8");
		
		if (firstRequest) {
			timer.start();
			firstRequest = false;
		}
		
		if (requestCounter >= 180)
			Thread.sleep(timer.getElapsedTime()*60000);
			
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		ArrayList<HashMap<String,Object>> tweets = new ArrayList<HashMap<String,Object>>();
		
		Util.printMessage("Searching tweets for the community: " + url,"info",logger);
		
		UrlValidator urlValidator = new UrlValidator();
		if (urlValidator.isValid(url)) {
			
			HttpGet httpGet = new HttpGet(fullURL);
			consumer.sign(httpGet);
			
			CloseableHttpResponse response = client.execute(httpGet);
			
			requestCounter += 1;
			
	        if (response.getStatusLine().getStatusCode() == 200) {
	        	ResponseHandler<String> handler = new BasicResponseHandler();
				String body = handler.handleResponse(response);
				response.close();
				
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
	        	throw new IOException("Wrong Twitter API response code, got: " + 
	        						  response.getStatusLine().getStatusCode() + 
	        						  " expected 200");
	        }
		}
        else {
        	Util.printMessage("Ingnoring invalid URL: " + fullURL, "severe",logger);
        }
		
		return tweets;
	}
	
	public void resetRequestCounter() {
		Util.printMessage("New time window, reseting the request counter.","info",logger);
		requestCounter = 0;
	}
	
}
