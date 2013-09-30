package src;

import java.util.HashMap;
import java.util.List;

public class Crawler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CommunityInfoReader commInfoReader = new CommunityInfoReader();
		Reporter reporter = new Reporter();
		try {
			System.out.println("Collecting information about Ideascale communities, please wait...");
			List<HashMap<String,String>> communities = commInfoReader.getCommunitiesInfo();
			System.out.println("Writing the report, please wait...");
			reporter.createReport(communities);
			System.out.println("Done!, take a look at the report at " + reporter.getPathReport());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
