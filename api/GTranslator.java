package api;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import src.Util;

public class GTranslator {
	private final static Logger logger = Logger.getLogger(TweetSearch.class.getName());
	private String APIKEY = "";
	private static String REQUESTURL = "https://www.googleapis.com/language/translate/v2";
	protected JSONParser parser = null;
	
	public GTranslator() {
		parser = new JSONParser();
		setUp();
	}
	
	private void setUp() {
		try {
			BufferedReader buffReader = new BufferedReader(new FileReader("conf"));
			String currentLine = buffReader.readLine(); //Discarding the first line since it contains indications
			while ((currentLine = buffReader.readLine()) != null) {
				if (currentLine.indexOf("GTAPIKEY") != -1) {
					APIKEY = currentLine.split("=")[1];
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String translateText(String text, String sourceLang, String targetLang) 
	{
		String translatedText = "";
		
		try {
			text = URLEncoder.encode(text,"utf-8");
			
			if (!sourceLang.isEmpty()) {
				String fullURL = REQUESTURL+"?key="+APIKEY+"&q="+text+"&source="+
								 sourceLang+"&target="+targetLang;
				HttpGet httpGet = new HttpGet(fullURL);
				HttpClient client = HttpClients.createDefault();
				HttpResponse response = client.execute(httpGet);
			
				if (response.getStatusLine().getStatusCode() == 200) {
					ResponseHandler<String> handler = new BasicResponseHandler();
					String body = handler.handleResponse(response);
		            
		            Object obj = parser.parse(body);
		            JSONObject jsonObj = (JSONObject) obj;
		            
		            JSONObject translationsData = (JSONObject) jsonObj.get("data");
		            JSONArray translationArray = (JSONArray) translationsData.get("translations");
		            
		            //Get the first language detected
		            JSONObject translation = (JSONObject) translationArray.get(0);
		            translatedText = (String) translation.get("translatedText");
				}
				else {
					Util.printMessage("Something wrong while translating the text: " + 
									  text,"severe",logger);
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Util.printMessage("Something wrong while translating the text: " + 
					  		   text,"severe",logger);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			Util.printMessage("Something wrong while translating the text: " + 
			  		   		   text,"severe",logger);
		} catch (IOException e) {
			e.printStackTrace();
			Util.printMessage("Something wrong while translating the text: " + 
			  		   		   text,"severe",logger);
		} catch (ParseException e) {
			e.printStackTrace();
			Util.printMessage("Something wrong while translating the text: " + 
			  		   		   text,"severe",logger);
		}
		
		return translatedText;
	}
	
	public String detectSourceLanguage(String text) 
	throws ClientProtocolException, IOException, ParseException 
	{
		text = URLEncoder.encode(text,"utf-8");
		String sourceLang = "";
		
		String fullURL = REQUESTURL+"/detect?key="+APIKEY+"&q="+text;
		
		HttpGet httpGet = new HttpGet(fullURL);
		HttpClient client = HttpClients.createDefault();
		HttpResponse response = client.execute(httpGet);
	
		if (response.getStatusLine().getStatusCode() == 200) {
			ResponseHandler<String> handler = new BasicResponseHandler();
			String body = handler.handleResponse(response);
            
            Object obj = parser.parse(body);
            JSONObject jsonObj = (JSONObject) obj;
            
            JSONObject responseData = (JSONObject) jsonObj.get("data");
            
            JSONArray languagesDet = (JSONArray) responseData.get("detections");
            
            //Get the first language detected
            JSONArray languageDetArray = (JSONArray) languagesDet.get(0);
            JSONObject languageDet = (JSONObject) languageDetArray.get(0); 
            sourceLang = (String) languageDet.get("language");
		}
		else {
			Util.printMessage("Something wrong while detecting the language of the text: " + 
							  text,"severe",logger);
		}
		
		return sourceLang;
	}
}
