package com.mathspp.appludus;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationViewHolder> {
    private final String LogTAG = LocationsAdapter.class.getSimpleName();
    private List<String> locationsNames;
    private List<String> locationsMarked;
    private OnLocationClickHandler mLocationClickHandler;
    private String category;
    private boolean insideMultiSelection = false;

    public LocationsAdapter(OnLocationClickHandler onLocationClickHandler, String category) {
        if (onLocationClickHandler != null) {
            mLocationClickHandler = onLocationClickHandler;
        } else {
            throw new RuntimeException(LogTAG + " needs a non-null OnLocationClickHandler");
        }
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        View view = layoutInflater.inflate(R.layout.list_item_layout, viewGroup, false);
        return new LocationViewHolder(view);
    }

    /* When the view holder is being bound, set the data to be displayed */
    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder locationViewHolder, int i) {
        String locName = locationsNames.get(i);
        locationViewHolder.mLocationNameTV.setText(locName);
        if (insideMultiSelection) {
            locationViewHolder.mMarkOnMapCB.setVisibility(View.VISIBLE);
            Log.d(LogTAG, "The locations marked are " + locationsMarked.toString());
            Log.d(LogTAG, "marked CB as visible, going to see if locationsMarked contains " + locName);
            if (locationsMarked.contains(locName)) {
                Log.d(LogTAG, "IN HERE");
                locationViewHolder.mMarkOnMapCB.setChecked(true);
            } else {
                locationViewHolder.mMarkOnMapCB.setChecked(false);
            }
        } else {
            locationViewHolder.mMarkOnMapCB.setVisibility(View.GONE);
        }
    }

    public void setLocationsNames(@NonNull List<String> names) {
        locationsNames = new ArrayList<>();
        locationsNames.addAll(names);
        // if we had locations marked that no longer exist, remove them
        if (locationsMarked != null) {
            for (int i = locationsMarked.size()-1; i >= 0; --i) {
                String loc = locationsMarked.get(i);
                if (!names.contains(loc)) {
                    locationsMarked.remove(i);
                    mLocationClickHandler.locationUnselected(category, loc);
                }
            }
        }
        // this notification is built-in and "refreshes" the UI with the new data
        notifyDataSetChanged();
    }

    public void startMultiSelection(List<String> previouslyMarkedLocations) {
        insideMultiSelection = true;
        if (locationsMarked == null) {
            locationsMarked = new ArrayList<>();
        }
        if (previouslyMarkedLocations != null) {
            locationsMarked.addAll(previouslyMarkedLocations);
        }
        Log.d(LogTAG, "Saved the previous values to " + locationsMarked.toString());
        notifyDataSetChanged();
    }

    public void clearMultiSelection() {
        for (String location : locationsMarked) {
            mLocationClickHandler.locationUnselected(category, location);
        }
        locationsMarked = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void allMultiSelection() {
        locationsMarked = new ArrayList<>();
        locationsMarked.addAll(locationsNames);
        notifyDataSetChanged();
    }

    public void stopMultiSelection() {
        if (insideMultiSelection) {
            insideMultiSelection = false;
            locationsMarked.clear();
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        if (locationsNames == null) return 0;
        return locationsNames.size();
    }

    /* Establish an interface to communicate with the outside
        whenever a location is clicked
     */
    public interface OnLocationClickHandler {
        void onLocationClick(String category, String location);
        void onLocationLongClick();
        void locationSelected(String category, String location);
        void locationUnselected(String category, String location);
    }

    /* This class is the container for each list item, aka this class is a ViewHolder
        (because it holds views...)
     */
    public class LocationViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener,
            View.OnLongClickListener,
            CompoundButton.OnCheckedChangeListener {
        private final String LogTAG = LocationViewHolder.class.getSimpleName();

        public TextView mLocationNameTV;
        public CheckBox mMarkOnMapCB;
        public LocationViewHolder(@NonNull final View view) {
            super(view);
            mLocationNameTV = view.findViewById(R.id.tv_location_name);
            mMarkOnMapCB = view.findViewById(R.id.cb_mark_on_map);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            mMarkOnMapCB.setOnCheckedChangeListener(this);
        }

        /* something was clicked, so handle this information */
        @Override
        public void onClick(View v) {
            Log.d(LogTAG, "An item was clicked");
            int index = getAdapterPosition();
            switch (v.getId()) {
                default:
                    if (insideMultiSelection) {
                        ((CheckBox) v.findViewById(R.id.cb_mark_on_map)).toggle();
                    } else {
                        mLocationClickHandler.onLocationClick(category, locationsNames.get(index));
                    }
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(LogTAG, "Something was long cliked");
            mLocationClickHandler.onLocationLongClick();
            return true;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int idx = getAdapterPosition();
            String name = locationsNames.get(idx);
            Log.d(LogTAG, "CheckBox was clicked at position " + idx + " with name " + name);
            // update the status of the location locally
            if (isChecked && !locationsMarked.contains(name)) {
                // if we checked the CheckBox and the location wasn't marked, mark it
                locationsMarked.add(name);
            } else if (!isChecked && locationsMarked.contains(name)) {
                // if we unchecked the CheckBox and the location was marked, remove it
                locationsMarked.remove(name);
            }
            // update the status of the location globally
            if (isChecked) mLocationClickHandler.locationSelected(category, name);
            else mLocationClickHandler.locationUnselected(category, name);
        }
    }
}
