package com.example.tobiastrumm.freifunkautoconnect;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;

import static butterknife.OnPageChange.Callback.PAGE_SCROLL_STATE_CHANGED;
import static butterknife.OnPageChange.Callback.PAGE_SELECTED;


public class MainActivity extends AppCompatActivity implements AddRemoveNetworksFragment.OnFragmentInteractionListener, RemoveAllDialogFragment.OnRemoveAllListener, AddAllDialogFragment.OnAddAllListener, NearestNodesFragment.OnFragmentInteractionListener{

    private static String TAG = MainActivity.class.getSimpleName();

    private MyFragmentPagerAdapter myFragmentPagerAdapter;
    int currentPagerPosition = 0;

    @BindView(R.id.appbar) AppBarLayout appBarLayout;
    @BindView(R.id.viewpager) ViewPager viewPager;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.tabLayout) TabLayout tabLayout;
    @BindView(R.id.coordinatorlayout) CoordinatorLayout coordinatorLayout;


    private void checkForNewSsidFile(){
        Intent intent = new Intent(this, DownloadSsidJsonService.class);
        startService(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind with ButterKnife annotated fields/methods to this activity.
        ButterKnife.bind(this);

        // Use Toolbar instead of ActionBar. See:
        // http://blog.xamarin.com/android-tips-hello-toolbar-goodbye-action-bar/
        // https://stackoverflow.com/questions/29055491/android-toolbar-for-api-19-for-api-21-works-ok
        setSupportActionBar(toolbar);

        // Setup Tabs and Fragments
        String titles[] =  {getString(R.string.nearest_freifunk), getString(R.string.ssids)};
        myFragmentPagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), titles);

        viewPager.setAdapter(myFragmentPagerAdapter);

        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);

        // Start NotificationService if it should running but isn't
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_preference_key_settings), Context.MODE_PRIVATE);
        boolean notifications = sharedPref.getBoolean("pref_notification", false);
        if(notifications && !isNotificationServiceRunning()){
            startService(new Intent(this, NotificationService.class));
        }

        checkForNewSsidFile();
    }

    /*
        onPageSelected callback for the viewPager, bound to it with ButterKnife
     */
    @OnPageChange(value = R.id.viewpager, callback = PAGE_SELECTED)
    void onPageSelected(int newPosition) {
        FragmentLifecycle fragmentToShow;
        switch (newPosition) {
            case 0:
                fragmentToShow = myFragmentPagerAdapter.nearestNodesFragment;
                break;
            case 1:
                fragmentToShow = myFragmentPagerAdapter.addRemoveNetworksFragment;
                break;
            default:
                fragmentToShow = null;
        }

        FragmentLifecycle fragmentToHide;
        switch (currentPagerPosition) {
            case 0:
                fragmentToHide = myFragmentPagerAdapter.nearestNodesFragment;
                break;
            case 1:
                fragmentToHide = myFragmentPagerAdapter.addRemoveNetworksFragment;
                break;
            default:
                fragmentToHide = null;
        }

        if (fragmentToShow != null) {
            fragmentToShow.onResumeFragment();
        }
        if (fragmentToHide != null) {
            fragmentToHide.onPauseFragment();
        }
        currentPagerPosition = newPosition;
    }

    /*
        onPageScrollStateChanged callback for the viewPager, bound to it with ButterKnife
     */
    @OnPageChange(value = R.id.viewpager, callback = PAGE_SCROLL_STATE_CHANGED)
    void onPageScrollStateChanged(int state) {
        if(state == ViewPager.SCROLL_STATE_DRAGGING){
            // Expand Toolbar if the tab was switched.
            appBarLayout.setExpanded(true, true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // The toolbar should always be expanded when the activity is started.
        appBarLayout.setExpanded(true, false);


        /********************************
         * Without this code the footer on the AddRemoveNetworksFragment would not be shown when the activity is restarted
         * after the the toolbar was collapsed. A new ViewTreeObserver.OnGlobalLayoutListener is added that set changes the
         * padding of the ViewPager as soon as the layout is ready to be changed. The Listener then removes itself to prevent
         * further calls.
         ********************************/
        coordinatorLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    coordinatorLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                else {
                    coordinatorLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                int bottomPadding = appBarLayout.getTotalScrollRange() + appBarLayout.getTop();
                boolean paddingChanged = bottomPadding != viewPager.getPaddingBottom();
                if (paddingChanged) {
                    viewPager.setPadding(
                            viewPager.getPaddingLeft(),
                            viewPager.getPaddingTop(),
                            viewPager.getPaddingRight(),
                            bottomPadding);
                            viewPager.requestLayout();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {
            Intent intent = new Intent(MainActivity.this, InfoActivity.class);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.action_settings){
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private boolean isNotificationServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for ( ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(NotificationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addAllNetworks() {
        myFragmentPagerAdapter.addRemoveNetworksFragment.addAllNetworks();
    }

    @Override
    public void removeAllNetworks() {
        myFragmentPagerAdapter.addRemoveNetworksFragment.removeAllNetworks();
    }

    @Override
    public void showDialogAddAllNetworks() {
        AddAllDialogFragment df = new AddAllDialogFragment();
        df.show(this.getFragmentManager(),"");
    }

    @Override
    public void showDialogRemoveAllNetworks() {
        RemoveAllDialogFragment df = new RemoveAllDialogFragment();
        df.show(this.getFragmentManager(), "");
    }

    @Override
    public void showDialogSSIDRemovalFailed() {
        RemovalFailedDialogFragment df = new RemovalFailedDialogFragment();
        df.show(this.getFragmentManager(), "");
    }

    @Override
    public void showDialogRemoveAllRemovalFailed(int number_failed_removal) {
        RemoveAllRemovalFailedDialogFragment df = new RemoveAllRemovalFailedDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(RemoveAllRemovalFailedDialogFragment.ARGUMENT_NUMBER_FAILED_REMOVAL, number_failed_removal);
        df.setArguments(arguments);
        df.show(this.getFragmentManager(), "");
    }
}