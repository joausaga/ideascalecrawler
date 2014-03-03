package api;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import src.DBManager;

public class CommunityParticipation {
	private static final String API_TOKEN = "5b3326f8-50a5-419d-8f02-eef6a42fd61a";
	private DBManager dbManager = null;
	private CloseableHttpClient httpClient = null;
	private JSONParser parser = null;
	
	public CommunityParticipation() {
		dbManager = new DBManager();
		httpClient = HttpClients.createDefault();
		parser = new JSONParser();
	}
	
	public ArrayList<String> getCampaigns(String communityURL) 
	throws ClientProtocolException, IOException, IllegalStateException, ParseException 
	{
		ArrayList<String> campaigns = new ArrayList<String>();
		
		HttpGet httpGet = new HttpGet(communityURL+"/a/rest/v1/campaigns");
		httpGet.setHeader("api_token", API_TOKEN);
		CloseableHttpResponse response = httpClient.execute(httpGet);
		if (response.getStatusLine().getStatusCode() == 200) {
			ResponseHandler<String> handler = new BasicResponseHandler();
			String body = handler.handleResponse(response);
			Object obj = parser.parse(body);
			System.out.println("URL: " + communityURL + " Campaings:" + body);
			response.close();
		}
		else {
			throw new IOException("Wrong API response code, got: " + response.getStatusLine().getStatusCode() + " expected 200");
		}
		
		return campaigns;
	}
	
	public void calculateParticipation() {
		try {
			ArrayList<HashMap<String,String>> communities = dbManager.getCommunitiesURL(null);
			getCampaigns(communities.get(0).get("url"));
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
