package web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

public class Reporter {
	private static final String dirReport = "reports/";
	private String pathReport;
	
	public Reporter() {
	}
	
	public String getPathReport() {
		return pathReport;
	}
	
	public void createReport(List<HashMap<String,String>> infoCommunities) {
		pathReport = dirReport + "report.csv";
		File file = new File(pathReport);
		try {
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			/* Write the header of the report */
			bw.write("\"Community\",\"Members\",\"Ideas\",\"Votes\",\"Comments\",\"URL\"\n");
			
			/* Write the corpus of the report */
			for (HashMap<String,String> community : infoCommunities) {
				bw.write("\""+community.get("name")+"\",\""
							 +community.get("members")+"\",\""
							 +community.get("ideas")+"\",\""
							 +community.get("votes")+"\",\""
							 +community.get("comments")+"\",\""
							 +community.get("url")+"\"\n");
			}
			bw.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
