# ShortestPath_NYCSubway

Project Summary:
New York subway system can be represented by a graph where a node is a station
and an edge is a path between two stations.
A traveler might look for a shortest path (which takes minimum time) from one
station, A to another station B.
You need to write a program which will take a graph of New York subway
system, a source station and a destination station. Your program will compute the
shortest path from stations A to station B.
You also need several other inputs which are: stoppage durations of each train at
every stations (take average stoppage duration of each train for every station),
running time from one station to another station of each train. Departure time and
arriving time of each train at every station. Finally, your transfer time from one
train to another (take average walking speed of a person). Remember, some
transfers take long time. You might need to walk several minutes to reach other
platform. These all represent factors or weights in the graph.
There are two types of trains in NY subway: local and express. Local trains stops
every station however, an express train does not stop at every stations.