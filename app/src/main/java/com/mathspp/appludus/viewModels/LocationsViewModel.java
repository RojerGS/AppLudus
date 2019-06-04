package com.mathspp.appludus.viewModels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mathspp.appludus.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class LocationsViewModel extends AndroidViewModel {
    private final String LogTAG = LocationsViewModel.class.getSimpleName();

    /* Pair<category, locationName> */
    private final MutableLiveData<Pair<String, String>> selected = new MutableLiveData<>();
    private final MutableLiveData<List<Pair<String, String>>> multiSelected = new MutableLiveData<>();
    private final MutableLiveData<JSONObject> locationsJSONdata = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showingVisited = new MutableLiveData<>();

    public LocationsViewModel(Application application) {
        super(application);
        Log.d(LogTAG, "Inside the LocationsViewModel constructor");
        loadJSON();
        setShowingVisited(true);
    }

    private void loadJSON() {
        Log.d(LogTAG, "Going to asynchronously load the JSON data");
        new LocationsAsyncLoader().execute(getApplication());
    }

    public MutableLiveData<Pair<String, String>> getSelected() {
        return selected;
    }

    public void setSelected(Pair<String, String> selection) {
        selected.setValue(selection);
    }

    public MutableLiveData<List<Pair<String, String>>> getMultiSelected() {
        return multiSelected;
    }

    public void setMultiSelected(List<Pair<String, String>> list) {
        multiSelected.setValue(list);
        Log.d(LogTAG, list.toString());
    }

    public void postMultiSelected(List<Pair<String, String>> list) {
        multiSelected.postValue(list);
    }

    public MutableLiveData<JSONObject> getLocationsJSONdata() {
        return locationsJSONdata;
    }

    public void setLocationsJSONdata(JSONObject obj) {
        locationsJSONdata.setValue(obj);
    }

    public MutableLiveData<Boolean> getShowingVisited() {
        return showingVisited;
    }

    public void setShowingVisited(boolean showing) {
        showingVisited.setValue(showing);
    }

    public void toggleShowingVisited() {
        showingVisited.setValue(!getShowingVisited().getValue());
    }

    /*  Observers of this ViewModel can use this method to stop
        observing changes to any field of the LocationsViewModel
     */
    public void removeObservers(LifecycleOwner lifecycleOwner) {
        getSelected().removeObservers(lifecycleOwner);
        getLocationsJSONdata().removeObservers(lifecycleOwner);
        getShowingVisited().removeObservers(lifecycleOwner);
        getMultiSelected().removeObservers(lifecycleOwner);
    }

    private class LocationsAsyncLoader extends AsyncTask<Application, Void, JSONObject> {
        private final String LogTAG = LocationsAsyncLoader.class.getSimpleName();

        @Override
        protected JSONObject doInBackground(Application... apps) {
            Application appContext = apps[0];

            Log.d(LogTAG, "Reading JSON data from file");
            String jsonString;
            InputStream input = appContext.getResources().openRawResource(R.raw.locations);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            jsonString = output.toString();

            try {
                return new JSONObject(jsonString);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            Log.d(LogTAG, "Data has finished loading");
            if (jsonObject == null) {
                Log.d(LogTAG, "Data loaded is null");
            }
            setLocationsJSONdata(jsonObject);
            super.onPostExecute(jsonObject);
        }
    }
}
