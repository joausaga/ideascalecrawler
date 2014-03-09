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
import org.apache.http.client.ClientProtocolException;
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


public class TweetUpdater {
	private final static Logger logger = Logger.getLogger(TweetSearch.class.getName());
	private static String ACCESSTOKEN = "156641445-h2WruI5Ha5Z6gyBWeyRrH1zp9VwBKJdPz5sCVzVK";
	private static String ACCESSSECRET = "iH5VgiHOLEo6JYyZPPJZ4X94yFZDb2N9r7h8MN7R4PRVU";
	private static String APIKEY = "qL2ne3j89Y6wbFbGq1FaA";
	private static String APISECRET= "7ERg6sHqf7AaWAv4te8ZsDcQusx7Q27Nw93ZyU6k";
	private static String REQUESTURL = "https://api.twitter.com/1.1/statuses/show/";
	private static String ARRAYTWS = "statuses";
	private static String TWRTCOUNTER = "retweet_count";
	private static String TWFVCOUNTER = "favorite_count";
	private static String TWAUTHOR = "user";
	private static String TWAUTHORHANDLE = "screen_name";
	private static String TWID = "id_str";
	private static String TWITTERURL = "http://twitter.com/";
	private final Integer TIME_WINDOW = 15; //For the Twitter API the time window is 15 minutes
	private OAuthConsumer consumer = null;
	private CloseableHttpClient client = null;
	private JSONParser parser = null;
	private TweetRepliesReader twRepReader = null;
	private Integer remainingRequests;
	private boolean firstRequest;
	private static Integer API_LIMIT = 180;
	
	
	public TweetUpdater() {
		consumer = new CommonsHttpOAuthConsumer(APIKEY,APISECRET);
		consumer.setTokenWithSecret(ACCESSTOKEN, ACCESSSECRET);
		client = HttpClients.createDefault();
		parser = new JSONParser();
		twRepReader = new TweetRepliesReader();
		firstRequest = true;
		remainingRequests = API_LIMIT;
	}
	
	public HashMap<String,Integer> updateTweetMetrics(String idTweet, Timer timer) 
	throws Exception 
	{
		HashMap<String,Integer> newMetrics = new HashMap<String,Integer>();
		
		String fullURL = REQUESTURL+idTweet+".json";
		
		Util.printMessage("Remaining requests  " + remainingRequests + " in this time window","info",logger);
		
		if (firstRequest) {
			timer.start();
			firstRequest = false;
		}	
		
		Util.printMessage("Updating metrics of the tweet: " + idTweet,"info",logger);
		
		HttpGet httpGet = new HttpGet(fullURL);
		consumer.sign(httpGet);
		
		if (remainingRequests == 0) pause(timer,httpGet);
		
		CloseableHttpResponse response = client.execute(httpGet);
		
        if (response.getStatusLine().getStatusCode() == 200) {
        	setRemainingRequests(response);
        	
        	ResponseHandler<String> handler = new BasicResponseHandler();
			String body = handler.handleResponse(response);
			response.close();
			
			Object obj = parser.parse(body);
			JSONObject jsonObj = (JSONObject) obj;
			JSONArray statuses = (JSONArray) jsonObj.get(ARRAYTWS);
			Iterator<JSONObject> iterator = statuses.iterator();
			while (iterator.hasNext()) {
				JSONObject status = (JSONObject) iterator.next();
				newMetrics.put("retweets", Integer.parseInt((String) status.get(TWRTCOUNTER)));
				newMetrics.put("favorites", Integer.parseInt((String) status.get(TWFVCOUNTER)));
				JSONObject authorObj = (JSONObject) status.get(TWAUTHOR);
				String author = (String) authorObj.get(TWAUTHORHANDLE);
				String statusURL = TWITTERURL+author+"/status/"+status.get(TWID);
				int replies = twRepReader.getReplies(statusURL);
				newMetrics.put("replies", replies);
			}
        }
        else {
        	throw new IOException("Wrong Twitter API response code, got: " + 
        						  response.getStatusLine().getStatusCode() + 
        						  " expected 200");
        }
		
		return newMetrics;
	}
	
	private void pause(Timer timer, HttpGet httpGet) 
	throws InterruptedException, ClientProtocolException, IOException {
		CloseableHttpResponse response;
		
		long waitingTime = TIME_WINDOW-timer.getElapsedTime();
		Util.printMessage("API request limit reached, we have to wait " + 
	    		  		  waitingTime + " minutes for the next time window", 
	    		  		  "info", logger);
		Thread.sleep((TIME_WINDOW-timer.getElapsedTime())*60000);
		response = client.execute(httpGet);
		setRemainingRequests(response);
		while (remainingRequests == 0) {
			Thread.sleep(1000);
			response = client.execute(httpGet);
			setRemainingRequests(response);
		}
	}
	
	private void setRemainingRequests(CloseableHttpResponse response) 
	throws ClientProtocolException, IOException 
	{	
        Header[] headers = response.getAllHeaders();
        for(Header header:headers) {
        	if (header.getName().equalsIgnoreCase("x-rate-limit-remaining"))
        		remainingRequests = Integer.parseInt(header.getValue());
    	}
	}
}
