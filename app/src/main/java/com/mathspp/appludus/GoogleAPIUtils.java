package com.mathspp.appludus;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/*  Use this class to handle requests to the Google APIs;
    Does not implement Exponential Backoff directly because it might hang the threads
 */
public class GoogleAPIUtils {
    private static final String LogTAG = GoogleAPIUtils.class.getSimpleName();
    /* Use these keys to access "time to" and "distance to" values */
    public static final String DISTANCE_VALUE_KEY = "dist_value";
    public static final String TIME_VALUE_KEY = "time_value";
    /* Use these keys to access "time to" and "distance to" texts */
    public static final String DISTANCE_TEXT_KEY = "dist_text";
    public static final String TIME_TEXT_KEY = "time_text";
    // if a request fails, wait at least 100ms to ask again
    public static final int MIN_REQUEST_WAIT_TIME = 100;
    // if all requests fail, never wait more than 1min to ask again
    public static final int MAX_REQUEST_WAIT_TIME = 60000;
    // multiplicative incrementer for Exponential Backoff
    public static final int EXPONENTIAL_BACKOFF_MULTIPLIER = 2;

    /*  Gets the distance and time between two locations;
        Returns a hash map with distance in meters and time in seconds;
            when the distance and time values are integers encoded as strings.
     */
    public static HashMap<String, String> getDistanceTo(LatLng from, LatLng to, String api_key) {
        HashMap<String, String> results = new HashMap<>();

        String baseUrl = "https://maps.googleapis.com/maps/api/distancematrix/";
        String outputFormat = "json";

        String origin = Double.toString(from.latitude) + "," + Double.toString(from.longitude);
        String destin = Double.toString(to.latitude) + "," + Double.toString(to.longitude);
        String language = "pt";

        String urlRequest = baseUrl + outputFormat + "?" +
                            "language=" + language +
                            "&origins=" + origin + "&destinations=" + destin +
                            "&key=" + api_key;

        boolean trying = true;
        int wait_time = MIN_REQUEST_WAIT_TIME;
        JSONObject data;
        String time_value="", time_text="", dist_value="", dist_text="";

        try {
            URL url = new URL(urlRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            input.close();
            reader.close();
            connection.disconnect();

            data = new JSONObject(stringBuilder.toString());
            /* follow https://developers.google.com/maps/documentation/distance-matrix/intro
                to parse the resulting JSON
             */
            JSONArray rows = data.getJSONArray("rows");
            JSONArray elements = ((JSONObject) rows.get(0)).getJSONArray("elements");
            JSONObject info = (JSONObject) elements.get(0);

            JSONObject duration = info.getJSONObject("duration");
            time_text = duration.getString("text");
            time_value = Integer.toString(duration.getInt("value"));
            JSONObject distance = info.getJSONObject("distance");
            dist_text = distance.getString("text");
            dist_value = Integer.toString(distance.getInt("value"));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        results.put(DISTANCE_VALUE_KEY, dist_value);
        results.put(DISTANCE_TEXT_KEY, dist_text);
        results.put(TIME_VALUE_KEY, time_value);
        results.put(TIME_TEXT_KEY, time_text);
        return results;
    }
}
