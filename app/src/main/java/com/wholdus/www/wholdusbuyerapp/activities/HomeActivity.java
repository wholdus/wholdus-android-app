package com.wholdus.www.wholdusbuyerapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;
import com.wholdus.www.wholdusbuyerapp.fragments.CategoryGridFragment;
import com.wholdus.www.wholdusbuyerapp.fragments.HomeFragment;
import com.wholdus.www.wholdusbuyerapp.fragments.NavigationDrawerFragment;
import com.wholdus.www.wholdusbuyerapp.helperClasses.Constants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.FilterClass;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.interfaces.HomeListenerInterface;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class HomeActivity extends AppCompatActivity implements HomeListenerInterface {

    private DrawerLayout mDrawerLayout;
    private boolean mDoublePressToExit;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        routeToActivity();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // check for google play api
        HelperFunctions.checkGooglePlay(this);

        // initialize the toolbar
        initToolbar();

        // initialize the navigation drawer
        initNavigationDrawer();

        openToFragment(getFragmentToOpenName(savedInstanceState), null);
    }

    public void routeToActivity(){
        Intent receivedIntent = getIntent();
        if (getIntent() == null){
            return;
        }
        Bundle bundle = receivedIntent.getBundleExtra("router");
        if (bundle == null){
            return;
        }
        Class activityClass;
        Intent intent = new Intent();

        String activityToStart = bundle.getString("activity", "");

        switch (activityToStart) {
            case "Handpicked":
                activityClass = HandPickedActivity.class;
                String productIDs = bundle.getString("productIDs", "");
                if (!productIDs.equals("")){
                    try {
                        ArrayList<String> productIDsStringArray = new ArrayList<>(Arrays.asList(TextUtils.split(productIDs, ",")));
                        ArrayList<Integer> productIDsArray = new ArrayList<>();
                        for (String productID:productIDsStringArray){
                            productIDsArray.add(Integer.parseInt(productID.trim()));
                        }
                        intent.putExtra(CatalogContract.ProductsTable.TABLE_NAME,productIDsArray);
                    }catch (Exception e){

                    }

                }
                break;
            default:
                return;
        }

        intent.setClass(getApplicationContext(), activityClass);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDoublePressToExit = false;
        FilterClass.resetFilter();
        FilterClass.resetCategoryFilter();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            if (!mDoublePressToExit) {
                mDoublePressToExit = true;
                Toast.makeText(this, getString(R.string.back_press_message), Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDoublePressToExit = false;
                    }
                }, 2000);
            } else {
                finish();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.default_action_buttons, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_checkout:
                startActivity(new Intent(this, CartActivity.class));
                break;
            case R.id.action_bar_store_home:
                startActivity(new Intent(this, StoreActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
    }

    @Override
    public void openCategory(int categoryID) {
        if (categoryID != -1) {
            Intent intent = new Intent(this, CategoryProductActivity.class);
            intent.putExtra(getString(R.string.selected_category_id), categoryID);
            startActivity(intent);
        } else {
            openToFragment(CategoryGridFragment.class.getSimpleName(), null);
        }
    }

    @Override
    public void fragmentCreated(String title, boolean backEnabled) {
        mToolbar.setTitle(title);
        if (backEnabled && mToolbar.getNavigationContentDescription() != "backEnabled") {
            mToolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_left_white_32dp);
            mToolbar.setNavigationContentDescription("backEnabled");
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        } else if (!backEnabled && mToolbar.getNavigationContentDescription() != "default") {
            mToolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp);
            mToolbar.setNavigationContentDescription("default");
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void initToolbar() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set default toolbar as the action bar for this activity
        mToolbar = (Toolbar) findViewById(R.id.default_toolbar);
        setSupportActionBar(mToolbar);
    }

    private void initNavigationDrawer() {
        Fragment navDrawerFragment = new NavigationDrawerFragment();

        Bundle args = new Bundle();
        args.putString(getString(R.string.open_activity_key), this.getClass().getSimpleName());
        navDrawerFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.navigation_drawer_fragment, navDrawerFragment).commit();
    }

    private String getFragmentToOpenName(Bundle savedInstanceState) {
        String openFragment;
        if (savedInstanceState == null) {
            openFragment = getIntent().getStringExtra(Constants.OPEN_FRAGMENT_KEY);
        } else {
            openFragment = (String) savedInstanceState.getSerializable(Constants.OPEN_FRAGMENT_KEY);
        }
        if (openFragment == null) {
            openFragment = "";
        }
        return openFragment;
    }

    private void openToFragment(String fragmentName, @Nullable Bundle bundle) {
        Fragment fragment;

        if (fragmentName.equals(HomeFragment.class.getSimpleName())) {
            fragment = new HomeFragment();
        } else if (fragmentName.equals(CategoryGridFragment.class.getSimpleName())) {
            fragment = new CategoryGridFragment();
        } else {
            fragment = new HomeFragment();
        }

        fragment.setArguments(bundle);
        String backStateName = fragment.getClass().getName();
        FragmentManager fm = getSupportFragmentManager();

        boolean fragmentPopped = fm.popBackStackImmediate(backStateName, 0);

        if (!fragmentPopped) { // fragment not in backstack create it
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
            ft.addToBackStack(backStateName);
            ft.commit();
        }

    }
}
