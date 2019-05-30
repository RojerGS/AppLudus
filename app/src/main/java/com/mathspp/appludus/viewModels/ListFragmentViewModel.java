package com.mathspp.appludus.viewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.support.v7.view.ActionMode;

import java.util.List;

public class ListFragmentViewModel extends AndroidViewModel {
    private final MutableLiveData<ActionMode> actionMode = new MutableLiveData<>();
    private final MutableLiveData<List<Boolean>> visibleContainers = new MutableLiveData<>();

    public ListFragmentViewModel(Application app) {
        super(app);
    }

    public MutableLiveData<ActionMode> getActionMode() {
        return actionMode;
    }

    public void setActionMode(ActionMode mode) {
        actionMode.setValue(mode);
    }

    public MutableLiveData<List<Boolean>> getVisibleContainers() {
        return visibleContainers;
    }

    public void setVisibleContainers(List<Boolean> visibilities) {
        visibleContainers.setValue(visibilities);
    }

    public void removeObservers(LifecycleOwner owner) {
        getActionMode().removeObservers(owner);
        getVisibleContainers().removeObservers(owner);
    }
}