package com.mathspp.appludus;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*  Use this class to deal with the raw json data
    Assumes the toplevel object is a list of categories, each category mapping
        to a sub-jsonObject locationJSONObject;
    These, in turn, have several keys where
        each top level key is the name of the location and the sub-object
        contains more information on the location, like the unique ID and
        the GPS coordinates of the place
 */
public class DataUtils {
    public static final String LOCATION_ID_HEADER = "id";
    public static final String LOCATION_LAT_HEADER = "lat";
    public static final String LOCATION_LON_HEADER = "lon";
    public static final String LOCATION_INFOFILE_HEADER = "info_filename";

    public static List<String> getLocationCategories(JSONObject jsonObject) {
        List<String> categories = new ArrayList<>();
        Iterator<String> categoriesIterator = jsonObject.keys();
        while (categoriesIterator.hasNext()) {
            categories.add(categoriesIterator.next());
        }
        return categories;
    }

    public static List<String> getLocationNames(JSONObject jsonObject, String category) {
        JSONObject categoryData = getCategoryObject(jsonObject, category);
        if (categoryData == null) return null;

        List<String> names = new ArrayList<>();
        Iterator<String> namesIterator = categoryData.keys();
        while (namesIterator.hasNext()) {
            names.add(namesIterator.next());
        }
        return names;
    }

    public static List<String> getNonVisitedLocationNames(JSONObject jsonObject, String category, VisitedStatusHandler handler) {
        JSONObject categoryData = getCategoryObject(jsonObject, category);
        if (categoryData == null) { return null; }

        List<String> names = new ArrayList<>();
        Iterator<String> namesIterator = categoryData.keys();
        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            if (!handler.getVisitedStatus(name)) {
                names.add(name);
            }
        }
        return names;
    }

    public static LatLng getLatLngFromName(JSONObject jsonObject, String category, String locName) {
        JSONObject categoryData = getCategoryObject(jsonObject, category);
        if (categoryData == null) return null;

        LatLng latLng = null;
        try {
            JSONObject locationInfo = categoryData.getJSONObject(locName);

            double lat = locationInfo.getDouble(LOCATION_LAT_HEADER);
            double lon = locationInfo.getDouble(LOCATION_LON_HEADER);

            latLng = new LatLng(lat, lon);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return latLng;
    }

    public static String getInfoFromName(JSONObject jsonObject, String category, Context cont, String name) {
        JSONObject categoryData = getCategoryObject(jsonObject, category);
        if (categoryData == null) return "";

        String info;
        try {
            String filename = categoryData.getJSONObject(name).getString(LOCATION_INFOFILE_HEADER);
            int fileID = cont.getResources().getIdentifier(filename, "raw", cont.getPackageName());
            InputStream input = cont.getResources().openRawResource(fileID);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len;
            // read the file with the given buffer
            try{
                while ((len = input.read(buff)) != -1) {
                    output.write(buff, 0, len);
                }
                output.close();
                input.close();
                info = output.toString();
            } catch (IOException e) {
                e.printStackTrace();
                info = "";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            info = "";
        }
        return info;
    }

    // (TODO) check we can delete this, and delete it
    public static int getIdFromName(JSONObject jsonObject, String locName) {
        int id = -1;
        try {
            JSONObject locationInfo = jsonObject.getJSONObject(locName);
            id = locationInfo.getInt(LOCATION_ID_HEADER);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }

    private static JSONObject getCategoryObject(JSONObject data, String category) {
        JSONObject categoryData;
        try {
            categoryData = data.getJSONObject(category);
        } catch (JSONException e) {
            return null;
        }
        return categoryData;
    }
}
