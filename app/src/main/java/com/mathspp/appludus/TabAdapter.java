package com.mathspp.appludus;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*  as seen in https://medium.com/@droidbyme/android-material-design-tabs-tab-layout-with-swipe-884085ae80ff
    this is used so that the main ViewPager can automatically populate the existing TabLayout
 */
public class TabAdapter extends FragmentPagerAdapter {
    private final String LogTAG = TabAdapter.class.getSimpleName();

    private final List<Fragment> fragments = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();
    private FragmentManager mFragmentManager;
    private Map<Integer, String> mFragmentTags;

    public TabAdapter(FragmentManager fm) {
        super(fm);
        mFragmentManager = fm;
        mFragmentTags = new HashMap<>();
    }

    public void addFragment(Fragment fragment, String title) {
        fragments.add(fragment);
        titles.add(title);
    }

    @Override
    public Fragment getItem(int i) {
        if (fragments == null) return null;
        return fragments.get(i);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Object obj = super.instantiateItem(container, position);
        if (obj instanceof Fragment) {
            Fragment f = (Fragment) obj;
            fragments.set(position, f);
            String tag = f.getTag();
            mFragmentTags.put(position, tag);
        } else {
            Log.d(LogTAG, "something here was not a fragment");
        }
        return obj;
    }

    public Fragment getFragment(int position) {
        String tag = mFragmentTags.get(position);
        if (tag == null) return null;
        return mFragmentManager.findFragmentByTag(tag);
    }

    // this populates the titles of the tabs
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (titles == null) return "";
        return titles.get(position);
    }

    @Override
    public int getCount() {
        if (fragments == null) return 0;
        return fragments.size();
    }
}