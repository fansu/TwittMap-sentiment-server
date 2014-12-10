<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="com.amazonaws.*" %>
<%@ page import="com.amazonaws.auth.*" %>
<%@ page import="com.amazonaws.services.ec2.*" %>
<%@ page import="com.amazonaws.services.ec2.model.*" %>
<%@ page import="com.amazonaws.services.s3.*" %>
<%@ page import="com.amazonaws.services.s3.model.*" %>
<%@ page import="com.amazonaws.services.dynamodbv2.*" %>
<%@ page import="com.amazonaws.services.dynamodbv2.model.*" %>

<%@ page import="com.datastax.driver.core.Cluster" %>
<%@ page import="com.datastax.driver.core.Metadata" %>
<%@ page import="com.datastax.driver.core.Session" %>
<%@ page import="com.datastax.driver.core.ResultSet" %>
<%@ page import="com.datastax.driver.core.Row" %>
<%@ page import="com.datastax.driver.core.policies.DCAwareRoundRobinPolicy" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="twitter4j.*" %>
<%@ page import="twitter4j.QueryResult" %>
<%@ page import="twitter4j.conf.ConfigurationBuilder" %>

<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.net.HttpURLConnection" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.net.URLEncoder" %>

<%@ page import="org.json.simple.JSONArray" %>
<%@ page import="org.json.simple.JSONObject" %>
<%@ page import="org.json.simple.JSONValue" %>

<%! // Share the client objects across threads to
    // avoid creating new clients for each web request
    private AmazonEC2         ec2;
    private AmazonS3           s3;
    private AmazonDynamoDB dynamo;
 %>
<%
    /*
     * AWS Elastic Beanstalk checks your application's health by periodically
     * sending an HTTP HEAD request to a resource in your application. By
     * default, this is the root or default resource in your application,
     * but can be configured for each environment.
     *
     * Here, we report success as long as the app server is up, but skip
     * generating the whole page since this is a HEAD request only. You
     * can employ more sophisticated health checks in your application.
     */
    if (request.getMethod().equals("HEAD")) return;
%>

<%
    if (ec2 == null) {
        AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        ec2    = new AmazonEC2Client(credentialsProvider);
        s3     = new AmazonS3Client(credentialsProvider);
        dynamo = new AmazonDynamoDBClient(credentialsProvider);
    }

	String choice = request.getParameter("dropdown");	
	
	ConfigurationBuilder cb = new ConfigurationBuilder();
%>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8">
    <title>TwittMap Application</title>
    <link rel="stylesheet" href="styles/styles.css" type="text/css" media="screen">
        <style>
      #map-canvas_positive, #map-canvas_neutral, #map-canvas_negative {
        width: 90%;
        height: 200px;
        margin-top: 50px;
      }
      body {
     	height: 100%;
     	width: 100%;
      	padding-left: 50px;
      	padding-right: 50px;
      }
      .button-borders{
        height: 64px;
      }
      .button-bottom{
        padding-bottom: 0;
      }
    </style>
    <link rel="stylesheet" href="//code.jquery.com/ui/1.11.2/themes/smoothness/jquery-ui.css">
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css">
    <script src="http://code.jquery.com/jquery-1.10.2.js"></script>
    <script src="http://code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
	<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>
    <script src="https://maps.googleapis.com/maps/api/js?v=3.exp&libraries=visualization"></script>    
    <script>
// Adding 500 Data Points
var map, pointarray, heatmap_positive,heatmap_neutral, heatmap_negative;

function initialize() {
  var mapOptions = {
    zoom: 2,
    center: new google.maps.LatLng(33.774546, -122.433523),
    mapTypeId: google.maps.MapTypeId.SATELLITE
  };

  map_positive = new google.maps.Map(document.getElementById('map-canvas_positive'),
      mapOptions);
  map_neutral = new google.maps.Map(document.getElementById('map-canvas_neutral'),
	      mapOptions);
  map_negative = new google.maps.Map(document.getElementById('map-canvas_negative'),
	      mapOptions);
  var pos_num = 0;
  var net_num = 0;
  var neg_num = 0;
  
  window.setInterval(function (){
	  heatmap = null;
    var keywords = 'halloween';
	$.get('Servlet', {
	    keyword : keywords
	}, function(responseText) {		
	  $.each(responseText, function(key, val) {
		var data = JSON.stringify(val);
		var data_s = data.substr(1, data.length-2);
		var data_spl = data_s.split(',');
		var lat = data_spl[0];
		var lon = data_spl[1];
		var sentiment = data_spl[2];
		if (sentiment == 1){
			var taxiData = [
                new google.maps.LatLng(lat, lon)];
       		var pointArray = new google.maps.MVCArray(taxiData);
      		heatmap_positive = new google.maps.visualization.HeatmapLayer({data: pointArray});	
      		heatmap_positive.setMap(map_positive);
			pos_num++;
		} else if (sentiment == 0){
			var taxiData = [
                new google.maps.LatLng(lat, lon)];
       		var pointArray = new google.maps.MVCArray(taxiData);
      		heatmap_neutral = new google.maps.visualization.HeatmapLayer({data: pointArray});	
      		heatmap_neutral.setMap(map_neutral);
			net_num++;
		} else if (sentiment == -1){
			var taxiData = [
                new google.maps.LatLng(lat, lon)];
       		var pointArray = new google.maps.MVCArray(taxiData);
      		heatmap_negative = new google.maps.visualization.HeatmapLayer({data: pointArray});	
      		heatmap_negativee.setMap(map_negative);
			neg_num++;
		}	
		document.getElementById("num_table").rows[1].cells[1].innerHTML = pos_num;
		document.getElementById("num_table").rows[1].cells[2].innerHTML = net_num;
		document.getElementById("num_table").rows[1].cells[3].innerHTML = neg_num;
  	  });
	});
  }, 3000);
}

google.maps.event.addDomListener(window, 'load', initialize);

</script>
</head>
<body>
<nav class="navbar navbar-default" role="navigation">
  <div class="container-fluid">
    <!-- Brand and toggle get grouped for better mobile display -->
    <div class="navbar-header">
      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="/index">TwittMap</a>
    </div>

    <!-- Collect the nav links, forms, and other content for toggling -->
    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
      <ul class="nav navbar-nav">
        <li><a href="/index">Main</a></li>
        <li><a href="/real-time">Real-time</a></li>
        <li class="active"><a href="/sentiment">Sentiment</a></li>
        <li><a href="/about">About</a></li>
      </ul>
    </div>
   </div>
 </nav>
  <h1 class="text-center">TwittMap Application Real time Display and Sentiment Analysis</h1>
  <h2>Sentiment Data</h2>
  <table class="table table-striped" id="num_table">
  <tr>
    <th>Number</th>
    <th>Positive</th>
    <th>Neutral</th>
    <th>Negative</th>
  </tr>
  <tr>
    <td></td>
    <td>0</td>
    <td>0</td>
    <td>0</td>
  </tr>
  </table>
  <h2>Sentiment Map</h2>
  <div class="row">
    <div class="col-md-4">
      <h4>Positive Tweet heatmap</h4>
	  <div id="map-canvas_positive"></div>
	</div>
    <div class="col-md-4">
      <h4>Neutral Tweet heatmap</h4>
	  <div id="map-canvas_neutral"></div>
	  </div>
    <div class="col-md-4">
      <h4>Negative Tweet heatmap</h4>
	  <div id="map-canvas_negative"></div>
	</div>
  </div>
</body>
</html>