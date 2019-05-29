package com.mathspp.appludus.viewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;

public class NotificationsViewModel extends AndroidViewModel {
    private MutableLiveData<Boolean> visitedStatusChanged = new MutableLiveData<>();
    private MutableLiveData<Boolean> insideContextualMultiSelection = new MutableLiveData<>();
    private MutableLiveData<String> mapMarkerMode = new MutableLiveData<>();

    public NotificationsViewModel(Application application) {
        super(application);
    }

    public MutableLiveData<Boolean> getVisitedStatusChanged() {
        return visitedStatusChanged;
    }

    public void setVisitedStatusChanged(boolean b) {
        visitedStatusChanged.setValue(b);
    }

    public MutableLiveData<Boolean> getInsideContextualMultiSelection() {
        return insideContextualMultiSelection;
    }

    public void setInsideContextualMultiSelection(boolean b) {
        insideContextualMultiSelection.setValue(b);
    }

    public MutableLiveData<String> getMapMarkerMode() {
        return mapMarkerMode;
    }

    public void setMapMarkerMode(String string) {
        mapMarkerMode.setValue(string);
    }

    public void removeObservers(LifecycleOwner owner) {
        visitedStatusChanged.removeObservers(owner);
        insideContextualMultiSelection.removeObservers(owner);
        mapMarkerMode.removeObservers(owner);
    }
}
