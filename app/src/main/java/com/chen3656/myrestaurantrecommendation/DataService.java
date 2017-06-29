package com.chen3656.myrestaurantrecommendation;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shilinchen on 6/20/17.
 */

public class DataService {
    /**
     * Get nearby restaurants through Yelp API.
     */
    LruCache<String, Bitmap> bitmapCache;
    private Context mContext;
    private double lat;
    private double lng;
    /**
     * Constructor.
     */
    public DataService(Context context) {
        mContext = context;
    }


    public DataService() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception.
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        Log.e("Cache size", Integer.toString(cacheSize));
        bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public List<Restaurant> getNearbyRestaurants(Context context, Activity activity) {
        YelpApi yelp = new YelpApi();
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( activity, new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    2);
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null) {
            // The minimum distance to change Updates in meters
            final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

            // The minimum time between updates in milliseconds
            final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                lat= location.getLatitude();
                lng = location.getLongitude();
            }
        }

        String jsonResponse = yelp.searchForBusinessesByLocation("dinner", lat, lng, 20);
        return parseResponse(jsonResponse);
    }

    /**
     * Parse the JSON response returned by Yelp API.
     */
    private List<Restaurant> parseResponse(String jsonResponse)  {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray businesses = json.getJSONArray("businesses");
            ArrayList<Restaurant> restaurants = new ArrayList<Restaurant>();
            for (int i = 0; i < businesses.length(); i++) {
                JSONObject business = businesses.getJSONObject(i);

                //Parse restaurant information
                if (business != null) {
                    String name = business.getString("name");
                    String type = ((JSONArray) business.get("categories")).
                            getJSONArray(0).get(0).toString();

                    JSONObject location = (JSONObject) business.get("location");
                    JSONObject coordinate = (JSONObject) location.get("coordinate");
                    double lat = coordinate.getDouble("latitude");
                    double lng = coordinate.getDouble("longitude");
                    String address =
                            ((JSONArray) location.get("display_address")).get(0).toString();

                    // Download the image.
                    Bitmap thumbnail =  getBitmapFromURL(business.getString("image_url"));
                    Bitmap rating = getBitmapFromURL(business.getString("rating_img_url"));
                    restaurants.add(new Restaurant(name, address, type, lat, lng, thumbnail,
                            rating));
                }
            }
            return restaurants;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Download an Image from the given URL, then decodes and returns a Bitmap object.
     */
    public Bitmap getBitmapFromURL(String imageUrl) {
        Bitmap bitmap = null;
        if (bitmapCache != null) {
            bitmap = bitmapCache.get(imageUrl);
        }

        if (bitmap == null) {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
                if (bitmapCache != null) {
                    bitmapCache.put(imageUrl, bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Error: ", e.getMessage().toString());
            }
        }
        return bitmap;
    }



}
