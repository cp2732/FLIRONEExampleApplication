package com.flir.flironeexampleapplication.helper;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationProvider implements LocationListener {
    private static final String TAG = LocationProvider.class.getSimpleName();
    private static LocationProvider instance;
    private static LocationManager locationManager;
    private static String provider;
    private static Location mostRecentLocation;

    //Default private constructor for singleton class
    private LocationProvider() {
        Log.d(TAG, "Made new LocationProvider");
    }

    /**
     * Lazy load the event bus
     *
     * @return Bus Singleton EventBus
     */
    public static LocationProvider getInstance() {
        if (instance == null) {
            synchronized (LocationProvider.class) {
                if (instance == null)
                    instance = new LocationProvider();
            }
        }
        return instance;
    }

    /** Called when the activity is first created. */
    public void initializeLocation(LocationManager manager) {

        // Get the location manager
        locationManager = manager;
        // Define the criteria how to select the location provider (default is used here)
        Criteria criteria = new Criteria();
        //get the best provider (GPS or Network)
        provider = locationManager.getBestProvider(criteria, false);
        Log.e(TAG, "Provider is " + provider);

        //if an immediate location is required, getLastKnownLocation is faster than requestLocationUpdates
        if (provider != null) {
            try {
                mostRecentLocation = locationManager.getLastKnownLocation(provider);
            }
            catch (SecurityException e) {
                mostRecentLocation = null;
                Log.w(TAG, "General Android Location permission is off. Cannot get last known location.");
            }
        }
        else
            Log.w(TAG, "App specific Location permission is off. Cannot set initial location.");

        // Initialize the location fields
        if (mostRecentLocation != null) {
            onLocationChanged(mostRecentLocation);
        }
        else {
            Log.w(TAG, "Cannot initially set most recent location! All required permissions may not be enabled.");
        }
    }

    public void requestUpdates() {
        if (locationManager != null) {
            try {
                if (provider != null) {
                    //a new location update will be received every 30 seconds
                    locationManager.requestLocationUpdates(provider, 30 * 1000, 0, this);
                    Log.d(TAG, "Automatic location updates was requested successfully.");
                }
                else
                    Log.w(TAG, "App specific Location permission is off. Cannot request location updates.");
            }
            catch (SecurityException e) {
                mostRecentLocation = null;
                Log.w(TAG, "General Android Location permission is off. Cannot request location updates.");
            }
        }
        else
            Log.w(TAG, "Cannot request location updates because no activity has registered a location manager yet.");
    }

    public void stopUpdates() {
        if (locationManager != null) {
            try {
                if (provider != null) {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.removeUpdates(this);
                        Log.d(TAG, "Removal from location updates was successful.");
                    }
                    else
                        Log.d(TAG, "The location provider was never enabled. Skipping unregister from updates...");
                }
                else
                    Log.w(TAG, "App specific Location permission is off. Skipping removal from location updates.");
            }
            catch (SecurityException e) {
                mostRecentLocation = null;
                Log.w(TAG, "Locations permissions not enabled yet. Skipping removal from location updates.");
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //setLatitude((int) (location.getLatitude()));
        //setLongitude((int) (location.getLongitude()));
        mostRecentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Enabled new provider " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Disabled new provider " + provider);
    }

    public Location getLocation() {
        return mostRecentLocation;
    }
}