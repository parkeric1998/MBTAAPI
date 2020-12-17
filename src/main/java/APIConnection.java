import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class APIConnection {
    static Gson gson = new Gson();

    static HashMap<JsonElement,JsonElement> routesIdToName = new HashMap<JsonElement,JsonElement>();

    static HashMap<JsonElement,Integer> routesIdToNumStops = new HashMap<JsonElement,Integer>();

    static List<JsonElement> longNameList = new ArrayList<JsonElement>();

    static HashMap<JsonElement, ArrayList<JsonElement>> stopsByRoutes = new HashMap<JsonElement, ArrayList<JsonElement>>();

    /**
     * Gets subway Route data from MBTA API for subwayRoute types:0,1
     * @return JsonArray of subway routes Data
     * @throws IOException
     * @throws InterruptedException
     */

    public static JsonArray getSubwayData()throws IOException, InterruptedException{
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-v3.mbta.com/routes?filter[type]=0,1"))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        String subwayData = response.body();
        JsonElement subwayRoutesData = gson.fromJson(getSubwayData(), JsonObject.class).get("data");
        JsonArray jsonRoutesData = gson.fromJson(subwayRoutesData, JsonArray.class);
        return jsonRoutesData;
    }

    public static void main(String[] args) throws IOException, InterruptedException{
        getLongNameList(getSubwayData());

        getStopsByRoute();

        stopsWithTwoPlusRoutes();
    }

    /**
     * Gets the list of the Long Names for Subway Routes type 0,1
     * @param jsonRoutesData the JsonArray containing all the information for each Subway Route
     */
    public static void getLongNameList(JsonArray jsonRoutesData){
        System.out.println("Subway routes by long name:");
        for (JsonElement routes: jsonRoutesData) {
            JsonElement attributes = routes.getAsJsonObject().get("attributes");
            JsonElement longName = attributes.getAsJsonObject().get("long_name");
            JsonElement routeId = routes.getAsJsonObject().get("id");

            routesIdToName.put(routeId,longName);
            longNameList.add(attributes);
            System.out.println(longName);
        }
    }

    /**
     * Helper method to add elements to hashmap's list value to correspond Stops with their routes
     * @param mapKey The Stop
     * @param route The route
     */

    public static void addToList(JsonElement mapKey, JsonElement route) {
        ArrayList<JsonElement> itemsList = stopsByRoutes.get(mapKey);
        // if list does not exist create it
        if(itemsList == null) {
            itemsList = new ArrayList<JsonElement>();
            itemsList.add(route);
            stopsByRoutes.put(mapKey, itemsList);
        } else {
            // add if item is not already in list
            if(!itemsList.contains(route)) itemsList.add(route);
        }
    }

    /**
     * Pulls Stop data from the API by the routeId
     * @param routeIdByRoute
     * @return JsonArray representing the Stops based off the routeId
     * @throws IOException
     * @throws InterruptedException
     */

    public static JsonArray apiStopsByRoute(Map.Entry<JsonElement, JsonElement> routeIdByRoute)throws IOException, InterruptedException{
        JsonElement routeId = routeIdByRoute.getKey();
        HttpClient client = HttpClient.newHttpClient();
        String apiUrl = "https://api-v3.mbta.com/stops?filter[route]=" +routeId.getAsString();
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();
        HttpResponse<String> response2 = client.send(request2,
                HttpResponse.BodyHandlers.ofString());
        String stopsByRoute = response2.body();
        JsonElement stops = gson.fromJson(stopsByRoute, JsonObject.class).get("data");
        JsonArray jsonArrayStops = gson.fromJson(stops, JsonArray.class);
        return jsonArrayStops;
    }

    /**
     * Gets the StopsByRoute and also calculates the min and max stops
     * @throws IOException
     * @throws InterruptedException
     */

    public static void getStopsByRoute() throws IOException, InterruptedException{
        String maxName = "";
        String minName = "";
        Integer max = Integer.MIN_VALUE;
        Integer min = Integer.MAX_VALUE;
        for (Map.Entry<JsonElement, JsonElement> routeIdByRoute : routesIdToName.entrySet()){
            JsonElement routeId = routeIdByRoute.getKey();
            String routeName = routeIdByRoute.getValue().getAsString();
            JsonArray jsonArrayStops = apiStopsByRoute(routeIdByRoute);
            Integer numStops = jsonArrayStops.size();
            if(numStops > max){
                max = numStops;
                maxName = routeName;
            }
            if (numStops < min){
                min = numStops;
                minName = routeName;
            }
            routesIdToNumStops.put(routeId,numStops);
            stopsByRoutes(jsonArrayStops,routeId);

        }
        // Question 2 part 1&2
        System.out.println("\nSubway route with most stops: " + maxName + ": " + max);
        System.out.println("Subway route with most stops: "+ minName + ": " + min);

    }

    /**
     * Iterates through the stops by the routes and appends the route to the stop->routeList hashmap
     * @param jsonArrayStops JsonArray of Stops
     * @param key Route that is connected to the stop
     */
    public static void stopsByRoutes(JsonArray jsonArrayStops, JsonElement key){
        // Question 2part 3
        // Mapping Stops names to Routes
        for (JsonElement obj: jsonArrayStops){
            JsonElement ele = obj.getAsJsonObject().get("attributes");
            JsonElement name = ele.getAsJsonObject().get("name");
            addToList(name,key);
        }
    }

    /**
     * Prints out the stops with two or more routes connected with it
     */
    public static void stopsWithTwoPlusRoutes(){
        System.out.print("\nStops connecting two or more routes:");
        for(Map.Entry<JsonElement, ArrayList<JsonElement>> stop : stopsByRoutes.entrySet()){
            String key = stop.getKey().getAsString();
            ArrayList<JsonElement> value = stop.getValue();
            if(value.size()>=2){
                System.out.println(key + value);
            }
        }
    }

}
