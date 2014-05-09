package api;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	private String ACCESSTOKEN = "";
	private String ACCESSSECRET = "";
	private String APIKEY = "";
	private String APISECRET= "";
	protected OAuthConsumer consumer = null;
	protected Integer remainingRequests;
	protected Long limitReset;
	protected JSONParser parser = null;
	protected static Integer API_LIMIT = 180;
	
	
	public TwitterApp() {
		setUp();
		consumer = new CommonsHttpOAuthConsumer(APIKEY,APISECRET);
		consumer.setTokenWithSecret(ACCESSTOKEN, ACCESSSECRET);
		remainingRequests = API_LIMIT;
		parser = new JSONParser();
	}
	
	private void setUp() {
		try {
			BufferedReader buffReader = new BufferedReader(new FileReader("conf"));
			String currentLine = buffReader.readLine(); //Discarding the first line since it contains indications
			while ((currentLine = buffReader.readLine()) != null) {
				if (currentLine.indexOf("TWACCESSTOKEN") != -1) {
					ACCESSTOKEN = currentLine.split("=")[1];
				} else if (currentLine.indexOf("TWACCESSSECRET") != -1) {
					ACCESSSECRET = currentLine.split("=")[1];
				} else if(currentLine.indexOf("TWAPIKEY") != -1) {
					APIKEY = currentLine.split("=")[1];
				} else if(currentLine.indexOf("TWAPISECRET") != -1) {
					APISECRET = currentLine.split("=")[1];
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
