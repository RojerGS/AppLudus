package com.mathspp.appludus.viewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.support.v4.util.Pair;

public class InfoFragmentViewModel extends AndroidViewModel {
    private final MutableLiveData<String> distanceToLocationStr = new MutableLiveData<>();
    private final MutableLiveData<String> timeToLocationStr = new MutableLiveData<>();
    private final MutableLiveData<Pair<String, String>> lastPairUsed = new MutableLiveData<>();

    public InfoFragmentViewModel(Application application) {
        super(application);
    }

    public void postLastPairUsed(Pair<String, String> pair) {
        lastPairUsed.postValue(pair);
    }

    public MutableLiveData<Pair<String, String>> getLastPairUsed() {
        return lastPairUsed;
    }

    public void postTimeToLocationStr(String timeToLocation) {
        timeToLocationStr.postValue(timeToLocation);
    }

    public MutableLiveData<String> getTimeToLocationStr() {
        return timeToLocationStr;
    }

    public void postDistanceToLocationStr(String distanceToLocation) {
        distanceToLocationStr.postValue(distanceToLocation);
    }

    public MutableLiveData<String> getDistanceToLocationStr() {
        return distanceToLocationStr;
    }

    public void removeObservers(LifecycleOwner lifecycleOwner) {
        distanceToLocationStr.removeObservers(lifecycleOwner);
        timeToLocationStr.removeObservers(lifecycleOwner);
        lastPairUsed.removeObservers(lifecycleOwner);
    }
}
