#include <iostream>
#include "csv.h"

int main(int argc, char *argv[]){
  if(argc < 2){
    std::cout << "Error: not enough arguments";
    return 1;
  }
  char *filename = argv[1];

  io::CSVReader<4> in(filename);
  in.set_header("id", "time_of_day", "lon", "lat");
  int id;
  int time_of_day;
  float lon;
  float lat;
  while(true){
    bool has_line = in.read_row(id, time_of_day, lon, lat);
    if(!has_line){
      break;
    }

    //std::cout << id << ' ' << time_of_day << ' ' << lon << ' ' << lat << ' ' << '\n';
  }
}
