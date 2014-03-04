package api;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Logger;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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
	private final Integer TIME_WINDOW = 15; //For the Twitter API the time window is 15 minutes
	private OAuthConsumer consumer = null;
	private HttpClient client = null;
	private JSONParser parser = null;
	private TweetRepliesReader twRepReader = null;
	private Integer remainingRequests;
	private boolean firstRequest;
	private static Integer API_LIMIT = 180;
	
	
	public TweetSearch() {
		consumer = new CommonsHttpOAuthConsumer(APIKEY,APISECRET);
		consumer.setTokenWithSecret(ACCESSTOKEN, ACCESSSECRET);
		parser = new JSONParser();
		twRepReader = new TweetRepliesReader();
		firstRequest = true;
		remainingRequests = API_LIMIT;
		client = HttpClients.createDefault();
	}
	
	public ArrayList<HashMap<String,Object>> GetTweets(String url, Timer timer) 
	throws Exception 
	{
		String fullURL = REQUESTURL+URLEncoder.encode(url, "utf-8");
		
		Util.printMessage("Remaining requests  " + remainingRequests + " in this time window","info",logger);
		
		if (firstRequest) {
			timer.start();
			firstRequest = false;
		}	
		
		SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy",Locale.ENGLISH);
		ArrayList<HashMap<String,Object>> tweets = new ArrayList<HashMap<String,Object>>();
		
		Util.printMessage("Searching tweets for: " + url,"info",logger);
		
		UrlValidator urlValidator = new UrlValidator();
		if (urlValidator.isValid(url)) {
			HttpGet httpGet = new HttpGet(fullURL);
			consumer.sign(httpGet);
			
			if (remainingRequests == 0) {
				System.out.println("Remaining requests " + remainingRequests);
				pause(timer,httpGet);
				System.out.println("Back from the pause");
			}
			
			HttpResponse response = client.execute(httpGet);
			
	        if (response.getStatusLine().getStatusCode() == 200) {
	        	setRemainingRequests(response);
	        	
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
	        else if (response.getStatusLine().getStatusCode() == 401 ||
	        		 response.getStatusLine().getStatusCode() == 406) {
	        	Util.printMessage("Ingnoring invalid URL: " + fullURL, "severe",logger);
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
	
	private void pause(Timer timer, HttpGet httpGet) 
	throws InterruptedException, ClientProtocolException, IOException {
		HttpResponse response;
		
		long waitingTime = TIME_WINDOW-timer.getElapsedTime();
		Util.printMessage("API request limit reached, we have to wait " + 
			    		  waitingTime + " minutes for the next time window", 
			    		  "info", logger);
		Thread.sleep((TIME_WINDOW-timer.getElapsedTime())*60000);
		HttpClient pauseClient = HttpClients.createDefault();
		response = pauseClient.execute(httpGet);
		setRemainingRequests(response);
		while (remainingRequests == 0) {
			Thread.sleep(1000);
			response = pauseClient.execute(httpGet);
			setRemainingRequests(response);
			System.out.println("Remaining request " + remainingRequests);
		}
		timer.setStartingTime();
	}
	
	private void setRemainingRequests(HttpResponse response) 
	throws ClientProtocolException, IOException 
	{	
        Header[] headers = response.getAllHeaders();
        for(Header header:headers) {
        	if (header.getName().equalsIgnoreCase("x-rate-limit-remaining"))
        		remainingRequests = Integer.parseInt(header.getValue());
    	}
	}
}
