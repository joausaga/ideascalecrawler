package src;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import web.CommunityInfoReader;
import api.GTranslator;
import api.TweetSearch;
import api.TweetUpdater;

public class Crawler {
	private static final Logger logger = Logger.getLogger(Crawler.class .getName()); 
	private static CommunityInfoReader commInfoReader = null;
	private static DBManager db = null;
	private static TweetSearch ts = null;
	private static TweetUpdater tu = null;
	private static ArrayList<String> directory = null;
	private static Scanner user_input = null;
	private final static String IDEASCALE_BASE_URL = "https://ideascale.com/index/";
	private static final String EXECUTIONFILE = "running.lck";
	private static final String ERRORFILE = "error.lck";
	private static GTranslator translator = null;
	
	public static void init() {
		commInfoReader = new CommunityInfoReader();
		db = new DBManager();
		ts = new TweetSearch();
		tu = new TweetUpdater();
		directory = new ArrayList<String>(Arrays.asList("a","b","c",
				  	"d","e","f","g","h","i","j","k","l","m","n","o",
				  	"p","q","r","s","t","u","v","w","x","y","z"));
		user_input = new Scanner(System.in);
		translator = new GTranslator();
		//logger.setLevel(Level.SEVERE);
		try {
			CrawlLogger.setup();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Problems creating the log files");
		}
	}
	
	public static void exit() {
		db.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		init();
		if (args.length > 0) {
			String type = args[0];
			if (type.equalsIgnoreCase("fg")) {
				Util.printMessage("Crawler started in foreground mode.","info",logger);
				startFg();
			}
			else if (type.equalsIgnoreCase("bg")) {
				Util.printMessage("Crawler started in background mode.","info",logger);
				startBg(args);
			}
			else {
				Util.printMessage("Unkown argument " + type,"severe",logger);
				exit();
				System.exit(1);
			}
		}
		else {
			System.out.println("Usage TYPE [OPTION] [LETTERS] [SYNCMODE]");
			System.out.println("TYPE: 'fg' or 'bg'");
			System.out.println("If 'bg':");
			System.out.println("- OPTION: 1, 2, 3, 4, 5, or 6");
			System.out.println("- 1: For synchronizing the entire IdeaScale community directory");
			System.out.println("- 2: For synchronizing only a sub-set of communities");
			System.out.println("- 3: For synchronizing ideas, votes, comments and social sharing counters of active communities");
			System.out.println("- 4: For updating recorded tweet's metrics");
			System.out.println("- 5: For synchronizing ideas, votes, comments and social sharing counters of active communities, and updating recorded tweet's metrics");
            System.out.println("- 6: For synchronizing ideas, votes, comments and social sharing counters of all civic-participation communities even if they are inactive or close");
			System.out.println("If 'OPTION' = 2:");
			System.out.println("- LETTERS: ");
			System.out.println("- 'a,b,c' specify a group of communities directory letters by using comma-separeted values");
			System.out.println("- 'a-d' specify a range of community directory letters by using dash");
			System.out.println("- 'a' specify a single community directory letter");
			System.out.println("If 'OPTION' = 3:");
			System.out.println("- SYNCMODE: ");
			System.out.println("- 1: Synchronization of the entire group of active communities");
			System.out.println("- 2: Continue with a previous stopped syncronization process");
		}
	}
	
	public static void startFg() {
		System.out.println("Please choose one of the following numbers:");
		System.out.println("1 - for synchronizing the entire IdeaScale community directory");
		System.out.println("2 - for synchronizing only a sub-set of communities");
		System.out.println("3 - for synchronizing active communities");
		System.out.println("4 - for updating recorded tweet's metrics");
		System.out.println("5 - for synchronizing the ideas, votes, comments and social sharing counters of all active communities, and updating recorded tweet's metrics");
        System.out.println("6 - for synchronizing ideas, votes, comments and social sharing counters of all civic-participation communities even if they are inactive or close");
		
		String option = user_input.next();
		
		if (option.equals("1")) {
			syncCommunityCat(directory);
			exit();
		}
		else if (option.equals("2")) {
			System.out.println("Introduce the directory letters you want to " +
							   "synchronize, you can do it by specifying individual " +
							   "letters separated by commas or by entering a " +
							   "range of letters");
			String letters = user_input.next();
			ArrayList<String> lettersSync = getSetLettersSync(letters);
			syncCommunityCat(lettersSync);
			exit();
		}
		else if (option.equals("3")) {
			System.out.println("Please select one of the synchronization modes:");
			System.out.println("1 - for synchronizing the entire group of active communities");
			System.out.println("2 - for continuing with a previous stopped synchronization process");
			String opSync = user_input.next();
			if (opSync.equals("1") || opSync.equals("2")) {
				syncActiveCommunitiesInfo(Integer.parseInt(option),opSync);
				exit();
			}
			else {
				Util.printMessage("Unknown option " + option + ".","severe",logger);
				exit();
				System.exit(1);
			}
		}
		else if(option.equals("4")) {
			updateTweetsMetrics();
			exit();
		}
		else if(option.equals("5")) {
			syncActiveCommunitiesInfo(Integer.parseInt(option),"1");
			updateTweetsMetrics();
			exit();
		}
        else if(option.equals("6")) {
        	downloadInfoCPCommunities();
			exit();
		}
		else {
			Util.printMessage("Unknown option " + option + ".","severe",logger);
			exit();
			System.exit(1);
		}
	}
	
	private static void startBg(String[]  args) {
		if (args.length > 1) {
			Integer op = Integer.parseInt(args[1]);
			if (op == 1) {
				createExecutionFile();
				syncCommunityCat(directory);
				removeFile(EXECUTIONFILE);
				exit();
			}
			else if (op == 3) {
				if (args.length == 3) {
					createExecutionFile();
					syncActiveCommunitiesInfo(op, args[2]);
					removeFile(EXECUTIONFILE);
					exit();
				}
				else {
					Util.printMessage("Invalid synchronization mode","severe",logger);
					exit();
					System.exit(1);
				}
			}
			else if (op == 2) {
				if (args.length == 3) {
					String letters = args[2];
					ArrayList<String> lettersSync = getSetLettersSync(letters);
					createExecutionFile();
					syncCommunityCat(lettersSync);
					removeFile(EXECUTIONFILE);
					exit();
				}
				else {
					Util.printMessage("Invalid community directory letters","severe",logger);
					exit();
					System.exit(1);
				}
			}
			else if (op == 4) {
				createExecutionFile();
				updateTweetsMetrics();
				removeFile(EXECUTIONFILE);
				exit();
			}
			else if(op == 5) {
				createExecutionFile();
				syncActiveCommunitiesInfo(op, "1");
				updateTweetsMetrics();
				removeFile(EXECUTIONFILE);
				exit();
			}
            else if(op == 6) {
            	downloadInfoCPCommunities();
				exit();
			}
            else if(op==7) {
				System.out.println(test("Il y a 2 ans"));
			}
			else {
				Util.printMessage("Unknown background option: " + op,"severe",logger);
				exit();
				System.exit(1);
			}
		}
		else {
			Util.printMessage("Invalid number of arguments for background mode","severe",logger);
			exit();
			System.exit(1);
		}
	}
	
	private static boolean englishDate(String date) {
    	if (date.indexOf("hours") != -1 || date.indexOf("hour") != -1) {
    		return true;
    	} else if (date.indexOf("days") != -1 || date.indexOf("day") != -1) {
    		return true;
    	} else if (date.indexOf("months") != -1 || date.indexOf("month") != -1) {
    		return true;
    	} else if (date.indexOf("years") != -1 || date.indexOf("year") != -1) {
    		return true;
    	}
    	return false;
    }
	
	private static String test(String vagueDate) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	Calendar cal = Calendar.getInstance();
        cal.setTime(cal.getTime());
    	int i = 0;
    	
    	//Find the number
    	while (!Character.isDigit(vagueDate.charAt(i))) i++;
    	
    	//Remove all non-numeric characters
    	vagueDate = vagueDate.substring(i);
    	
    	int num = Integer.parseInt(vagueDate.replaceAll("[^0-9]+", " ").trim());
    	
    	String translatedText = "";
    	if (!englishDate(vagueDate))
    		translatedText = translator.translateText(vagueDate, "en");
    	else
    		translatedText = vagueDate;
    	
    	if (translatedText.indexOf("hours") != -1 || translatedText.indexOf("hour") != -1) {
    		cal.add(Calendar.HOUR_OF_DAY, -num);
    	} else if (translatedText.indexOf("days") != -1 || translatedText.indexOf("day") != -1) {
    		cal.add(Calendar.DAY_OF_YEAR, -num);
    	} else if (translatedText.indexOf("months") != -1 || translatedText.indexOf("month") != -1) {
    		cal.add(Calendar.MONTH, -num);
    	} else if (translatedText.indexOf("years") != -1 || translatedText.indexOf("year") != -1) {
    		cal.add(Calendar.YEAR, -num);
    	}
    	
    	return dateFormat.format(cal.getTime());
	}
	
	private static void createExecutionFile() {
        //Remove the execution file
        removeFile(EXECUTIONFILE);
        
		File file = new File(EXECUTIONFILE);
		 
		try {
			if (!file.createNewFile()) {
				Util.printMessage("Cannot create the execution file","severe",logger);
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void removeFile(String fileName) {
		File file = new File(fileName);
		 
		if(file.exists()) {
			if (!file.delete()) {
				Util.printMessage("Cannot delete the file " + fileName,"severe",logger);
			}
		}
	}
	
	private static void createErrorFile() {
        //Remove previous existing error files
        removeFile(ERRORFILE);
        
		File file = new File(ERRORFILE);
		 
		try {
			if (!file.createNewFile()) {
				Util.printMessage("Cannot create the error file","severe",logger);
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static ArrayList<String> getSetLettersSync(String letters) {
		ArrayList<String> lettersSync = new ArrayList<String>();
		
		letters = letters.replaceAll("\\s","");
		if (letters.length() > 1) {
			if (letters.contains(",")) {
				for (String letter : letters.split(",")) {
					letter = letter.trim();
					if (directory.contains(letter)) {
						lettersSync.add(letter);
					}
					else {
						Util.printMessage("Unknown letter: " + letter + ".","severe",logger);
						System.exit(1);
					}
				}
			}
			else if (letters.contains("-")) {
				int startR, endR;
				String[] auxLetters = letters.split("-");
				startR = directory.indexOf(auxLetters[0]);
				if (startR == -1) {
					Util.printMessage("Unknown letter: " + auxLetters[0] + ".","severe",logger);
					System.exit(1);
				}
				endR = directory.indexOf(auxLetters[1]);
				if (endR == -1) {
					Util.printMessage("Unknown letter: " + auxLetters[1] + ".","severe",logger);
					System.exit(0);
				}
				for (int i = startR; i <= endR; i++) {
					lettersSync.add(directory.get(i));
				}
			}
			else {
				Util.printMessage("Unknown symbol.","severe",logger);
				System.exit(1);
			}
		}
		else if (letters.length() == 1) {
			if (directory.contains(letters)) {
				lettersSync.add(letters.trim());
			}
			else {
				Util.printMessage("Unknown letter: " + letters + ".","severe",logger);
				System.exit(1);
			}
		}
		else {
			Util.printMessage("Wrong input.","severe",logger);
			System.exit(1);
		}
		
		return lettersSync;
	}
	
	private static ArrayList<String> syncCommunityCat(ArrayList<String> letters) 
	{
		String urlDir = IDEASCALE_BASE_URL;
		ArrayList<String> newCommunities = new ArrayList<String>();
		long startingTime = System.currentTimeMillis();
		HashMap<String,String> duration = null;
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		
		for (String letter : letters) {
			System.out.println("");
			String content;
			String idCommunity;
			try {
				Util.printMessage("Resetting update flags of communities whose " +
						   	 "names start with " + letter.toUpperCase() + "...",
						   	 "info",logger);
				db.resetUpdateFlag(letter);
				content = commInfoReader.getUrlContent(Util.toURI(urlDir+letter+".html"));
				Document doc = Jsoup.parse(content);	        
		        Elements liElements = doc.getElementsByTag("li");
		        Util.printMessage("Starting syncronization of communities whose " +
					   	 	 "names start with: " + letter.toUpperCase() + "...",
					   	 	 "info",logger);
		        for (Element li : liElements) {
		        	if (li.children().size() > 0) {
		        		String link = li.child(0).attr("href");
		        		String name = li.text();
		        		if (link.contains("http://") && 
		        			!commInfoReader.inBlackList(link)) 
		        		{ 
		        			if (commInfoReader.checkHTTPSecureProtocol(link)) {
		        				int colon = link.indexOf(":");
		        				//Convert http to https
		        				link = link.substring(0, colon) + "s" + 
		        					   link.substring(colon);
		        			}
		        			
		        			//link = "http://www.wtfcolumbia.com";
		        			//name = "WTF Columbia";
		        			
		    				HashMap<String,Object> communityInfo = new HashMap<String,Object>();
		    				HashMap<String,Object> communityStats = commInfoReader.readCommunityStats(link, name);
		    				if (communityStats.get("ideas") != null) {   //Checking if it isn't a wrong page
			    				HashMap<String,Object> communityDates = commInfoReader.getCommunityLifeSpan(link, 
			    														(ArrayList<HashMap<String,String>>) communityStats.get("tabs"));
			    				communityInfo.put("name", name);
			    				communityInfo.put("url", link);
			    				communityInfo.put("ideas", communityStats.get("ideas"));
			    				communityInfo.put("ideas_in_review", communityStats.get("ideas_in_review"));
			    				communityInfo.put("ideas_in_progress", communityStats.get("ideas_in_progress"));
			    				communityInfo.put("ideas_completed", communityStats.get("ideas_completed"));
			    				communityInfo.put("members", communityStats.get("members"));
			    				communityInfo.put("votes", communityStats.get("votes"));
			    				communityInfo.put("comments", communityStats.get("comments"));
			    				communityInfo.put("facebook", communityStats.get("facebook"));
			    				communityInfo.put("twitter", communityStats.get("twitter"));
			    				communityInfo.put("logo", communityStats.get("logo"));
			    				communityInfo.put("explanation_text", communityStats.get("explanation_text"));
			    				communityInfo.put("dateLastIdea", communityDates.get("dateLastIdea"));
			    				communityInfo.put("dateFirstIdea", communityDates.get("dateFirstIdea"));
			    				communityInfo.put("lifespan", communityDates.get("lifespan"));
			    				communityInfo.put("status", communityDates.get("status"));
			    				Util.printMessage(communityInfo.toString(),"info",logger);
			    				//Check whether the community already exists in the DB
			    				idCommunity = db.alreadyOnDB(link);    
			        			//Update its information
			        			if (idCommunity != null) {
			        				db.updateCommunityInfo(idCommunity,communityInfo);
			        			}
			        			//Save it to DB
			        			else  {
			        				newCommunities.add(name);
			        				db.insertCommunityFromHash(communityInfo);
			        			}
		    				}
		        			
		    				//Wait for a moment to avoid being banned
		        			double rand = Math.random() * 5;		        			
		        			Thread.sleep((long) (rand * 1000)); 
		        			
		        			System.out.println("");
		        		}
		        	}
		        }	        
		        duration = calcOpDuration(startingTime);
		        
		        //Remove unexisting communities
		        int removedCommunities = db.removeUnexistingCommunities(letter);
		        Util.printMessage("Removed " + letter +"'s communities: " + removedCommunities,"info",logger);		        		      
		        
				//Register operation in Audit table
				Calendar cal = Calendar.getInstance();
				Date opDuration = timeFormat.parse(duration.get("hours") + ":" + 
												   duration.get("minutes")  + ":" + 
												   duration.get("seconds"));
				db.registerOperation(cal.getTime(), "Syncronization of communities: " + letter, opDuration,-1);
				
				Util.printMessage("Syncronization of communities "+ letter +
							 	  " has finished with a duration of: " + 
							 	  duration.get("hours") + ":" + 
							 	  duration.get("minutes")  + ":" + 
				  		   	  	  duration.get("seconds"),"info",logger);
				
				Util.printMessage("Found " + newCommunities.size() + " new communities.","info",logger);
				for (int i = 0; i < newCommunities.size(); i++)
					Util.printMessage("- " + newCommunities.get(i),"info",logger);
			}
			catch(Exception e) {
				e.printStackTrace();
				logger.log(Level.SEVERE,e.getMessage(),e);
				createErrorFile();
			}
		}
		
        return newCommunities;		
	}
	
	/*
	 * Synchronize the twitter counters and ideas of active communities
	 * */
	private static void syncActiveCommunitiesInfo(Integer op, String opSync) {
		long startingTime = 0;
		ArrayList<HashMap<String, String>> activeCommunities;
		Calendar cal = Calendar.getInstance();
		Date today = cal.getTime();
		Integer observation;
		
		try {
			if (opSync.equals("1")) {
				db.resetSyncFlag();
				db.cleanSyncProgressTable();
				observation = db.getLastObservationId() + 1;
			}
			else {
				observation = db.getLastObservationId();
				//Before starting check whether exists unfinished processes and finishing them
				HashMap<String,Object> unfinishedProcess = db.getUnfinishedSyncProcess();
				if (!unfinishedProcess.isEmpty()) {
					startingTime = System.currentTimeMillis();
					//1: Resume the unfinished process
					resumeUnfinishedProcess(unfinishedProcess, today);
					HashMap<String,String> unfinishedCommunity = new HashMap<String,String>();
					unfinishedCommunity.put("id", unfinishedProcess.get("community_id").toString());
					unfinishedCommunity.put("url", unfinishedProcess.get("community_url").toString());
					unfinishedCommunity.put("tab", unfinishedProcess.get("current_tab").toString());
					//2: Save Tweets of unfinished community
					saveCommunityTweets(unfinishedCommunity);
					//3: Save Tweets of unfinished community ideas
					saveCommunityIdeasTweets(unfinishedCommunity,observation);
					//4: Finishing synchronization of the unfinished process
					finishCommunitySync(startingTime, unfinishedCommunity.get("url"),
									    unfinishedCommunity.get("id"), today,
									    observation);
					//5: Pause for a moment to avoid being banned
					pause();
				}
			}
			activeCommunities = db.getActiveCommunities();
			if (activeCommunities.size() == 0) {
				Util.printMessage("All communities are already synchronized.","info",logger);
				return;
			}
			System.out.println("");
			Util.printMessage("Starting the synchronization of active communities...","info",logger);
			for (HashMap<String,String> activeCommunity : activeCommunities) {
				System.out.println("");
				Util.printMessage("Synchronizing community: " + activeCommunity.get("name"),"info",logger);
				startingTime = System.currentTimeMillis();
				String url = activeCommunity.get("url");
				if (commInfoReader.checkHTTPSecureProtocol(url)) {
    				int colon = url.indexOf(":");
    				//Convert http to https
    				url = url.substring(0, colon) + "s" + 
    					  url.substring(colon);
    			}				
				//1: Sync Community Ideas and Social Network Statistics
				boolean processed = syncCommunitySnStatsAndIdeas(activeCommunity,observation);
				if (processed) {
					//2: Save Community Tweets
					saveCommunityTweets(activeCommunity);
					//3: Save Community Ideas Tweets
					saveCommunityIdeasTweets(activeCommunity,observation);
                }
				//4: Finishing synchronization
				finishCommunitySync(startingTime,activeCommunity.get("name"),
								    activeCommunity.get("id"), today, observation);
				//5: Pause for a moment to avoid being banned
				pause();
			}		
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
			createErrorFile();
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
			createErrorFile();
		}
	}
	
    private static void downloadInfoCPCommunities() {
		long startingTime = 0;
		ArrayList<HashMap<String, String>> cpCommunities;
		Calendar cal = Calendar.getInstance();
		Date today = cal.getTime();
		
		try {
			//Before starting check whether exists unfinished processes and finishing them
			HashMap<String,Object> unfinishedProcess = db.getUnfinishedSyncProcess();
			if (!unfinishedProcess.isEmpty()) {
				startingTime = System.currentTimeMillis();
				//1: Resume the unfinished process
				resumeUnfinishedProcess(unfinishedProcess, today);
				HashMap<String,String> unfinishedCommunity = new HashMap<String,String>();
				unfinishedCommunity.put("id", unfinishedProcess.get("community_id").toString());
				unfinishedCommunity.put("url", unfinishedProcess.get("community_url").toString());
				unfinishedCommunity.put("tab", unfinishedProcess.get("current_tab").toString());
				//2: Save Tweets of unfinished community
				saveCommunityTweets(unfinishedCommunity);
				//3: Save Tweets of unfinished community ideas
				saveCommunityIdeasTweets(unfinishedCommunity,-1);
				//4: Finishing synchronization of the unfinished process
				finishCommunitySync(startingTime, unfinishedCommunity.get("url"),
								    unfinishedCommunity.get("id"), today,
								    -1);
				//5: Pause for a moment to avoid being banned
				pause();
			}
			cpCommunities = db.getCivicParticipationCommunities();
			System.out.println("");
			Util.printMessage("Starting the synchronization of civic-participation communities...","info",logger);
			for (HashMap<String,String> cpCommunity : cpCommunities) {
				System.out.println("");
				Util.printMessage("Synchronizing community: " + cpCommunity.get("name"),"info",logger);
                String url = cpCommunity.get("url");
                if (commInfoReader.existsCommunity(url)) {
                    startingTime = System.currentTimeMillis();
                    if (commInfoReader.checkHTTPSecureProtocol(url)) {
                        int colon = url.indexOf(":");
                        //Convert http to https
                        url = url.substring(0, colon) + "s" + 
                              url.substring(colon);
                    }				
                    //1: Sync Community Ideas and Social Network Statistics
                    boolean processed = syncCommunitySnStatsAndIdeas(cpCommunity,-1);
                    if (processed) {
	                    //2: Save Community Tweets
	                    saveCommunityTweets(cpCommunity);
	                    //3: Save Community Ideas Tweets
	                    saveCommunityIdeasTweets(cpCommunity,-1);
                    }
	                //4: Finishing synchronization
	                finishCommunitySync(startingTime,cpCommunity.get("name"),
	                                    cpCommunity.get("id"), today, -1);
	                //5: Pause for a moment to avoid being banned
	                pause();
                }
                else {
                    Util.printMessage("The community doesn't exist","info",logger);   
                }
			}		
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
			createErrorFile();
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
			createErrorFile();
		}
	}
    
	private static void finishCommunitySync(long startingTime, String communityName, 
											String communityId, Date today,
											Integer observation) 
	throws ParseException, SQLException {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		HashMap<String,String> duration = calcOpDuration(startingTime);
		
		Util.printMessage("The syncronization of communities'ideas has " +
						  "finished with a duration of: " + 
						  duration.get("hours") + ":" + 
				 	 	  duration.get("minutes")  + ":" + 
				 	 	  duration.get("seconds"),"info",logger);
		
		//Register operation in Audit table
		Date opDuration = timeFormat.parse(duration.get("hours") + ":" + 
										   duration.get("minutes")  + ":" + 
										   duration.get("seconds"));
		String opMsg = "Synchronization of the ideas of the community: " + 
						communityName;
		
		db.updateSyncFlag(communityId);				
		db.registerOperation(today, opMsg, opDuration, observation);	
	}
	
	private static void resumeUnfinishedProcess(HashMap<String,Object> unfinishedProcess,
															 Date today) 
	throws Exception {
		commInfoReader.resumeSyncProcess(unfinishedProcess, db);
	}
	
	private static boolean syncCommunitySnStatsAndIdeas(HashMap<String,String> community,
										  			 Integer observation) 
	throws Exception 
	{
		HashMap<String,Object> communityStats = null;
		
		String url = community.get("url");
		String communityId = community.get("id");
		
		communityStats = commInfoReader.syncCommunityStats(communityId, url, db);
		if (communityStats.get("status").equals("active")) {
			checkIncrementSNCounters(observation,community,communityStats);
		
			ArrayList<HashMap<String,String>> tabs = (ArrayList<HashMap<String,String>>) communityStats.get("tabs");
			commInfoReader.syncIdeas(url, Integer.parseInt(communityId), tabs, 0, db, observation);
			
			return true;
		}
		else {
			return false;
		}
	}
	
	private static void checkIncrementSNCounters(Integer observation, 
												 HashMap<String,String> old,
												 HashMap<String,Object> current) 
	throws SQLException {
		
		if (current.get("facebook") != null && current.get("twitter") != null &&
			old.get("facebook") != null && old.get("twitter") != null) 
		{
			Integer currentFBCounter = Integer.parseInt((String) current.get("facebook"));
			Integer currentTWCounter = Integer.parseInt((String) current.get("twitter"));
			Integer oldFBCounter = Integer.parseInt((String) old.get("facebook"));
			Integer oldTWCounter = Integer.parseInt((String) old.get("twitter"));
			if (currentFBCounter > oldFBCounter || currentTWCounter > oldTWCounter) {
				if (current.get("votes") != null && old.get("votes") != null) {
					if (Integer.parseInt((String)current.get("votes")) < 
						Integer.parseInt(old.get("votes")))
						Util.printMessage("There are less votes than before. " +
										  "Community: " + old.get("url") + ". " +
										  "Before: " + old.get("votes") +
										  " - Now: " + current.get("votes"), 
										  "severe", logger);
				}
				if (current.get("comments") != null && old.get("comments") != null) {
					if (Integer.parseInt((String)current.get("comments")) < 
						Integer.parseInt(old.get("comments")))
						Util.printMessage("There are less comments than before. " +
										  "Community: " + old.get("url") + ". " +
										  "Before: " + old.get("comments") +
										  " - Now: " + current.get("comments"), 
										  "severe", logger);
				}
				if (current.get("ideas") != null && old.get("ideas") != null) {
					if (Integer.parseInt((String)current.get("ideas")) < 
						Integer.parseInt(old.get("ideas")))
						Util.printMessage("There are less ideas than before. " +
										  "Community: " + old.get("url") + ". " +
										  "Before: " + old.get("ideas") +
										  " - Now: " + current.get("ideas"), 
										  "severe", logger);
				}
				db.saveLogCommunity(observation, old, current);
			}
		}
		else {
			Util.printMessage("Some of the SN counters of the community " + 
						   	  old.get("name") + " are null", "info", logger);
		}
		
	}
	
	private static void saveCommunityTweets(HashMap<String,String> community) 
	throws Exception {
		String url = community.get("url");
		String idCommunity = community.get("id");
		
		ArrayList<HashMap<String,Object>> tweets = ts.GetTweets(url);
		if (tweets.size() > 0) {
			for (HashMap<String,Object> tweet : tweets) {
				String idTweet = (String) tweet.get("id");
				if (!db.tweetAlreadyInserted(idTweet))
					//Saves only if the tweet wasn't saved previously
					db.insertCommunityTweets(tweet, idCommunity);
			}
		}
	}
	
	private static void saveCommunityIdeasTweets(HashMap<String,String> community,
												 Integer observation) 
 	throws Exception 
 	{
		String idCommunity = community.get("id");
		ArrayList<HashMap<String,String>> ideas = db.getCommunityIdeas(idCommunity);
		ArrayList<HashMap<String,String>> remainingIdeas = new ArrayList<HashMap<String,String>>();
		
		if (community.containsKey("tab")) {   //Finishing an unfinished process
			String idUnSavedIdea = community.get("tab");
			boolean found = false;
			for (HashMap<String,String> idea : ideas) {
				if (idea.get("id").equals(idUnSavedIdea) || found) {
					found = true;
					remainingIdeas.add(idea);
				}
			}
		}
		else {
			remainingIdeas = ideas;
		}
		db.cleanSyncProgressTable();
		for (HashMap<String,String> idea : remainingIdeas) {
			String url = idea.get("url");
			String idIdea = idea.get("id");
			db.insertSyncProcess(url, idIdea, -1, 
								Integer.parseInt(idCommunity), 
								observation);
			ArrayList<HashMap<String,Object>> tweets = ts.GetTweets(url);
			if (tweets.size() > 0) {
				for (HashMap<String,Object> tweet : tweets) {
					String idTweet = (String) tweet.get("id");
					if (!db.tweetAlreadyInserted(idTweet))
						//Saves only if the tweet wasn't saved previously
						db.insertTweetsIdea(tweet, idIdea);	
				}
			}
			db.cleanSyncProgressTable();
		}
	}
	
	private static HashMap<String,String> calcOpDuration(long startingTime) {
		long etH = 0, etM = 0, etS = 0;
		String sETH = "", sETM = "", sETS = "";
		HashMap<String,String> duration = new HashMap<String,String>();
		
		long finishingTime = System.currentTimeMillis();
	    etH = (finishingTime - startingTime) / (60 * 60 * 1000) % 24;
	    if (etH < 10) sETH = "0"+etH; else sETH = Long.toString(etH);
	    etM = (finishingTime - startingTime) / (60 * 1000) % 60;
	    if (etM < 10) sETM = "0"+etM; else sETM = Long.toString(etM);
	    etS = (finishingTime - startingTime) / 1000 % 60;
	    if (etS < 10) sETS = "0"+etS; else sETS = Long.toString(etS);
	    
	    duration.put("hours", sETH);
	    duration.put("minutes", sETM);
	    duration.put("seconds", sETS);
	    
	    return duration;
	}
	
	public static void updateTweetsMetrics() {
		Calendar cal = Calendar.getInstance();
		Date today = cal.getTime();
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		long startingTime = System.currentTimeMillis();
		
		try {
			//Update metrics of tweets related to communities
			ArrayList<HashMap<String,String>> tweets = db.getCommunitiesTweets();
			for (HashMap<String,String> tweet : tweets) {
				HashMap<String,Long> newMetrics = tu.updateTweetMetrics(tweet.get("id_tweet"));
                if (newMetrics.size() > 0)
				    db.updateCommunityTweetMetric(tweet.get("id"), newMetrics);
			}
			//Update metrics of tweets related to ideas
			tweets = db.getIdeasTweets();
			for (HashMap<String,String> tweet : tweets) {
				HashMap<String,Long> newMetrics = tu.updateTweetMetrics(tweet.get("id_tweet"));
                if (newMetrics.size() > 0)
				    db.updateIdeaTweetMetric(tweet.get("id"), newMetrics);
			}
			Util.printMessage("Tweets Metrics Update finished.", "info", logger);
			
			HashMap<String,String> duration = calcOpDuration(startingTime);
			Date opDuration = timeFormat.parse(duration.get("hours") + ":" + 
					   						   duration.get("minutes")  + ":" + 
					   						   duration.get("seconds"));
			db.registerOperation(today, "Updating tweet metrics", opDuration,-1);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
			createErrorFile();
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE,e.getMessage(),e);
			createErrorFile();
		}
	}
	
	public static void pause() throws InterruptedException {
		//Wait for a moment to avoid being banned
		double rand = Math.random() * 5;		        			
		Thread.sleep((long) (rand * 1000));
	}
	
	/*public static void checkCommunitiesExistence(CommunityInfoReader commInfoReader,
			 									  DBManager db) {
		ArrayList<HashMap<String,String>> communities = getCommunitiesURLs(db,null);
		System.out.println("Checking communities existence, please wait...");
		for (HashMap<String,String> community : communities) {
			try {
				commInfoReader.existsCommunity(community.get("url"));
			} catch (Exception e) {
				System.out.println("Community " + community.get("name") + " does not exist!");
			}
		}
	}
	
	public static void recordCommunitiesLifespan(CommunityInfoReader commInfoReader,
					  DBManager db) 
	{
		System.out.println("Saving Communities LifeSpan, please wait...");
		ArrayList<HashMap<String,String>> communities = getCommunitiesURLs(db,getFilters());
		HashMap<String,Object> communityDates = new HashMap<String,Object>();
		communityDates.put("dateLastIdea", null);
		communityDates.put("dateFirstIdea", null);
		communityDates.put("lifespan", null);
		communityDates.put("status", "unexisting");
		for (HashMap<String,String> community : communities) {
			try {
				System.out.println("Processing community: " + community.get("name") + " ...");
				communityDates = commInfoReader.getCommunityLifeSpan(community.get("url"),null);
			} catch(Exception e) {
				System.out.println("Problem:  getting dates from community with id " + community.get("id") + " message: ");
				e.printStackTrace();
			} finally {
				try {
					db.saveCommunityDates(community.get("id"),communityDates);
				} catch(SQLException e) {
					System.out.println("Problem saving data into de database");
					e.printStackTrace();
				}
			}
		}
		System.out.println("End");
	}
	
	public static void recordInactiveCommunitiesLifespan(CommunityInfoReader commInfoReader,
				  		  								 DBManager db) 
	{
		ArrayList<ArrayList<String>> filters = new ArrayList<ArrayList<String>>();
		ArrayList<String> filter = new ArrayList<String>();
		filter.add("status");
		filter.add("=");
		filter.add("'inactive'");
		filters.add(filter);
		ArrayList<String> filter2 = new ArrayList<String>();
		filter2.add("outlier");
		filter2.add("=");
		filter2.add("0");
		filters.add(filter2);
		ArrayList<String> filter3 = new ArrayList<String>();
		filter3.add("lastidea_ts");
		filter3.add(">");
		filter3.add("'2013-01-01'");
		filters.add(filter3);
		ArrayList<HashMap<String,String>> communities = getCommunitiesURLs(db,filters);
		for (HashMap<String,String> community : communities) {
			try {
				System.out.println("Processing community: " + community.get("name") + " ...");
				HashMap<String,Object> communityDates = commInfoReader.getCommunityLifeSpan(community.get("url"),null);
				db.saveCommunityDates(community.get("id"),communityDates);
			} catch(Exception e) {
				System.out.println("Problem: " + e.getMessage() + " getting dates from community with id " + community.get("id"));
			}
		}
	}
	
	private static ArrayList<HashMap<String,String>> getCommunitiesURLs(DBManager db, 
																		ArrayList<ArrayList<String>> filter) 
	{
		ArrayList<HashMap<String,String>> communitiesURLs = db.getCommunitiesURL(filter);
		return communitiesURLs;
	}
	
	private static ArrayList<ArrayList<String>> getFilters() {
		ArrayList<ArrayList<String>> filters = null;
		ArrayList<ArrayList<String>> filters = new ArrayList<ArrayList<String>>();
		ArrayList<String> filter = new ArrayList<String>();
		filter.add("outlier");
		filter.add("=");
		filter.add("1");
		filters.add(filter);
		ArrayList<String> filter2 = new ArrayList<String>();
		filter2.add("lifespan");
		filter2.add("is");
		filter2.add("NULL");
		filters.add(filter2);
		
		return filters;
	}
	
	public static void crawlIdeaScaleDir(CommunityInfoReader commInfoReader) {
		Reporter reporter = new Reporter();
		System.out.println("Collecting information about Ideascale communities, please wait...");
		List<HashMap<String,Object>> communities = commInfoReader.getCommunitiesInfo();
		System.out.println("Writing the report, please wait...");
		//reporter.createReport(communities);
		System.out.println("Done!, take a look at the report at " + reporter.getPathReport());
	}
	
	public static void syncCommunityDir(CommunityInfoReader commInfoReader,
										 DBManager db, Logger logger) 
	{
	
		ArrayList<String> letterDir = new ArrayList<String>(Arrays.asList("a","b","c",
			  "d","e","f","g","h","i","j","k","l","m","n","o",
			  "p","q","r","s","t","u","v","w","x","y","z"));
		
		ArrayList<String> totalNewCommunities = new ArrayList<String>();
		
		System.out.println("Synchronizing local DB with Ideascale community directory, please wait...");
		
		ArrayList<String> newCommunitiesCat = syncCommunityCat(commInfoReader,db,letterDir,logger);
		for (int i = 0; i < newCommunitiesCat.size(); i++)
		totalNewCommunities.add(newCommunitiesCat.get(i));
		
		System.out.println("Finished!, next the results of the syncronization...");
		System.out.println("New Communities: " + totalNewCommunities.size());
		for (int i = 0; i < totalNewCommunities.size(); i++)
		System.out.println("- " + totalNewCommunities.get(i));
	}*/
}
