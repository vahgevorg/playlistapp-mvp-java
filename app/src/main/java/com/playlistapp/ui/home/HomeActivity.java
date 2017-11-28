package com.playlistapp.ui.home;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.playlistapp.R;
import com.playlistapp.eventbus.event.OpenWebViewEvent;
import com.playlistapp.ui.base.BaseActivity;
import com.playlistapp.ui.home.settings.SettingsFragment;
import com.playlistapp.ui.home.tracks.TracksFragment;
import com.playlistapp.ui.web.WebViewActivity;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

import static com.playlistapp.Constants.EXTRA_WEB_TITLE;
import static com.playlistapp.Constants.EXTRA_WEB_URL;

/**
 * Home screen activity class.
 */
public class HomeActivity extends BaseActivity
        implements HomeMvpView, NavigationView.OnNavigationItemSelectedListener {

    @Inject
    HomeMvpPresenter<HomeMvpView> mPresenter;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.nav_view)
    NavigationView mNavigationView;
    @BindView(R.id.layout_container)
    DrawerLayout mDrawer;

    @Override
    protected int attachLayoutRes() {
        return R.layout.activity_home;
    }

    @Override
    protected void initInjector() {
        Timber.d("Injecting \"Home\" activity");
        getActivityComponent().inject(this);
    }

    @Override
    protected void initViews() {
        Timber.d("Trying to initialize view elements");
        mPresenter.onAttach(HomeActivity.this);
    }

    @Override
    protected void prepareLayout() {
        super.prepareLayout();
        prepareToolbar();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

    }

    @SuppressWarnings("ConstantConditions")
    private void prepareToolbar() {
        setSupportActionBar(mToolbar);
        prepareNavigationDrawer();
    }

    /**
     * Prepares Navigation drawer with items.
     */
    private void prepareNavigationDrawer() {
        Timber.d("Preparing Navigation view and drawer");
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawer,
                mToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                hideKeyboard();
            }
        };
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        mNavigationView.setNavigationItemSelectedListener(this);
        mPresenter.onNavMenuCreated();
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG);
            if (fragment == null) {
                super.onBackPressed();
            } else {
                onFragmentDetached(SettingsFragment.TAG);
            }
        }
    }

    @Override
    public void onFragmentDetached(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            fragmentManager
                    .beginTransaction()
                    .disallowAddToBackStack()
                    .setCustomAnimations(R.anim.slide_left, R.anim.slide_right)
                    .remove(fragment)
                    .commitNow();
            unlockDrawer();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_tracks:
                showTracksFragment();
                break;
            case R.id.nav_favorites:
                break;
            case R.id.nav_tools:
                showSettingsFragment();
                break;
            case R.id.nav_about:
                break;
            case R.id.nav_share:
                break;
            default:
                break;
        }
        Toast.makeText(this, "Open fragment " + item.getTitle(), Toast.LENGTH_SHORT).show();
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void showSettingsFragment() {
        Timber.d("Showing \"Settings\" fragment");
        lockDrawer();
        getSupportFragmentManager()
                .beginTransaction()
                .disallowAddToBackStack()
                .setCustomAnimations(R.anim.slide_left, R.anim.slide_right)
                .add(R.id.layout_container, SettingsFragment.newInstance(), SettingsFragment.TAG)
                .commit();
    }

    @Override
    public void showTracksFragment() {
        Timber.d("Showing \"Tracks\" fragment");
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(TracksFragment.TAG);
        if (fragment == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_left, R.anim.slide_right)
                    .replace(R.id.layoutMainContainer, TracksFragment.newInstance(), TracksFragment.TAG)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .addToBackStack(TracksFragment.TAG)
                    .setCustomAnimations(R.anim.slide_left, R.anim.slide_right)
                    .replace(R.id.layoutMainContainer, TracksFragment.newInstance())
                    .commit();
        }
    }

    @OnClick(R.id.btnFilter)
    public void onFilterClicked() {
        showSettingsFragment();
    }

    @Subscribe
    public void onOpenWebViewEvent(OpenWebViewEvent event) {
        Timber.d("Trying to open \"Web view\" activity " + event.getWebUrl());
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(EXTRA_WEB_URL, event.getWebUrl());
        intent.putExtra(EXTRA_WEB_TITLE, event.getWebTitle());
        startActivity(intent);
    }

    @Override
    public void lockDrawer() {
        if (mDrawer != null)
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void unlockDrawer() {
        if (mDrawer != null)
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDrawer != null)
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    protected void onDestroy() {
        mPresenter.onDetach();
        super.onDestroy();
    }
}
