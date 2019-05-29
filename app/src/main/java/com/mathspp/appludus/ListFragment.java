package com.mathspp.appludus;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mathspp.appludus.interfaces.TabChangeRequestHandler;
import com.mathspp.appludus.viewModels.ListFragmentViewModel;
import com.mathspp.appludus.viewModels.LocationsViewModel;
import com.mathspp.appludus.viewModels.NotificationsViewModel;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TabChangeRequestHandler} interface
 * to handle interaction events.
 */
public class ListFragment extends Fragment implements
        LocationsAdapter.OnLocationClickHandler,
        ActionMode.Callback {
    private final String LogTAG = ListFragment.class.getSimpleName();

    private List<String> categories;
    private List<View> categoryContainers;
    private List<LocationsAdapter> categoryAdapters;

    private TabChangeRequestHandler mListener;
    private LocationsViewModel locationsViewModel;
    private NotificationsViewModel notificationsViewModel;
    private ListFragmentViewModel listFragmentViewModel;
    private ViewGroup mViewGroupListContainer;
    private ProgressBar mLoadingIndicator;
    private FloatingActionButton mFAB;

    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LogTAG, "lifecycle: onCreate");
        super.onCreate(savedInstanceState);

        /* acquire the view model and hook the observer to the list data */
        locationsViewModel = ViewModelProviders.of(getActivity()).get(LocationsViewModel.class);
        notificationsViewModel = ViewModelProviders.of(getActivity()).get(NotificationsViewModel.class);
        listFragmentViewModel = ViewModelProviders.of(getActivity()).get(ListFragmentViewModel.class);
    }

    public void showLoading() {
        mViewGroupListContainer.setVisibility(View.GONE); // GONE = INVISIBLE + (no longer takes up space)
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    public void showData() {
        mLoadingIndicator.setVisibility(View.GONE);
        mViewGroupListContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(LogTAG, "lifecycle: onCreateView");
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    public void requestShowMap() {
        if (mListener != null) {
            mListener.showMapTab();
        }
    }

    public void requestShowInfo() {
        if (mListener != null) {
            mListener.showInfoTab();
        }
    }

    /* bind all the views with the variables and set that up */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(LogTAG, "lifecycle: onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        mLoadingIndicator = getActivity().findViewById(R.id.pb_loading_indicator);
        mViewGroupListContainer = getActivity().findViewById(R.id.inner_contents_layout);

        mFAB = getActivity().findViewById(R.id.fab);

        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationsViewModel.toggleShowingVisited();
                if (locationsViewModel.getShowingVisited().getValue() != null &&
                        locationsViewModel.getShowingVisited().getValue()) {
                    Toast.makeText(getContext(), getString(R.string.visited_were_shown), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.visited_were_hidden), Toast.LENGTH_SHORT).show();
                }
                JSONObject data = locationsViewModel.getLocationsJSONdata().getValue();
                setLocationListContents(data);
            }
        });
        /*  acquire the observer in here
            we want to know whenever the JSON data changes */
        locationsViewModel.getLocationsJSONdata().observe(this, new Observer<JSONObject>() {
            @Override
            /* when the JSON data changes, this is called */
            public void onChanged(@Nullable JSONObject jsonObject) {
                Log.d(LogTAG, "The ListFragment has been notified that the JSON data changed");
                if (jsonObject == null) {
                    Log.d(LogTAG, "Got null JSON data");
                } else {
                    showData();
                    setLocationListContents(jsonObject);
                }
            }
        });
        locationsViewModel.getShowingVisited().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean showing) {
                if (showing) {
                    /*  we are showing the visited, so now we need to display the button to hide them
                     */
                    mFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_visibility_off_white_24dp));
                } else {
                    mFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_visibility_white_24dp));
                }
            }
        });
        notificationsViewModel.getVisitedStatusChanged().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean flag) {
                Log.d(LogTAG, "onChanged " + flag);
                if (flag != null && flag) {
                    setLocationListContents(locationsViewModel.getLocationsJSONdata().getValue());
                    notificationsViewModel.setVisitedStatusChanged(false);
                }
            }
        });
        notificationsViewModel.getInsideContextualMultiSelection().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean flag) {
                Log.d(LogTAG, "observing contextualMultiSelection with value " + flag);
                if (flag) {
                    ActionMode mode = ((AppCompatActivity) getContext()).startSupportActionMode(ListFragment.this);
                    listFragmentViewModel.setActionMode(mode);
                    if (locationsViewModel.getMultiSelected().getValue() == null) {
                        Log.d(LogTAG, "Sending an empty multi-selection");
                        for (LocationsAdapter adapter : categoryAdapters) {
                            adapter.startMultiSelection(new ArrayList<String>());
                        }
                    } else {
                        Log.d(LogTAG, "Sending previous values " + locationsViewModel.getMultiSelected().getValue().toString());
                        List<Pair<String, String>> previouslyMarked = locationsViewModel.getMultiSelected().getValue();
                        // divide previously marked places by category and assign to corresponding adapter
                        for (LocationsAdapter adapter : categoryAdapters) {
                            String category = adapter.getCategory();
                            List<String> locationNames = new ArrayList<>();
                            for (Pair<String, String> pair : previouslyMarked) {
                                if (pair.first.equals(category)) {
                                    Log.d(LogTAG, "hit");
                                    locationNames.add(pair.second);
                                }
                            }
                            adapter.startMultiSelection(locationNames);
                        }
                    }
                } else {
                    for (LocationsAdapter adapter : categoryAdapters) {
                        adapter.stopMultiSelection();
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        Log.d(LogTAG, "lifecycle: onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(LogTAG, "lifecycle: onResume");
        super.onResume();
        /*  if we are currently hiding the visited places, we might need to refresh the list
         *  do this only when the list comes back into view */
        /* (TODO) in some cases, we fill the data twice in a row, once from this and
            once from observing the dataJsonObject at locationsViewModel */
        if (locationsViewModel.getLocationsJSONdata() != null &&
                locationsViewModel.getLocationsJSONdata().getValue() != null) {
            setLocationListContents(locationsViewModel.getLocationsJSONdata().getValue());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LogTAG, "Unregistering the observers");
        locationsViewModel.removeObservers(this);
        notificationsViewModel.removeObservers(this);
    }

    @Override
    public void onAttach(Context context) {
        Log.d(LogTAG, "lifecycle: onAttach");
        super.onAttach(context);
        if (context instanceof TabChangeRequestHandler) {
            mListener = (TabChangeRequestHandler) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement TabChangeRequestHandler");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onLocationClick(String category, String location) {
        Pair<String, String> pair = new Pair<>(category, location);
        locationsViewModel.setSelected(pair);
        notificationsViewModel.setMapMarkerMode(GoogleMapsFragment.SINGLE_MARK_MODE);
        // (TODO) stop using this call as a stub to change tabs
        requestShowInfo();
    }

    @Override
    public void onLocationLongClick() {
        Log.d(LogTAG, "Location long click received");
        // if we already are in the multi selection mode, do nothing
        if (notificationsViewModel.getInsideContextualMultiSelection() != null &&
                notificationsViewModel.getInsideContextualMultiSelection().getValue() != null &&
                notificationsViewModel.getInsideContextualMultiSelection().getValue()) {
            return;
        }
        triggerNewContextualMultiSelection();
    }

    /*  This function takes the necessary actions to trigger the
        Contextual Action Mode that allows for multi selection
     */
    public void triggerNewContextualMultiSelection() {
        locationsViewModel.setMultiSelected(new ArrayList<Pair<String, String>>());
        notificationsViewModel.setInsideContextualMultiSelection(true);
    }

    @Override
    public void locationSelected(String category, String location) {
        List<Pair<String, String>> locationList;
        if (locationsViewModel.getMultiSelected() == null) {
            locationList = new ArrayList<>();
        } else locationList = locationsViewModel.getMultiSelected().getValue();
        if (locationList == null) locationList = new ArrayList<>();

        Pair<String, String> pair = new Pair<>(category, location);
        if (!locationList.contains(pair)) locationList.add(pair);
        locationsViewModel.setMultiSelected(locationList);
    }

    @Override
    public void locationUnselected(String category, String location) {
        Log.d(LogTAG, "inside locationUnselected with location " + location);
        List<Pair<String, String>> locationList;
        if (locationsViewModel.getMultiSelected() == null) {
            locationList = new ArrayList<>();
        } else locationList = locationsViewModel.getMultiSelected().getValue();
        if (locationList == null) locationList = new ArrayList<>();

        Pair<String, String> pair = new Pair<>(category, location);
        locationList.remove(pair);
        locationsViewModel.setMultiSelected(locationList);
    }

    /*  This method checks whether we should display all locations,
            or only the ones to be visited, and updates the UI accordingly
         */
    public void setLocationListContents(JSONObject data) {
        /* set up the contents of each category */
        if (data == null) return;
        List<String> catList = DataUtils.getLocationCategories(data);
        if (catList == null) return;

        if (categories == null) categories = new ArrayList<>(catList);
        else {
            categories.clear();
            categories.addAll(catList);
        };

        createCategoryAdapters();

        for (int i = 0; i < categories.size(); ++i) {
            String category = categories.get(i);
            Log.d(LogTAG, "Filling in category " + category);
            List<String> names;
            if (locationsViewModel.getShowingVisited().getValue()) {
                names = DataUtils.getLocationNames(data, category);
            } else {
                VisitedStatusHandler handler = ((MainActivity) getActivity()).getVisitedStatusHandler();
                names = DataUtils.getNonVisitedLocationNames(data, category, handler);
            }
            /* failed to get the names for this category */
            if (names == null) continue;
            categoryAdapters.get(i).setLocationsNames(names);

            View container = categoryContainers.get(i);
            TextView textView = container.findViewById(R.id.category_name);
            String text = getResources().getQuantityString(R.plurals.category_header, names.size(), category.toUpperCase(), names.size());
            textView.setText(text);
        }

        /*
        int nLocs = locationNames.size();
        String text = getResources().getQuantityString(R.plurals.locations_count, nLocs, nLocs);
        mTextView.setText(text);
        mLocationsAdapter.setLocationsNames(locationNames); */
    }

    private void createCategoryAdapters() {
        /* check if the adapters have already been set-up */
        if (categories != null && categoryAdapters != null &&
                categoryAdapters.size() == categories.size()) {
            return;
        }
        /* Set up the views for the categories */
        ViewGroup container = getActivity().findViewById(R.id.inner_contents_layout);
        categoryContainers = new ArrayList<>();
        categoryAdapters = new ArrayList<>();
        for (String cat : categories) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.category_view, container, false);

            RecyclerView recyclerView = view.findViewById(R.id.rv_category_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false
            ));
            LocationsAdapter adapter = new LocationsAdapter(this, cat);
            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(true);

            container.addView(view);
            categoryContainers.add(view);
            categoryAdapters.add(adapter);
        }
    }

    public void onTabUnselected() {
        Log.d(LogTAG, "tab was unselected");
        if (listFragmentViewModel != null &&
                listFragmentViewModel.getActionMode() != null &&
                listFragmentViewModel.getActionMode().getValue() != null) {
            Log.d(LogTAG, "finishing support action mode");
            listFragmentViewModel.getActionMode().getValue().finish();
        }

        if (categoryAdapters != null) {
            for (LocationsAdapter adapter : categoryAdapters) {
                adapter.stopMultiSelection();
            }
        } else {
            Log.d(LogTAG, "no adapters available...");
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.contextual_multi_selection_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.show_in_map_option:
                notificationsViewModel.setMapMarkerMode(GoogleMapsFragment.MULTI_MARK_MODE);
                // (TODO) before categories, I had this line here below and I deleted it; does everything still work?
                //locationsViewModel.setMultiSelected(mLocationsAdapter.getMultiSelection());
                actionMode.finish();
                requestShowMap();
                return true;
            // asynchronously select all or clear all
            case R.id.select_all_option:
                new ContextualBulkSelectionSetup().execute(ContextualBulkSelectionSetup.SELECT_ALL);
                return true;
            case R.id.select_none_option:
                new ContextualBulkSelectionSetup().execute(ContextualBulkSelectionSetup.SELECT_NONE);
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        notificationsViewModel.setInsideContextualMultiSelection(false);
    }


    class ContextualBulkSelectionSetup extends AsyncTask<Integer, Void, Void> {
        public static final int SELECT_ALL = 0;
        public static final int SELECT_NONE = 1;

        @Override
        protected void onPreExecute() {
            showLoading();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            int id = integers[0];
            // (TODO) refactor the select all/none to do by category
            for (LocationsAdapter adapter : categoryAdapters) {
                switch (id) {
                    case SELECT_ALL:
                        adapter.allMultiSelection();
                        break;
                    case SELECT_NONE:
                        adapter.clearMultiSelection();
                        break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            showData();
            super.onPostExecute(aVoid);
        }
    }
}