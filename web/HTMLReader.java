package web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import src.Util;

import com.sun.tools.javac.util.Context;

public class HTMLReader {
	private final static Logger logger = Logger.getLogger(HTMLReader.class.getName());
	
	/**
     * {@link StatusLine} HTTP status code when no server error has occurred.
     */
    private static final int HTTP_STATUS_OK = 200;
    
    private static final int HTTP_STATUS_NO_SERVICE = 503;
    
    private static final int HTTP_STATUS_NOT_FOUND = 404;
	
    /**
     * Shared buffer used by {@link #getUrlContent(String)} when reading results
     * from an API request.
     */
    private static byte[] sBuffer = new byte[512];
    
    /**
     * User-agent string to use when making requests. Should be filled using
     * {@link #prepareUserAgent(Context)} before making any other calls.
     */
    private static String sUserAgent = null;
    
    /**
     * Prepare the internal User-Agent string for use. This requires a
     * {@link Context} to pull the package name and version number for this
     * application.
     */
    public void prepareUserAgent() {
        sUserAgent = String.format("Mozilla/5.0 (Macintosh; " +
        						   "Intel Mac OS X 10_7_5) " +
        						   "AppleWebKit/537.36 (KHTML, like Gecko) " +
        						   "Chrome/29.0.1547.76 Safari/537.36");
    }
    
    /**
     * Pull the raw text content of the given URL. This call blocks until the
     * operation has completed, and is synchronized because it uses a shared
     * buffer {@link #sBuffer}.
     * 
     * @param url The exact URL to request.
     * @return The raw content returned by the server.
     * @throws Exception 
     * @throws ApiException If any connection or server error occurs.
     */
    public synchronized String getUrlContent(URI url) 
    throws Exception 
    {
        if (sUserAgent == null)
        	throw new Exception("User-Agent string must be prepared");
        
        // Create client and set our specific user-agent string
        /*RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.NETSCAPE)
				.build();*/
        
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", sUserAgent);

        //request.setConfig(globalConfig);

        try {
        	
            HttpResponse response = doRequest(request);
            
            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            while (status.getStatusCode() != HTTP_STATUS_OK &&
                   status.getStatusCode() != HTTP_STATUS_NOT_FOUND) {
            	Util.printMessage("Invalid response from server: " +
		     	 			  	  status.toString() + 
		     	 			  	  ". Trying again", "info", logger);
            	Thread.sleep(10000); //Wait for 10 seconds and try again
            	response = doRequest(request);
            	status = response.getStatusLine();
            }
            
            // Pull content stream from response
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            
            // Read response into a buffered stream
            int readBytes = 0;
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            
            // Return result from buffered stream
            return new String(content.toByteArray(),"utf-8");
        } catch (IOException e) {
            throw new Exception("Communication problems", e);
        }
    }
	
    public synchronized String getUrlContent(String url) 
	throws Exception 
	{
		if (sUserAgent == null)
			throw new Exception("User-Agent string must be prepared");
		
		// Create client and set our specific user-agent string
		/*RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.NETSCAPE)
				.build();*/
		
		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", sUserAgent);
		
		//request.setConfig(globalConfig);
		
		try {
			
		    HttpResponse response = doRequest(request);
		    
		    // Check if server response is valid
			StatusLine status = response.getStatusLine();
			while (status.getStatusCode() != HTTP_STATUS_OK &&
                   status.getStatusCode() != HTTP_STATUS_NOT_FOUND) {
				Util.printMessage("Invalid response from server: " +
       		     	 			  status.toString() + 
       		     	 			  ". Trying again", "info", logger);
				Thread.sleep(10000); //Wait for 10 seconds and try again
				response = doRequest(request);
				status = response.getStatusLine();
			}
		
			// Pull content stream from response
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			
			// Read response into a buffered stream
			int readBytes = 0;
			while ((readBytes = inputStream.read(sBuffer)) != -1) {
			    content.write(sBuffer, 0, readBytes);
			}
		
			// Return result from buffered stream
            return new String(content.toByteArray());
		} catch (IOException e) {
		    throw new Exception("Communication problems", e);
		}
	}
    
    private HttpResponse doRequest(HttpGet request) 
    throws ClientProtocolException, IOException {
    	HttpClient client = HttpClients.createDefault();
    	HttpResponse response = client.execute(request);
    	return response;
    }
    
    
    public boolean checkHTTPSecureProtocol(String url) {
    	HttpClient client = HttpClients.createDefault();
    	RequestConfig requestConfig = RequestConfig.custom().
    								  setRedirectsEnabled(false).build();
    	HttpGet request = new HttpGet(url);
    	request.setConfig(requestConfig);
    	HttpResponse response;
		try {
			response = client.execute(request);
	    	// expect a 302 response.
	    	if (response.getStatusLine().getStatusCode() == 302)
	    		return true;
	    	return false;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
	    	return false;
		} catch (IOException e) {
			e.printStackTrace();
	    	return false;
		}
    }
}
