
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.google.gson.Gson;

/**
 * Servlet implementation class Servlet
 */

class Pair {
	Double lats;
	Double lons;
}
public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
	public AmazonSQS sqs;
	public String myQueueUrl;
    public Servlet() {
        super();

        System.out.println("--Reading credential information...");
    	AWSCredentials credentials;
		try {
			credentials = new PropertiesCredentials(
					Servlet.class.getResourceAsStream("AwsCredentials.properties"));
	        sqs = new AmazonSQSClient(credentials);
	        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
	        sqs.setRegion(usWest2);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	System.out.println("--Read credential information.");
    	


        // TODO Auto-generated constructor stub
    	TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    	final Cluster cluster;
		final Session session;
		cluster = Cluster.builder().withLoadBalancingPolicy(new DCAwareRoundRobinPolicy("")).addContactPoint("").build();
		
		session = cluster.connect("demo");
	    StatusListener listener = new StatusListener(){
	        public void onStatus(Status t) {
	        	try {
		    		final String keywords = "halloween";
					HttpURLConnection conn;
					String location;
					if (t.getGeoLocation() == null){
						if (t.getUser().getLocation() != null && t.getUser().getLocation().length() > 8 && !t.getUser().getLocation().substring(0, 8).equals("Location"))
						{
							try {
								location = URLEncoder.encode(t.getUser().getLocation(), "UTF-8").replace("+", "%20");
								URL url = new URL("http://nominatim.openstreetmap.org/search/" + location + "?format=json&addressdetails=1&limit=1&polygon_svg=1");
								conn = (HttpURLConnection) url.openConnection();
								conn.setRequestMethod("GET");
								conn.setRequestProperty("Accept", "application/json");
						 
								if (conn.getResponseCode() == 200) {
									BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
									String output;
									if (br != null){
										output = br.readLine();
										if (output!=null){
											JSONArray array = (JSONArray)JSONValue.parse(output);
											if (array != null){
												if (array.size()!=0){
													JSONObject obj2=(JSONObject)array.get(0);
													String text = t.getText().replace("'","");
													java.util.Date date= new java.util.Date();
													String q = "INSERT INTO twitt (keywords, id, content, createddate, date, latitude, longitude) VALUES (" + "'" + keywords + "', "+ t.getId() + ", '" + text + "', " + date.getTime() + "," + t.getCreatedAt().getTime() + ", " + obj2.get("lat") + ", " + obj2.get("lon") + ") using TTL 5;";
													session.execute(q);
											//		sqs.sendMessage(new SendMessageRequest(myQueueUrl, "'" + keywords + "', "+ t.getId() + ", '" + text + "', " + date.getTime() + "," + t.getCreatedAt().getTime() + ", " + obj2.get("lat") + ", " + obj2.get("lon")));
													String q2 = "INSERT INTO twitt_all (keywords, id, content, createddate, date, latitude, longitude) VALUES (" + "'" + keywords + "', "+ t.getId() + ", '" + text + "', " + date.getTime() + "," + t.getCreatedAt().getTime() + ", " + obj2.get("lat") + ", " + obj2.get("lon") + ");";
													session.execute(q2);
													sqs.sendMessage(new SendMessageRequest("", keywords + "::"+ t.getId() + "::" + text + "::" + date.getTime() + "::" + t.getCreatedAt().getTime() + "::" + obj2.get("lat") + "::" + obj2.get("lon")));										
												}
											}
										}
									}
									conn.disconnect();								
								}			
							} catch (IOException  | IllegalArgumentException ie) {
								// TODO Auto-generated catch block
//								e.printStackTrace();
							} 
						}
					} else {
						String text = t.getText().replace("'","");
						java.util.Date date= new java.util.Date();
						String q = "INSERT INTO twitt (keywords, id, content, createddate, date, latitude, longitude) VALUES (" + "'" + keywords + "', "+ t.getId() + ", '" + text + "', " + date.getTime() + ", " + t.getCreatedAt().getTime() + ", " + t.getGeoLocation().getLatitude() + ", " + t.getGeoLocation().getLongitude() + ") using TTL 5;";
						session.execute(q);
						String q2 = "INSERT INTO twitt_all (keywords, id, content, createddate, date, latitude, longitude) VALUES (" + "'" + keywords + "', "+ t.getId() + ", '" + text + "', " + date.getTime() + ", " + t.getCreatedAt().getTime() + ", " + t.getGeoLocation().getLatitude() + ", " + t.getGeoLocation().getLongitude() + ");";
						session.execute(q2);
						sqs.sendMessage(new SendMessageRequest("", keywords + "::"+ t.getId() + "::" + text + "::" + date.getTime() + "::" + t.getCreatedAt().getTime() + "::" + t.getGeoLocation().getLatitude() + "::" + t.getGeoLocation().getLongitude()));
					}
	        	} catch (IllegalArgumentException e) {
					cluster.close();
					session.close();
				} 
	        	
	        }
	        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
	        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
	        public void onException(Exception ex) {
//	            ex.printStackTrace();
	        }
			@Override
			public void onScrubGeo(long arg0, long arg1) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub
				
			}
	    }; 
		
		FilterQuery q = new FilterQuery();
		String[] keywordsArray = { "halloween"};
	    q.track(keywordsArray);		
	    twitterStream.addListener(listener);
		twitterStream.filter(q);
		
	    // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
	    twitterStream.sample();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String keywords = request.getParameter("keyword");
        
//        List<Double[]> locations = new ArrayList<Double[]>();
        List<Double[]> locations = new ArrayList<Double[]>();
        int find_count = 0;
        AmazonSQS sqs = new AmazonSQSClient(new AWSCredentialsProviderChain(
		        new InstanceProfileCredentialsProvider(),
		        new ClasspathPropertiesFileCredentialsProvider()));
        ReceiveMessageResult messages = sqs.receiveMessage("");
    	List<Message> ms = messages.getMessages();
    	
    	String[] bo = null;
		bo = ms.get(0).getBody().split("\"");
		Double[] tmp = new Double[3];
		tmp[0] = Double.parseDouble(bo[15].split(",")[0]);
		tmp[1] = Double.parseDouble(bo[15].split(",")[1]);
		if (bo[15].split(",")[2].startsWith("neutral"))
			tmp[2] = Double.parseDouble("0");
		else if (bo[15].split(",")[2].startsWith("positive"))
			tmp[2] = Double.parseDouble("1");
		else if (bo[15].split(",")[2].startsWith("negative"))
			tmp[2] = Double.parseDouble("-1");
		locations.add(tmp);
    		
		String messageReceiptHandle = ms.get(0).getReceiptHandle();
		sqs.deleteMessage(new DeleteMessageRequest()
	        .withQueueUrl("")
	        .withReceiptHandle(messageReceiptHandle));
       
    	Gson gson = new Gson();
    	String json =gson.toJson(locations);
        response.setContentType("application/json");
        PrintWriter out = response.getWriter(); 
        out.print(json);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
}
