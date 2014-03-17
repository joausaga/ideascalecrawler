package api;

import java.io.IOException;
import java.util.logging.Logger;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.parser.JSONParser;

import src.Util;

public class TwitterApp {
	private final static Logger logger = Logger.getLogger(TwitterApp.class.getName());
	private static String ACCESSTOKEN = "156641445-h2WruI5Ha5Z6gyBWeyRrH1zp9VwBKJdPz5sCVzVK";
	private static String ACCESSSECRET = "iH5VgiHOLEo6JYyZPPJZ4X94yFZDb2N9r7h8MN7R4PRVU";
	private static String APIKEY = "qL2ne3j89Y6wbFbGq1FaA";
	private static String APISECRET= "7ERg6sHqf7AaWAv4te8ZsDcQusx7Q27Nw93ZyU6k";
	protected OAuthConsumer consumer = null;
	protected Integer remainingRequests;
	protected Long limitReset;
	protected JSONParser parser = null;
	protected static Integer API_LIMIT = 180;
	
	
	public TwitterApp() {
		consumer = new CommonsHttpOAuthConsumer(APIKEY,APISECRET);
		consumer.setTokenWithSecret(ACCESSTOKEN, ACCESSSECRET);
		remainingRequests = API_LIMIT;
		parser = new JSONParser();
	}
	
	protected HttpResponse pause(HttpGet httpGet) 
	throws InterruptedException, ClientProtocolException, IOException {
		HttpResponse response = null;
		
		long now = System.currentTimeMillis();
		long waitingTime = limitReset-now;
		Util.printMessage("API request limit reached, we have to wait " + 
			    		  (waitingTime/60000) + " minutes for the next time window", 
			    		  "info", logger);
		if (waitingTime > 0) {
			Thread.sleep(waitingTime);
			response = doRequest(httpGet);
			while (remainingRequests == 0) {
				System.out.println("Still banned.");
				Thread.sleep(60000);
				response = doRequest(httpGet);
			}
		}
		else {
			response = doRequest(httpGet);
		}
		
		return response;
	}
	
	protected HttpResponse doRequest(HttpGet httpGet) 
	throws ClientProtocolException, IOException {
		HttpClient client = HttpClients.createDefault();
		HttpResponse response = client.execute(httpGet);
		setRemainingRequests(response);
		return response;
	}
	
	protected void setRemainingRequests(HttpResponse response) 
	throws ClientProtocolException, IOException 
	{	
        Header[] headers = response.getAllHeaders();
        for(Header header:headers) {
        	if (header.getName().equalsIgnoreCase("x-rate-limit-remaining"))
        		remainingRequests = Integer.parseInt(header.getValue());
        	if (header.getName().equalsIgnoreCase("x-rate-limit-reset")) {
        		long timeLimitMS = Long.parseLong(header.getValue())*1000;
        		limitReset = timeLimitMS;
        	}
    	}
	}
}
