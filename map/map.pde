import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.providers.Yahoo;
import de.fhpotsdam.unfolding.*;
import de.fhpotsdam.unfolding.geo.*;
import de.fhpotsdam.unfolding.utils.*;
import de.fhpotsdam.unfolding.marker.*;

UnfoldingMap map;
MarkerManager<Marker> markerManager;

ArrayList<ArrayList<float[]>> private_data;
ArrayList<ArrayList<float[]>> bus_data;
ArrayList<ArrayList<float[]>> taxi_data;
ArrayList<ArrayList<float[]>> truck_data;
int count = 0;

ArrayList<ArrayList<float[]>> readFile(String filename) {
  String path = "C:\\Users\\ishan\\PycharmProjects\\ind_study\\";
  BufferedReader reader = createReader(path+filename);
  
  ArrayList<ArrayList<float[]>> locations = new ArrayList<ArrayList<float[]>>(1440);
  for (int x = 0; x<1440; x++) {
    ArrayList<float[]> arr = new ArrayList<float[]>();
    locations.add(arr);
  }
  float prevId = -1.0;
  int prevIntervalIndex = -1;
  String line = "";
  while (true) {
    try {
      line = reader.readLine();
    } 
    catch (IOException e) {
      e.printStackTrace();
      line = null;
    }
    if (line == null) {
      // Stop reading because of an error or file is empty
      break;
    } else {
      if (line.equals("")) {
        continue;
      }
      String[] data = line.split(",");

      if (data.length < 4) {
        continue;
      }

      // first car
      if (prevId == -1.0) {
        prevId = Float.parseFloat(data[0]);
      }
      // new car
      else if (prevId != float(data[0])) {
        prevId = Float.parseFloat(data[0]);
        prevIntervalIndex = -1;
      }

      // check if this is the closest time to the next interval to fill
      int intervalIndex = (Math.round(Float.parseFloat(data[1])) % 86400) / 60;
      if (intervalIndex > prevIntervalIndex) {
        float[] newData = new float[3];
        newData[0] = Float.parseFloat(data[0]);
        newData[1] = Float.parseFloat(data[2]);
        newData[2] = Float.parseFloat(data[3]);
        locations.get(intervalIndex).add(newData);

        // update prev ind
        prevIntervalIndex = intervalIndex;
      }
    }
  }

  return locations;
}
void setup() {
  size(800, 600);
  
  
  map =  new UnfoldingMap(this, new Microsoft.RoadProvider());
  MapUtils.createDefaultEventDispatcher(this, map);
  map.zoomAndPanTo(new Location(22.57356597f, 114.0544452f), 10);

  private_data = readFile("private_raw_p.txt");
  bus_data = readFile("bus_raw_p.txt");
  taxi_data = readFile("taxi_raw_p.txt");
  truck_data = readFile("truck_raw_p.txt");
  
  markerManager = map.getDefaultMarkerManager();
  frameRate(5);
}

void draw() {
  map.draw();
  markerManager.clearMarkers();
  
  if(count >= 288){
    return;
  }
  
  ArrayList<float[]> p_interval = private_data.get(count++);
  ArrayList<float[]> bus_interval = bus_data.get(count++);
  ArrayList<float[]> taxi_interval = taxi_data.get(count++);
  ArrayList<float[]> truck_interval = truck_data.get(count++);
  
  add_interval_to_map(p_interval, "private");
  add_interval_to_map(bus_interval, "bus");
  add_interval_to_map(taxi_interval, "taxi");
  add_interval_to_map(truck_interval, "truck");
  


}

void add_interval_to_map(ArrayList<float[]> interval, String type){
    for (float[] v : interval) {
    int vid = int(v[0]);
    float lon = v[1];
    float lat = v[2];
    
//System.out.println(vid);
    int one = 0; int two = 0; int three = 0;

    if (type.equals("private")){
      one = 255; two = 0; three = 0;      
    }
    
    if (type.equals("bus")){
      one = 0; two = 0; three = 255;      
    }
    
    if (type.equals("truck")){
      one = 0; two = 204; three = 0;    
    }
    
    if (type.equals("taxi")){
      one = 255; two = 255; three = 0;      
    }
    

    Location startLocation = new Location(lat, lon);
    SimplePointMarker startMarker = new SimplePointMarker(startLocation);
    
    
    

//    if(vid > 255){
//      three = 255;
//      vid -= 255;
//    }
//    else{
//      three = vid;
//      vid = 0;
//    }
//    
//    if(vid > 255){
//      two = 255;
//      vid -= 255;
//    }
//    else{
//      two = vid;
//      vid = 0;
//    }
//    
//    if(vid > 255){  
//      one = 255;
//      vid -= 255;
//    }
//    else{
//      one = vid;
//      vid = 0;
//    }
    

  
    markerManager.addMarker(startMarker);
    // startMarker.setStrokeWeight(3);
    startMarker.setRadius(3);
    startMarker.setColor(color(one, two, three));
  
    //startMarker.setColor(color(vid, 5, 100, vid*3));
    // startMarker.setStrokeColor(color(vid, vid, vid));


  }
}



    //Point
    //    Location startLocation = new Location(starty, startx);
    //    SimplePointMarker startMarker = new SimplePointMarker(startLocation);
    //    map.addMarkers(startMarker);
    //    startMarker.setStrokeWeight(1);
  //public void mouseMoved() {
  //    Marker hitMarker = map.getFirstHitMarker(mouseX, mouseY);
  //    if (hitMarker != null) {
  //        // Select current marker 
  //        hitMarker.setSelected(true);
  //        System.out.println(hitMarker.getLocation());
  //    } else {
  //        // Deselect all other markers
  //        for (Marker marker : map.getMarkers()) {
  //            marker.setSelected(false);
  //        }
  //    }
  //}
