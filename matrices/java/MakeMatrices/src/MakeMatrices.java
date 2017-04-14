import javafx.scene.shape.*;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MakeMatrices {
	public static double distance(double[] p1, double[] p2){
		return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
	}
	
	public static double[] makeDistanceList(double lon, double lat, ArrayList<double[]> regionPoints){
		double[] distances = new double[regionPoints.size()];
		for(int i = 0; i < regionPoints.size(); i++){
			distances[i] = distance(new double[]{lon, lat}, regionPoints.get(i));
		}
		
		return distances;
	}
	
	public static int getLastVehicleId(String filename) throws IOException{
		BufferedReader f = new BufferedReader(new FileReader(filename));
		String lastLine = "";
		String line = "";
		while ((line = f.readLine()) != null) {
			lastLine = (line.indexOf(',') == -1) ? lastLine : line;
		}
		int lastVehicleId = Integer.parseInt(lastLine.split(",")[0]);
		f.close();
		
		return lastVehicleId;
	}
	
	public static double[] toDoubleArray(JSONArray j){
		double[] d = new double[j.size()];
		for(int i = 0; i < j.size(); i++){
			d[i] = (double) j.get(i);
		}
		
		return d;
	}
	
	public static double[] getXY(JSONArray points){
		double[] x = new double[points.size() * 2];
		for(int i = 0, j = 0; i < points.size(); i++, j += 2){
			JSONArray point = (JSONArray) points.get(i);
			x[j] = (double) point.get(0);
			x[j + 1] = (double) point.get(1);
		}
		
		return x;
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		long startTime = System.nanoTime();
		
		ArrayList<Polygon> plistPolygon = makePolygonList();
		ArrayList<JSONArray> plist = makePnpolyPolygonList();
		ArrayList<double[]> regionPoints = makeRegionPoints(plist);
		ArrayList<ArrayList<double[]>> vehiclePaths = makeVehicleArray("data/private_raw_p.txt", plistPolygon, regionPoints);
		
		long endTime = System.nanoTime();
		long duration = endTime - startTime;
		double seconds = ((double)duration / 1000000000);
		System.out.println("time elapsed: " + new DecimalFormat("#.##########").format(seconds));
	}
	
	public static ArrayList<Polygon> makePolygonList() throws FileNotFoundException, IOException, ParseException{
		ArrayList<Polygon> polygonList = new ArrayList<Polygon>();
		JSONObject data = (JSONObject) new JSONParser().parse(new FileReader("data/shenzhen_tran_mapbox_polygon.json"));
		JSONArray features = (JSONArray) data.get("features");
		
		for(int i = 0; i < features.size(); i++){
			JSONObject tmp1 = (JSONObject) features.get(i);
			JSONObject tmp2 = (JSONObject) tmp1.get("geometry");
			JSONArray tmp3 = (JSONArray) tmp2.get("coordinates");
			JSONArray tmp4 = (JSONArray) tmp3.get(0);
			double[] xyList = getXY(tmp4);
			Polygon polygon = new Polygon(xyList);
			polygonList.add(polygon);
		}
		
		return polygonList;
	}
	
	public static ArrayList<JSONArray> makePnpolyPolygonList() throws FileNotFoundException, IOException, ParseException{
		ArrayList<JSONArray> polygonList = new ArrayList<JSONArray>();
		JSONObject data = (JSONObject) new JSONParser().parse(new FileReader("data/shenzhen_tran_mapbox_polygon.json"));
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
				double[] point = toDoubleArray((JSONArray) obj2);
				x += point[0];
				y += point[1];
			}
			centroids.add(new double[]{x / (double) polygon.size(), y / (double) polygon.size()});
		}
		
		return centroids;
	}
	
	public static ArrayList<ArrayList<double[]>> makeVehicleArray(String filename, ArrayList<Polygon> polygonList, ArrayList<double[]> regionPoints) throws IOException{
		String line = "";
		int lastVehicleId = getLastVehicleId(filename);
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
}
