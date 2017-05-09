import javafx.scene.shape.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.commons.cli.*;

public class Distributions {
	public static final int SECONDS_IN_A_DAY = 86400;
	
	// indices for accessing the location arrays in vehiclePaths (see makeVehicleArray())
	public static final int ID = 0;
	public static final int TIME = 1;
	public static final int LON = 2;
	public static final int LAT = 3;
	public static final int REGION = 4;
	
	public static String pathsFilename;
	public static String polygonsFilename;
	
	public static double[] makeDistanceList(double lon, double lat, ArrayList<double[]> regionPoints){
		double[] distances = new double[regionPoints.size()];
		for(int i = 0; i < regionPoints.size(); i++){
			distances[i] = Util.distance(new double[]{lon, lat}, regionPoints.get(i));
		}
		
		return distances;
	}
	
		public static void makeGraph(double [] timeGraphDataList, HashMap<Integer, Integer> timeGraphDataMap, double [] distanceGraphDataList, HashMap<Integer, Integer>distanceGraphDataMap) {		
		// x axis
		double [] timeList = new double[1000];
		double [] distanceList = new double[5000];
		
		if (timeGraphDataMap.isEmpty()){
			System.out.println("TIME HASHMAP IS EMPTY");
		}
		
		if (distanceGraphDataMap.isEmpty()){
			System.out.println("DISTANCE HASHMAP IS EMPTY");
		}
		
		System.out.println(timeGraphDataMap.size());
		System.out.println(distanceGraphDataMap.size());
		
		for (int i = 0; i < 5000; i++){
			if (i < 1000){
				timeList[i] = i;
			}
			distanceList[i] = i;
		}
		
		makeGraphHelper(timeList, timeGraphDataList, "Time Graph");
		makeGraphHelper(distanceList, distanceGraphDataList, "Distance Graph");
		
			
	}
	
	public static void makeGraphHelper(double [] x, double [] y, String title){
		
        Plot2DPanel plot = new Plot2DPanel();
        
        // define the legend position
        plot.addLegend("SOUTH");

        // add a line plot to the PlotPanel
        plot.addLinePlot(title, x, y);

        // put the PlotPanel in a JFrame like a JPanel
        JFrame frame = new JFrame(title);
        frame.setSize(600, 600);
        frame.setContentPane(plot);
        frame.setVisible(true);
		
		
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		long startTime = System.nanoTime();
		
		Options options = new Options();
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
		
		Option rf = new Option("rf", "regionFile", true, "region file path");
        rf.setRequired(true);
        options.addOption(rf);
        
        Option pf = new Option("pf", "pathFile", true, "vehicle-paths file path");
        pf.setRequired(true);
        options.addOption(pf);
        
        Option mst = new Option("mst", "minimumStopTime", true, "Minimum time, in seconds, that a vehicle must stay at a location before it is considered a stop.");
        mst.setRequired(false);
        options.addOption(mst);
        
        try{
            cmd = parser.parse(options, args);
        }
        catch(org.apache.commons.cli.ParseException e){
            System.out.println(e.getMessage());
            formatter.printHelp("Distributions", options);

            System.exit(1);
            return;
        }
        
        pathsFilename = cmd.getOptionValue("pathFile");
        polygonsFilename = cmd.getOptionValue("regionFile");
        int minStopTime = Integer.parseInt(cmd.getOptionValue("minimumStopTime", "180"));
        double[] timeGraphDataList = new double[1000];
        HashMap<Integer, Integer> timeGraphDataMap = new HashMap<Integer, Integer>();
        int[] distanceGraphDataList = new int[5000];
        HashMap<Integer, Integer> distanceGraphDataMap = new HashMap<Integer, Integer>();
        
		ArrayList<Polygon> plistPolygon = makePolygonList();
		ArrayList<JSONArray> plist = makePnpolyPolygonList();
		ArrayList<double[]> regionPoints = makeRegionPoints(plist);
		ArrayList<ArrayList<double[]>> vehiclePaths = makeVehicleArray(pathsFilename, plistPolygon, regionPoints);
		makeGraphData(vehiclePaths, minStopTime, pathsFilename, timeGraphDataList, timeGraphDataMap
				, distanceGraphDataList, distanceGraphDataMap);
		makeGraph(timeGraphDataList, timeGraphDataMap, distanceGraphDataList, distanceGraphDataMap);
		
		for(int i = 0; i < distanceGraphDataList.length; i++){
			if(distanceGraphDataList[i] > 0){
				System.out.println("time: " + i + " freq: " + distanceGraphDataList[i]);
			}
		}
		
		long endTime = System.nanoTime();
		long duration = endTime - startTime;
		double seconds = ((double)duration / 1000000000);
		System.out.println("time elapsed: " + new DecimalFormat("#.##########").format(seconds));
	}
	
	public static ArrayList<Polygon> makePolygonList() throws FileNotFoundException, IOException, ParseException{
		ArrayList<Polygon> polygonList = new ArrayList<Polygon>();
		JSONObject data = (JSONObject) new JSONParser().parse(new FileReader(polygonsFilename));
		JSONArray features = (JSONArray) data.get("features");
		
		for(int i = 0; i < features.size(); i++){
			JSONObject tmp1 = (JSONObject) features.get(i);
			JSONObject tmp2 = (JSONObject) tmp1.get("geometry");
			JSONArray tmp3 = (JSONArray) tmp2.get("coordinates");
			JSONArray tmp4 = (JSONArray) tmp3.get(0);
			double[] xyList = Util.getXY(tmp4);
			Polygon polygon = new Polygon(xyList);
			polygonList.add(polygon);
		}
		
		return polygonList;
	}
	
	public static ArrayList<JSONArray> makePnpolyPolygonList() throws FileNotFoundException, IOException, ParseException{
		ArrayList<JSONArray> polygonList = new ArrayList<JSONArray>();
		JSONObject data = (JSONObject) new JSONParser().parse(new FileReader(polygonsFilename));
		JSONArray features = (JSONArray) data.get("features");
		
		for(int i = 0; i < features.size(); i++){
			JSONObject tmp1 = (JSONObject) features.get(i);
			JSONObject tmp2 = (JSONObject) tmp1.get("geometry");
			JSONArray tmp3 = (JSONArray) tmp2.get("coordinates");
			JSONArray points = (JSONArray) tmp3.get(0);
			polygonList.add(points);
		}
		
		return polygonList;
	}
	
	public static ArrayList<double[]> makeRegionPoints(ArrayList<JSONArray> polygonList){
		ArrayList<double[]> centroids = new ArrayList<double[]>();
		
		for(Object obj : polygonList){
			double x = 0.0;
			double y = 0.0;
			JSONArray polygon = (JSONArray) obj;
			for(Object obj2 : polygon){
				double[] point = Util.toDoubleArray((JSONArray) obj2);
				x += point[0];
				y += point[1];
			}
			centroids.add(new double[]{x / (double) polygon.size(), y / (double) polygon.size()});
		}
		
		return centroids;
	}
	
	public static ArrayList<ArrayList<double[]>> makeVehicleArray(String filename, ArrayList<Polygon> polygonList, ArrayList<double[]> regionPoints) throws IOException{
		String line = "";
		int lastVehicleId = Util.getLastVehicleId(filename);
		BufferedReader f = new BufferedReader(new FileReader(filename));
		
		ArrayList<ArrayList<double[]>> vehiclePaths = new ArrayList<ArrayList<double[]>>();
		for(int i = 0; i <= lastVehicleId; i++){
			vehiclePaths.add(new ArrayList<double[]>());
		}
		
		long whichPolyTime = 0;
	
		while ((line = f.readLine()) != null) {
			if(line.indexOf(',') == -1){
				continue;
			}
			String[] row = line.split(",");
			double id = Double.parseDouble(row[0]);
			double time = Double.parseDouble(row[1]);
			double lon = Double.parseDouble(row[2]);
			double lat = Double.parseDouble(row[3]);
			
			//System.out.println(line);
			
			int region = -1;
			
			// 10 closest regions
			double[] distanceList = makeDistanceList(lon, lat, regionPoints);
			int[] smallestIndices = new int[10];
			for(int i = 0; i < 10; i++){
				double smallest = Double.MAX_VALUE;
				for(int j = 0; j < distanceList.length; j++){
					double distance = distanceList[j];
					if(distance < smallest){
						smallest = distance;
						smallestIndices[i] = j;
					}
				}
				distanceList[smallestIndices[i]] = Double.MAX_VALUE;
			}
			
			// check top 10
			for(int index : smallestIndices){
				if(polygonList.get(index).contains(lon, lat)){
					region = index;
					break;
				}
			}
			
			long s = System.nanoTime();
			// check the rest
			if(region == -1){
				region = whichPolygon(lon, lat, polygonList);
			}
			whichPolyTime += System.nanoTime() - s;
			
			double[] v = {id, time, lon, lat, (double) region};
			vehiclePaths.get((int) v[0]).add(v);
		}
		
		double seconds = ((double)whichPolyTime / 1000000000);
		System.out.println("whichpoly time elapsed: " + new DecimalFormat("#.##########").format(seconds));
		f.close();
		
		return vehiclePaths;
	}
	
	public static int whichPolygon(double lon, double lat, ArrayList<Polygon> polygonList){
		int region = 0;
		for(Polygon polygon : polygonList){
			if(polygon.contains(lon, lat)){
				return region;
			}
			region += 1;
		}
		
		return region;
	}
	
	public static void makeGraphData(ArrayList<ArrayList<double[]>> vehiclePaths, int minStopTime,
	String filename, int[] timeGraphDataList, HashMap<Integer, Integer> timeGraphDataMap,
	int[] distanceGraphDataList, HashMap<Integer, Integer> distanceGraphDataMap)throws IOException{	
		for(ArrayList<double[]> path : vehiclePaths){
			// can't have an origin and destination
			if(path.size() < 2){
				continue;
			}
			
			double[] firstLocation = path.get(0);
			double[] originCoord = {firstLocation[LON], firstLocation[LAT]};
			double[] prevLocationCoord = null;
			double prevLocationTime = -1.0;
			double prevLocationDuration = 0;
			double tripDistance = 0; // in km
			double tripTime = 0;
			
			for(int i = 1; i < path.size(); i++){
				double[] location = path.get(i);
				double locationTime = location[TIME];
				double[] locationCoord = {location[LON], location[LAT]};
				
				
				// we are still at the previous location so skip
				if(Util.isSamePoint(locationCoord, originCoord)){
					continue;
				}
				// just set a new origin so there's no previous location since leaving the origin
				if(prevLocationCoord == null){
					prevLocationCoord = locationCoord;
					prevLocationTime = locationTime;
				}
				// vehicle stayed in same place
				else if(Util.isSamePoint(prevLocationCoord, locationCoord)){
					// update time spent at location
					prevLocationDuration += locationTime - prevLocationTime;
					tripTime += locationTime - prevLocationTime;
					prevLocationTime = locationTime;
					
					if(prevLocationDuration >= minStopTime){
						// add to graph data
						int tripTimeMinutes = (int)(tripTime / 60.0);
						if(tripTimeMinutes >= timeGraphDataList.length){
							int frequency = timeGraphDataMap.containsKey(tripTimeMinutes) ?
									timeGraphDataMap.get(tripTimeMinutes) + 1 : 1;
							timeGraphDataMap.put(tripTimeMinutes, frequency);
						}
						else{
							timeGraphDataList[tripTimeMinutes] += 1;
						}
						int tripDistanceRounded = (int)tripDistance;
						if(tripDistanceRounded >= distanceGraphDataList.length){
							int frequency = distanceGraphDataMap.containsKey(tripDistanceRounded) ?
									distanceGraphDataMap.get(tripDistanceRounded) + 1 : 1;
							distanceGraphDataMap.put(tripDistanceRounded, frequency);
						}
						else{
							distanceGraphDataList[tripDistanceRounded] += 1;
						}
						
						// update origin to location
						originCoord = locationCoord;
						
						// reset prev location and distance and time
						prevLocationCoord = null;
						prevLocationTime = -1;
						prevLocationDuration = 0;
						tripDistance = 0;
						tripTime = 0;
					}
				}
				// vehicle moved to another location
				else{
					tripDistance += Util.distanceKm(prevLocationCoord, locationCoord);
					tripTime += locationTime - prevLocationTime;
					prevLocationCoord = locationCoord;
					prevLocationTime = locationTime;
					prevLocationDuration = 0;
				}
			}
		}
		
		return;
	}
}
