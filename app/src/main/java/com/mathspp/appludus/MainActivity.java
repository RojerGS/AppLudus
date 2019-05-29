package com.mathspp.appludus;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mathspp.appludus.interfaces.TabChangeRequestHandler;
import com.mathspp.appludus.viewModels.LocationsViewModel;
import com.mathspp.appludus.viewModels.NotificationsViewModel;
import com.mathspp.appludus.viewModels.UserLocViewModel;


public class MainActivity extends AppCompatActivity implements
        TabChangeRequestHandler,
        InfoFragment.OnInfoFragmentInteractionListener {

    private final String LogTAG = MainActivity.class.getSimpleName();

    private final int LIST_FRAGMENT_TAB_POSITION = 0;
    private final int INFO_FRAGMENT_TAB_POSITION = 1;
    private final int GOOGLE_MAPS_FRAGMENT_TAB_POSITION = 2;
    private ListFragment mListFragment;
    private InfoFragment mInfoFragment;
    private GoogleMapsFragment mGoogleMapsFragment;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    protected UserLocationSettingsUtils mUserLocationSettingsUtils;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final int REQUEST_CHECK_SETTINGS_CODE = 2;
    /*  as per the docs, 5s fastest update interval is appropriate
        for displaying real-time location
     */
    private final int LOCATION_UPDATE_INTERVAL = 10000;
    private final int LOCATION_UPDATE_FASTEST_INTERVAL = 5000;

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private TabAdapter mTabAdapter;
    private LocationsViewModel locationsViewModel;
    private UserLocViewModel userLocViewModel;
    private NotificationsViewModel notificationsViewModel;

    private VisitedStatusHandler mVisitedStatusHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViewModels();

        mVisitedStatusHandler = new VisitedStatusHandler(
                getSharedPreferences(getString(R.string.visited_status_filename),
                        Context.MODE_PRIVATE));

        setupLocationPermissions();
        setupTabs();
    }

    private void setupViewModels() {
        // get the View Model and hook the observers to the data
        locationsViewModel = ViewModelProviders.of(this).get(LocationsViewModel.class);
        userLocViewModel = ViewModelProviders.of(this).get(UserLocViewModel.class);
        notificationsViewModel = ViewModelProviders.of(this).get(NotificationsViewModel.class);
        // set the default view mode for the map
    }

    private void setupLocationPermissions() {
        // the first time this activity is created, check for the location permission
        mUserLocationSettingsUtils = new UserLocationSettingsUtils();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (userLocViewModel.getLocationPermissionGranted().getValue() == null) {
            Log.d(LogTAG, "location permission is null atm");
            getLocationPermission();
        }
        // whenever we get the permission, ask for the location
        userLocViewModel.getLocationPermissionGranted().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean permissionGranted) {
                if (permissionGranted) {
                    mUserLocationSettingsUtils.getDeviceLocation();
                }
            }
        });
        // after we have tried to setup the location request settings, ask for updates
        userLocViewModel.getLocationSettingsSetupResult().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean setupResult) {
                Log.d(LogTAG, "The setup result was "+setupResult+"; asking for updates");
            }
        });
        if (userLocViewModel.getLocationSettingsSetupResult().getValue() == null) {
            Log.d(LogTAG, "No setup result found, going to ask for it");
            mUserLocationSettingsUtils.setupLocationSettings();
        }
        // always ask for location updates
        mUserLocationSettingsUtils.startLocationUpdates();
    }

    private void setupTabs() {
        // populate the TabAdapter with the fragments I need
        mTabAdapter = new TabAdapter(this.getSupportFragmentManager());
        mListFragment = new ListFragment();
        mTabAdapter.addFragment(mListFragment, getString(R.string.tab_loc_list_text));
        mInfoFragment = new InfoFragment();
        mTabAdapter.addFragment(mInfoFragment, getString(R.string.tab_loc_info_text));
        mGoogleMapsFragment = new GoogleMapsFragment();
        mTabAdapter.addFragment(mGoogleMapsFragment, getString(R.string.tab_map_text));
        // hook the TabAdapter and the ViewPager together
        mViewPager = findViewById(R.id.viewpager);
        mTabLayout = findViewById(R.id.tablayout);
        mViewPager.setAdapter(mTabAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mTabLayout.setupWithViewPager(mViewPager);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(LogTAG, "in onTabSelected");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Log.d(LogTAG, "in onTabUnselected: " + tab.getPosition());
                Fragment f = mTabAdapter.getFragment(tab.getPosition());
                switch (tab.getPosition()) {
                    case LIST_FRAGMENT_TAB_POSITION:
                        if (f != null) ((ListFragment) f).onTabUnselected();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_start_multi_selection:
                showListTab();
                Fragment fragment = mTabAdapter.getFragment(LIST_FRAGMENT_TAB_POSITION);
                ((ListFragment) fragment).triggerNewContextualMultiSelection();
                return true;
            case R.id.main_menu_help:
                int helpCase;
                switch (mTabLayout.getSelectedTabPosition()) {
                    case LIST_FRAGMENT_TAB_POSITION:
                        helpCase = HelpDialogFragment.LIST_HELP;
                        break;
                    case INFO_FRAGMENT_TAB_POSITION:
                        helpCase = HelpDialogFragment.INFO_HELP;
                        break;
                    case GOOGLE_MAPS_FRAGMENT_TAB_POSITION:
                        helpCase = HelpDialogFragment.MAP_HELP;
                        break;
                    default:
                        helpCase = HelpDialogFragment.DEFAULT_HELP;
                        break;
                }
                HelpDialogFragment.newInstance(helpCase).show(getSupportFragmentManager(),
                                                    getString(R.string.dialog_tag));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUserLocationSettingsUtils.stopLocationUpdates();
    }

    /* This method communicates with the InfoFragment */
    // (TODO) stop assuming this method is just a stub to "show on map"
    @Override
    public void showSelectedOnMap() {
        showMapTab();
    }

    @Override
    public void showInfoTab() {
        mTabLayout.getTabAt(INFO_FRAGMENT_TAB_POSITION).select();
    }

    @Override
    public void showMapTab() {
        mTabLayout.getTabAt(GOOGLE_MAPS_FRAGMENT_TAB_POSITION).select();
    }

    @Override
    public void showListTab() {
        mTabLayout.getTabAt(LIST_FRAGMENT_TAB_POSITION).select();
    }

    protected void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            userLocViewModel.setLocationPermissionGranted(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        userLocViewModel.setLocationPermissionGranted(false);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    userLocViewModel.setLocationPermissionGranted(true);
                    mUserLocationSettingsUtils.getDeviceLocation();
                }
            }
        }
    }

    protected class UserLocationSettingsUtils {
        private final String LogTAG = UserLocationSettingsUtils.class.getSimpleName();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(LogTAG, "null Location update received");
                    return;
                }
                Log.d(LogTAG, "non-null Location update received");
                // we got a bunch of locations, update with the latest one
                userLocViewModel.setLastKnownLocation(locationResult.getLastLocation());
            }
        };

        public void getDeviceLocation() {
            try {
                if (userLocViewModel.getLocationPermissionGranted().getValue()) {
                    Task locationResult = mFusedLocationProviderClient.getLastLocation();
                    // the task is going to run on the background; when it is done, run this:
                    locationResult.addOnCompleteListener(MainActivity.this, new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task.isSuccessful()) {
                                userLocViewModel.setLastKnownLocation((Location) task.getResult());
                            }
                        }
                    });
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        protected void setupLocationSettings() {
            LocationRequest request = createLocationRequest();

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(request);

            Task<LocationSettingsResponse> result =
                    LocationServices.getSettingsClient(MainActivity.this).checkLocationSettings(builder.build());

            // setup a listener for when the task finishes
            result.addOnSuccessListener(MainActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    // all settings are fine, start requesting updates here
                    Log.d(LogTAG, "location request successfully handled");
                    userLocViewModel.setLocationSettingsSetupResult(true);
                }
            });

            result.addOnFailureListener(MainActivity.this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(LogTAG, "location request failed;");
                    // check if we can still recover from the failure
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            Log.d(LogTAG, "Going to try and solve it");
                            resolvable.startResolutionForResult(MainActivity.this,
                                    REQUEST_CHECK_SETTINGS_CODE);
                            /*  the user is prompted to enable Google location;
                                after acting upon the request, onActivityResult will be called */
                        } catch (IntentSender.SendIntentException sendEx) {
                            // ignore this, I can't use this for any good
                            sendEx.printStackTrace();
                            userLocViewModel.setLocationSettingsSetupResult(false);
                        }
                    } else {
                        userLocViewModel.setLocationSettingsSetupResult(false);
                    }
                }
            });
        }

        protected void startLocationUpdates() {
            LocationRequest request = createLocationRequest();
            try {
                mFusedLocationProviderClient.requestLocationUpdates(request, locationCallback, null);
            } catch (SecurityException e) {
                Log.d(LogTAG, "Could not request for location updates");
            }
        }

        protected void stopLocationUpdates() {
            mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        protected LocationRequest createLocationRequest() {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
            locationRequest.setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL);
        /*  the app has as targets, users who are walking in the city of Lisbon;
            if you are walking, you need an accurate location, otherwise it can
            look like you are in the wrong street
            (esp. in some parts of Lisbon...)
         */
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            return locationRequest;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        userLocViewModel.setLocationSettingsSetupResult(true);
                        break;
                    default:
                        userLocViewModel.setLocationSettingsSetupResult(false);
                        break;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    // Utility getter
    public VisitedStatusHandler getVisitedStatusHandler() {
        return mVisitedStatusHandler;
    }
}