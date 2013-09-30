package src;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class HTMLReader {

	/**
     * {@link StatusLine} HTTP status code when no server error has occurred.
     */
    private static final int HTTP_STATUS_OK = 200;
	
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
    protected synchronized String getUrlContent(String url) 
    throws Exception 
    {
        if (sUserAgent == null)
        	throw new Exception("User-Agent string must be prepared");
        
        // Create client and set our specific user-agent string
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", sUserAgent);

        try {
        	
            HttpResponse response = client.execute(request);
            
            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HTTP_STATUS_OK) {
                throw new Exception("Invalid response from server: " +
                        		     status.toString());
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
	
}
