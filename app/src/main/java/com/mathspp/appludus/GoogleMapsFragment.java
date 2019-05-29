package com.mathspp.appludus;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mathspp.appludus.viewModels.LocationsViewModel;
import com.mathspp.appludus.viewModels.NotificationsViewModel;
import com.mathspp.appludus.viewModels.UserLocViewModel;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class GoogleMapsFragment extends Fragment implements
        OnMapReadyCallback {

    public static final String SINGLE_MARK_MODE = "single_mark";
    public static final String MULTI_MARK_MODE = "multi_mark";
    public static final String DEFAULT_MARK_MODE = SINGLE_MARK_MODE;

    private final String LogTAG = GoogleMapsFragment.class.getSimpleName();
    private final int DEFAULT_ZOOM = 15;
    private final double DEFAULT_LAT = 38.722501;
    private final double DEFAULT_LON = -9.139435;
    private final String DEFAULT_LOCATION_NAME = "Lisbon";

    private LocationsViewModel locationsViewModel;
    private UserLocViewModel userLocViewModel;
    private NotificationsViewModel notificationsViewModel;
    private GoogleMap mGoogleMap;
    private MapView mMapView;

    public GoogleMapsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LogTAG, "onCreate");

        locationsViewModel = ViewModelProviders.of(getActivity()).get(LocationsViewModel.class);
        userLocViewModel = ViewModelProviders.of(getActivity()).get(UserLocViewModel.class);
        notificationsViewModel = ViewModelProviders.of(getActivity()).get(NotificationsViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LogTAG, "onCreateView");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_google_maps, container, false);

        mMapView = v.findViewById(R.id.mapview);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LogTAG, "Unregistering the observer for the map");
        locationsViewModel.removeObservers(this);
        userLocViewModel.removeObservers(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(LogTAG, "map is ready");
        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);

        /* ensure we have all the view models needed */
        if (locationsViewModel == null) {
            locationsViewModel = ViewModelProviders.of(getActivity()).get(LocationsViewModel.class);
        }
        if (notificationsViewModel == null) {
            notificationsViewModel = ViewModelProviders.of(getActivity()).get(NotificationsViewModel.class);
        }
        // depending on the mode, update the markers accordingly
        notificationsViewModel.getMapMarkerMode().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                Log.d(LogTAG, "Change in 'Marker Mode' observed " + s);
                if (mGoogleMap == null) return;
                if (s == null) s = GoogleMapsFragment.DEFAULT_MARK_MODE;
                ArrayList<Pair<String, String>> selection = new ArrayList<>();
                switch (s) {
                    case GoogleMapsFragment.MULTI_MARK_MODE:
                        List<Pair<String, String>> names = locationsViewModel.getMultiSelected().getValue();
                        selection.addAll(names);
                        prepareLatLngObjectsToMark(selection);
                        break;
                    case GoogleMapsFragment.SINGLE_MARK_MODE:
                        Pair<String, String> pair = locationsViewModel.getSelected().getValue();
                        selection.add(pair);
                        prepareLatLngObjectsToMark(selection);
                    default:
                        break;
                }
            }
        });
        if (userLocViewModel == null) {
            userLocViewModel = ViewModelProviders.of(getActivity()).get(UserLocViewModel.class);
        }
        userLocViewModel.getLocationPermissionGranted().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean permission) {
                Log.d(LogTAG, "We observed the location permission was changed to " + permission);
                updateLocationUI();
            }
        });
    }

    public void updateLocationUI() {
        if (mGoogleMap == null) { return; }
        try {
            if (userLocViewModel.getLocationPermissionGranted().getValue() != null &&
                    userLocViewModel.getLocationPermissionGranted().getValue()) {
                /* location permission was granted */
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        Log.d(LogTAG, "onMyLocationButtonClick");
                        ((MainActivity) getActivity()).mUserLocationSettingsUtils.getDeviceLocation();
                        if (userLocViewModel.getLastKnownLocation().getValue() == null &&
                                !userLocViewModel.getLocationSettingsSetupResult().getValue()) {
                            ((MainActivity) getActivity()).mUserLocationSettingsUtils.setupLocationSettings();
                        }
                        return false;
                    }
                });
            } else {
                mGoogleMap.setMyLocationEnabled(false);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                ((MainActivity) getActivity()).getLocationPermission();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void setPositionOnMap(LatLng latLng, String name) {
        if (mGoogleMap == null) { return; }
        Log.d(LogTAG, "Setting position from LatLng object " + latLng + " with name " + name);
        mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(name));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }

    public void setDefaultPositionOnMap() {
        LatLng latLng = new LatLng(DEFAULT_LAT, DEFAULT_LON);
        setPositionOnMap(latLng, DEFAULT_LOCATION_NAME);
    }

    public void setPositionsOnMap(List<LatLng> latLngs, List<String> names) {
        if (mGoogleMap == null) { return; }

        if (latLngs == null || names == null || latLngs.size() == 0 || names.size() == 0 ||
                latLngs.size() != names.size()) {
            setDefaultPositionOnMap();
            return;
        } else if (latLngs.size() == 1) {
            setPositionOnMap(latLngs.get(0), names.get(0));
            return;
        }

        //  use this to move the map in a way that shows all markers
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (int i = 0; i < latLngs.size(); ++i) {
            LatLng latLng = latLngs.get(i);
            String name = names.get(i);
            bounds.include(latLng);
            mGoogleMap.addMarker(new MarkerOptions().position(latLng).title(name));
        }
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 60));
    }

    private void prepareLatLngObjectsToMark(List<Pair<String, String>> selectionPairs) {
        JSONObject data = locationsViewModel.getLocationsJSONdata().getValue();
        ArrayList<LatLng> latLngs = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();

        if (selectionPairs == null || selectionPairs.size() == 0 || data == null) {
            // set default position on the map
            setDefaultPositionOnMap();
        } else {
            for (Pair<String, String> pair : selectionPairs) {
                LatLng latLng = DataUtils.getLatLngFromName(data, pair.first, pair.second);
                if (latLng != null) {
                    latLngs.add(latLng);
                    names.add(pair.second);
                }
            }
            setPositionsOnMap(latLngs, names);
        }
    }

    /*  Series of methods that we have to call explicitly for the mapview */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView == null) { return; }
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView == null) { return; }
        mMapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView == null) { return; }
        mMapView.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMapView == null) { return; }
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMapView == null) { return; }
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMapView == null) { return; }
        mMapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mMapView == null) { return; }
        mMapView.onLowMemory();
    }
}
