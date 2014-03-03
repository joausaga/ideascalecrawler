package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class Importer {
	private static final String DIRCSV = "reports/";
	private DBManager db = null;
	
	public Importer() {
		db = new DBManager();
	}
	
	public void importCSV() 
	throws IOException 
	{
		String pathCSV = DIRCSV + "ideascale_communities.csv";
		BufferedReader buffReader = new BufferedReader(
									new FileReader(pathCSV));
		String currentLine = buffReader.readLine(); //Discarding the first line since it contains the titles
		String[] fields = null;
		HashMap<String,Object> community = new HashMap<String,Object>();
		while ((currentLine = buffReader.readLine()) != null) {
			fields = currentLine.split(",");
			community.put("name", fields[0]);
			community.put("url", fields[12]);
			community.put("type", fields[1]);
			community.put("orientation", fields[2]);
			community.put("ideas", fields[3]);
			community.put("ideas_implemented", fields[4]);
			community.put("members", fields[5]);
			community.put("votes", fields[6]);
			community.put("comments", fields[7]);
			community.put("owner", fields[8]);
			community.put("purpose", fields[9]);
			community.put("language", fields[11]);
			if (!currentLine.contains("closed") && !currentLine.contains("inactive"))
				community.put("status", "open");
			else
				community.put("status", fields[10]);
			if (currentLine.contains("outlier"))
				community.put("outlier", true);
			else
				community.put("outlier", false);
			try {
				db.insertCommunityFromCSV(community);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		buffReader.close();
		db.close();
	}
	
}
