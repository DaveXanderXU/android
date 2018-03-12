/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2018 Bartosz Przybylski
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountAuthenticatorActivity;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.features.FeatureList;
import com.owncloud.android.features.FeatureList.FeatureItem;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;

/**
 * @author Bartosz Przybylski
 */
public class WhatsNewActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private static final String KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode";

    private ImageButton mForwardFinishButton;
    private ProgressIndicator mProgress;
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whats_new_activity);

        mProgress = (ProgressIndicator) findViewById(R.id.progressIndicator);
        mPager = (ViewPager)findViewById(R.id.contentPanel);
        boolean isBeta = BuildConfig.BUILD_TYPE.equals(MainApp.BUILD_TYPE_BETA);

        FeaturesViewAdapter adapter = new FeaturesViewAdapter(getSupportFragmentManager(),
                FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun(), isBeta));

        mProgress.setNumberOfSteps(adapter.getCount());
        mPager.setAdapter(adapter);
        mPager.addOnPageChangeListener(this);


        mForwardFinishButton = (ImageButton) findViewById(R.id.forward);
        mForwardFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mProgress.hasNextStep()) {
                    mPager.setCurrentItem(mPager.getCurrentItem()+1, true);
                    mProgress.animateToStep(mPager.getCurrentItem()+1);
                } else {
                    onFinish();
                    finish();
                }
                updateNextButtonIfNeeded();
            }
        });
        Button skipButton = (Button) findViewById(R.id.skip);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFinish();
                finish();
            }
        });

        updateNextButtonIfNeeded();
    }

    @Override
    public void onBackPressed() {
        onFinish();
        super.onBackPressed();
    }


    private void updateNextButtonIfNeeded() {
        if (!mProgress.hasNextStep()) {
            mForwardFinishButton.setImageResource(R.drawable.ic_done_white);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mForwardFinishButton.setBackground(getResources().getDrawable(R.drawable.round_button));
            } else {
                mForwardFinishButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button));
            }
        } else {
            mForwardFinishButton.setImageResource(R.drawable.ic_arrow_forward);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mForwardFinishButton.setBackground(null);
            } else {
                mForwardFinishButton.setBackgroundDrawable(null);
            }
        }
    }

    private void onFinish() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_LAST_SEEN_VERSION_CODE, MainApp.getVersionCode());
        editor.apply();
    }

    static private int getLastSeenVersionCode() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
        return pref.getInt(KEY_LAST_SEEN_VERSION_CODE, 0);
    }

    static private boolean isFirstRun() {
        if (getLastSeenVersionCode() != 0)
            return false;
        return AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext()) == null;

    }

    static public void runIfNeeded(Context context) {
        if (context instanceof WhatsNewActivity)
            return;

        if (shouldShow(context))
            context.startActivity(new Intent(context, WhatsNewActivity.class));
    }

    static private boolean shouldShow(Context context) {
        boolean isBeta = BuildConfig.BUILD_TYPE.equals(MainApp.BUILD_TYPE_BETA);
        boolean showWizard = context.getResources().getBoolean(R.bool.wizard_enabled);
        return showWizard &&
                ((isFirstRun() && context instanceof AccountAuthenticatorActivity) ||
                (
                        !(isFirstRun() && (context instanceof FileDisplayActivity)) &&
                        !(context instanceof PassCodeActivity) &&
                        (FeatureList.getFiltered(getLastSeenVersionCode(), isFirstRun(), isBeta).length > 0)
                ));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mProgress.animateToStep(position+1);
        updateNextButtonIfNeeded();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private final class FeaturesViewAdapter extends FragmentPagerAdapter {

        FeatureItem[] mFeatures;

        public FeaturesViewAdapter(FragmentManager fm, FeatureItem[]features) {
            super(fm);
            mFeatures = features;
        }

        @Override
        public Fragment getItem(int position) {
            return FeatureFragment.newInstance(mFeatures[position]);
        }

        @Override
        public int getCount() {
            return mFeatures.length;
        }
    }

    public static class FeatureFragment extends Fragment {
        private FeatureItem mItem;

        static public FeatureFragment newInstance(FeatureItem item) {
            FeatureFragment f = new FeatureFragment();
            Bundle args = new Bundle();
            args.putParcelable("feature", item);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mItem = getArguments() != null ? (FeatureItem)getArguments().getParcelable("feature") : null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.whats_new_element, container, false);

            ImageView iv = (ImageView)v.findViewById(R.id.whatsNewImage);
            if (mItem.shouldShowImage())
                iv.setImageResource(mItem.getImage());

            TextView tv2 = (TextView)v.findViewById(R.id.whatsNewTitle);
            if (mItem.shouldShowTitleText())
                tv2.setText(mItem.getTitleText());

            tv2 = (TextView)v.findViewById(R.id.whatsNewText);
            if (mItem.shouldShowContentText())
                tv2.setText(mItem.getContentText());

            return v;
        }
    }
}