package com.mathspp.appludus.viewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.location.Location;

public class UserLocViewModel extends AndroidViewModel {
    private final String LogTAG = UserLocViewModel.class.getSimpleName();

    private final MutableLiveData<Boolean> locationPermissionGranted = new MutableLiveData<>();
    private final MutableLiveData<Location> lastKnownLocation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> locationSettingsSetupResult = new MutableLiveData<>();

    public UserLocViewModel(Application application) {
        super(application);
    }

    public MutableLiveData<Boolean> getLocationPermissionGranted() {
        return locationPermissionGranted;
    }

    public MutableLiveData<Location> getLastKnownLocation() {
        return lastKnownLocation;
    }

    public MutableLiveData<Boolean> getLocationSettingsSetupResult() {
        return locationSettingsSetupResult;
    }

    public void setLocationPermissionGranted(boolean permissionGranted) {
        locationPermissionGranted.setValue(permissionGranted);
    }

    public void setLastKnownLocation(Location loc) {
        lastKnownLocation.setValue(loc);
    }

    public void setLocationSettingsSetupResult(boolean setupResult) {
        locationSettingsSetupResult.setValue(setupResult);
    }

    public void removeObservers(LifecycleOwner lifecycleOwner) {
        getLastKnownLocation().removeObservers(lifecycleOwner);
        getLocationPermissionGranted().removeObservers(lifecycleOwner);
        getLocationSettingsSetupResult().removeObservers(lifecycleOwner);
    }
}
