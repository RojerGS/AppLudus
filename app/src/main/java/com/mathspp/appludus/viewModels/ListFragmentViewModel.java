package com.mathspp.appludus.viewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.v7.view.ActionMode;

public class ListFragmentViewModel extends AndroidViewModel {
    private final MutableLiveData<ActionMode> actionMode = new MutableLiveData<>();

    public ListFragmentViewModel(Application app) {
        super(app);
    }

    public MutableLiveData<ActionMode> getActionMode() {
        return actionMode;
    }

    public void setActionMode(ActionMode mode) {
        actionMode.setValue(mode);
    }
}