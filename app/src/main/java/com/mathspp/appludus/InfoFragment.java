package com.mathspp.appludus;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.mathspp.appludus.viewModels.InfoFragmentViewModel;
import com.mathspp.appludus.viewModels.LocationsViewModel;
import com.mathspp.appludus.viewModels.NotificationsViewModel;
import com.mathspp.appludus.viewModels.UserLocViewModel;

import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InfoFragment.OnInfoFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class InfoFragment extends Fragment {
    private final String LogTAG = InfoFragment.class.getSimpleName();

    private OnInfoFragmentInteractionListener mListener;
    private LocationsViewModel locationsViewModel;
    private UserLocViewModel mUserLocViewModel;
    private NotificationsViewModel notificationsViewModel;
    private InfoFragmentViewModel infoFragmentViewModel;

    private TextView mLocationNameTV;
    private LinearLayout mDistanceTimeInfo;
    private TextView mDistanceToLocationTV;
    private TextView mTimeToLocationTV;
    private Switch mVisitedSwitch;
    private TextView mLocationInfoTV;
    private ProgressBar mLoadInfoPB;
    private ProgressBar mLoadDistanceTimePB;
    private Button mShowOnMapButton;

    /*  use these so that we can cancel previous async tasks
            if the selected location changes meanwhile
     */
    private DistanceAsyncFetcher mDistanceAsyncFetcher;
    private InfoAsyncLoader mInfoAsyncLoader;

    public InfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationsViewModel = ViewModelProviders.of(getActivity()).get(LocationsViewModel.class);
        mUserLocViewModel = ViewModelProviders.of(getActivity()).get(UserLocViewModel.class);
        notificationsViewModel = ViewModelProviders.of(getActivity()).get(NotificationsViewModel.class);
        infoFragmentViewModel = ViewModelProviders.of(getActivity()).get(InfoFragmentViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_info, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mLocationNameTV = getActivity().findViewById(R.id.tv_location_name);
        mDistanceToLocationTV = getActivity().findViewById(R.id.tv_distance_to);
        mTimeToLocationTV = getActivity().findViewById(R.id.tv_time_to);
        mVisitedSwitch = getActivity().findViewById(R.id.switch_visited);
        mLocationInfoTV = getActivity().findViewById(R.id.tv_location_info);
        mLoadInfoPB = getActivity().findViewById(R.id.pb_loading_info);
        mLoadDistanceTimePB = getActivity().findViewById(R.id.pb_load_distance_time);
        mDistanceTimeInfo = getActivity().findViewById(R.id.distance_time_info_view);
        mShowOnMapButton = getActivity().findViewById(R.id.btn_show_on_map);

        mDistanceTimeInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*  If we clicked the info and it had nothing displayed, check if the location
                    settings are how we wanted them, and ask for an update if needed
                 */
                if (mUserLocViewModel.getLastKnownLocation().getValue() == null &&
                        !mUserLocViewModel.getLocationSettingsSetupResult().getValue()) {
                    ((MainActivity) getActivity()).mUserLocationSettingsUtils.setupLocationSettings();
                }
                /*  Check if we have location settings and if we have a place to find a distance to
                * but only if we are not calculating distances yet */
                if (mUserLocViewModel.getLastKnownLocation().getValue() != null &&
                        locationsViewModel.getSelected().getValue() != null &&
                        mDistanceAsyncFetcher == null) {
                    new DistanceAsyncFetcher().execute(locationsViewModel.getSelected().getValue());
                }
            }
        });

        mShowOnMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // (TODO) ensure we can remove this line below vvv
                if (mListener != null) {
                    mListener.showSelectedOnMap();
                }
            }
        });

        /*  observe changes to the selected location */
        locationsViewModel.getSelected().observe(this, new Observer<Pair<String, String>>() {
            @Override
            public void onChanged(@Nullable Pair<String, String> selected) {
                String locCategory = selected.first;
                String locName = selected.second;
                if (mUserLocViewModel.getLastKnownLocation().getValue() == null) {
                    //  if we got no user location, try to get one
                    ((MainActivity) getActivity()).mUserLocationSettingsUtils.getDeviceLocation();
                }
                if (mLocationNameTV != null) {
                    mLocationNameTV.setText(locName);
                }
                if (mVisitedSwitch != null) {
                    VisitedStatusHandler handler = ((MainActivity) getActivity()).getVisitedStatusHandler();
                    boolean status = handler.getVisitedStatus(locName);
                    mVisitedSwitch.setChecked(status);
                }
                if (mLocationInfoTV != null) {
                    // ask for the information asynchronously
                    showLoadingInfo();
                    new InfoAsyncLoader().execute(selected);
                }
                /*  if we selected a location for which we do not have the time/distance info, we
                    surely do not want to be displaying the wrong info
                 */
                if (!selected.equals(infoFragmentViewModel.getLastPairUsed().getValue())) {
                    mDistanceToLocationTV.setText(getString(R.string.distance_to_location_null));
                    mTimeToLocationTV.setText(getString(R.string.time_to_location_null));
                // if possible, display cached results
                } else {
                    if (infoFragmentViewModel.getDistanceToLocationStr().getValue() != null) {
                        mDistanceToLocationTV.setText(getString(R.string.distance_to_location,
                                infoFragmentViewModel.getDistanceToLocationStr().getValue())
                        );
                    }
                    if (infoFragmentViewModel.getTimeToLocationStr().getValue() != null) {
                        mTimeToLocationTV.setText(getString(R.string.time_to_location,
                                infoFragmentViewModel.getTimeToLocationStr().getValue()));
                    }
                }
            }
        });

        mVisitedSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                VisitedStatusHandler handler = ((MainActivity) getActivity()).getVisitedStatusHandler();
                handler.setVisitedStatus(locationsViewModel.getSelected().getValue().second, isChecked);
                if (locationsViewModel.getShowingVisited() != null &&
                        locationsViewModel.getShowingVisited().getValue() != null &&
                        !locationsViewModel.getShowingVisited().getValue()) {
                    notificationsViewModel.setVisitedStatusChanged(true);
                }
            }
        });

        /*  Observing changes to the distance/time values will allow us to update them */
        infoFragmentViewModel.getDistanceToLocationStr().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                /* only update if the location we are looking at is the same as the one selected
                    otherwise set the default distance text
                 */
                if (s != null && infoFragmentViewModel.getLastPairUsed().getValue() != null &&
                        infoFragmentViewModel.getLastPairUsed().getValue().equals(locationsViewModel.getSelected().getValue())) {
                    mDistanceToLocationTV.setText(
                            getString(R.string.distance_to_location, s)
                    );
                } else {
                    mDistanceToLocationTV.setText(
                            getString(R.string.distance_to_location_null)
                    );
                }
            }
        });
        infoFragmentViewModel.getTimeToLocationStr().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s != null && infoFragmentViewModel.getLastPairUsed() != null &&
                        infoFragmentViewModel.getLastPairUsed().getValue().equals(locationsViewModel.getSelected().getValue())) {
                    mTimeToLocationTV.setText(
                            getString(R.string.time_to_location, s)
                    );
                } else {
                    mTimeToLocationTV.setText(
                            getString(R.string.time_to_location_null)
                    );
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /*  unregister the observations when this tab is destroyed */
        locationsViewModel.removeObservers(this);
        infoFragmentViewModel.removeObservers(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInfoFragmentInteractionListener) {
            mListener = (OnInfoFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnInfoFragmentInteractionListener {
        void showSelectedOnMap();
    }

    public void showLoadingInfo() {
        mLocationInfoTV.setVisibility(View.GONE);
        mLoadInfoPB.setVisibility(View.VISIBLE);
    }

    public void showInfoLoaded() {
        mLoadInfoPB.setVisibility(View.GONE);
        mLocationInfoTV.setVisibility(View.VISIBLE);
    }

    public void showLoadingDistanceTime() {
        mDistanceToLocationTV.setVisibility(View.GONE);
        mTimeToLocationTV.setVisibility(View.GONE);
        mLoadDistanceTimePB.setVisibility(View.VISIBLE);
    }

    public void showDistanceTimeLoaded() {
        mLoadDistanceTimePB.setVisibility(View.GONE);
        mDistanceToLocationTV.setVisibility(View.VISIBLE);
        mTimeToLocationTV.setVisibility(View.VISIBLE);
    }

    /* Use this location to find time and distance to a given location asynchronously */
    private class DistanceAsyncFetcher extends AsyncTask<Pair<String, String>, Void, HashMap<String, String>> {
        private final String LogTAG = DistanceAsyncFetcher.class.getSimpleName();
        private final static int SUSPENSE_PAUSE = 800;

        @Override
        protected void onPreExecute() {
            if (mDistanceAsyncFetcher != null && mDistanceAsyncFetcher.getStatus() != Status.FINISHED) {
                mDistanceAsyncFetcher.cancel(true);
            }
            mDistanceAsyncFetcher = this;
            showLoadingDistanceTime();
            super.onPreExecute();
        }

        @Override
        protected HashMap<String, String> doInBackground(Pair<String, String>... pairs) {
            // do a short pause here to help discourage irrelevant calls by the user
            try {
                Thread.sleep(DistanceAsyncFetcher.SUSPENSE_PAUSE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            infoFragmentViewModel.postLastPairUsed(pairs[0]);
            String category = pairs[0].first;
            String name = pairs[0].second;
            HashMap<String, String> requestResults;
            LatLng selectedLocation = DataUtils.getLatLngFromName(
                                            locationsViewModel.getLocationsJSONdata().getValue(),
                                            category, name);
            Location userLocation = mUserLocViewModel.getLastKnownLocation().getValue();
            if (userLocation == null) { // we have no user location
                return getNullResultsHashMap();
            }

            requestResults = GoogleAPIUtils.getDistanceTo(
                    new LatLng(userLocation.getLatitude(), userLocation.getLongitude()),
                    selectedLocation, getString(R.string.distance_matrix_api_key)
            );

            if (requestResults == null) {  // the requests all failed
                return getNullResultsHashMap();
            }

            HashMap<String, String> results = new HashMap<>();
            results.put(GoogleAPIUtils.TIME_TEXT_KEY,
                        requestResults.get(GoogleAPIUtils.TIME_TEXT_KEY));
            results.put(GoogleAPIUtils.DISTANCE_TEXT_KEY,
                        requestResults.get(GoogleAPIUtils.DISTANCE_TEXT_KEY));
            return results;
        }

        @Override
        protected void onPostExecute(HashMap<String, String> results) {
            Log.d(LogTAG, "onPostExecute");
            if (getContext() == null) {
                Log.d(LogTAG, "We are not attached to a context");
                super.onPostExecute(results);
            }
            showDistanceTimeLoaded();
            infoFragmentViewModel.postDistanceToLocationStr(results.get(GoogleAPIUtils.DISTANCE_TEXT_KEY));
            infoFragmentViewModel.postTimeToLocationStr(results.get(GoogleAPIUtils.TIME_TEXT_KEY));
            mDistanceAsyncFetcher = null;
            super.onPostExecute(results);
        }
    }

    private HashMap<String, String> getNullResultsHashMap() {
        HashMap<String, String> results = new HashMap<>();
        results.put(GoogleAPIUtils.DISTANCE_TEXT_KEY,
                getString(R.string.distance_to_location_null));
        results.put(GoogleAPIUtils.TIME_TEXT_KEY,
                getString(R.string.time_to_location_null));
        return results;
    }

    /*  Use this class to ask for the location information asynchronously */
    private class InfoAsyncLoader extends AsyncTask<Pair<String, String>, Void, String> {
        @Override
        protected void onPreExecute() {
            if (mInfoAsyncLoader != null && mInfoAsyncLoader.getStatus() != Status.FINISHED) {
                mInfoAsyncLoader.cancel(true);
            }
            mInfoAsyncLoader = this;

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Pair<String, String>... pairs) {
            String category = pairs[0].first;
            String name = pairs[0].second;
            return DataUtils.getInfoFromName(locationsViewModel.getLocationsJSONdata().getValue(), category,
                                                getContext(), name);
        }

        @Override
        protected void onPostExecute(String info) {
            /*  use https://stackoverflow.com/a/2116191/2828287
                        so that our string is interpreted as HTML and the link tags work well
                     */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationInfoTV.setText(Html.fromHtml(info, Html.FROM_HTML_MODE_COMPACT));
            } else {
                mLocationInfoTV.setText(Html.fromHtml(info));
            }
            //  this enables clicking on links
            mLocationInfoTV.setMovementMethod(LinkMovementMethod.getInstance());
            showInfoLoaded();
            mInfoAsyncLoader = null;
            super.onPostExecute(info);
        }
    }
}
