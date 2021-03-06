package com.wholdus.www.wholdusbuyerapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.activities.ProductDetailActivity;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract.UserAddressTable;
import com.wholdus.www.wholdusbuyerapp.helperClasses.InputValidationHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TrackingHelper;
import com.wholdus.www.wholdusbuyerapp.interfaces.UserAddressInterface;
import com.wholdus.www.wholdusbuyerapp.loaders.BuyerAddressLoader;
import com.wholdus.www.wholdusbuyerapp.loaders.ProfileLoader;
import com.wholdus.www.wholdusbuyerapp.models.Buyer;
import com.wholdus.www.wholdusbuyerapp.models.BuyerAddress;
import com.wholdus.www.wholdusbuyerapp.services.UserService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by aditya on 29/11/16.
 */

public class EditAddressFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<ArrayList<BuyerAddress>> {

    private UserAddressInterface mListener;
    private int mAddressID;
    private int m_ID;

    private TextInputLayout mPincodeWrapper, mMobileNumberWrapper, mAddressWrapper,
            mCityWrapper, mStateWrapper, mAliasWrapper;
    private TextInputEditText mPincodeEditText, mMobileNumberEditText,
            mAddressEditText, mCityEditText, mStateEditText, mLandmarkEditText, mAliasEditText;

    private TextView mSaveButton, mCurrentLocationButton;
    private ProgressBar mProgressBar;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationListener mLocationListener;
    private BuyerAddress mBuyerAddress;

    private static final int PERMISSION_ACCESS_FINE_LOCATION = 0;
    private int USER_ADDRESS_LOADER = 80;
    private final int USER_DB_LOADER = 81;

    private static final int MINIMUM_LOCATION_ACCURACY = 30;
    public static final int REQUEST_CHECK_SETTINGS = 0x1;

    private int mLocationCount = 0;

    public EditAddressFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (UserAddressInterface) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        mAddressID = arguments.getInt("addressID", -1);
        m_ID = arguments.getInt("_ID", -1);
        startLocationListener();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_edit_address, container, false);

        initReferences(rootView);

        if (mAddressID == -1 && m_ID == -1) {
            // Create new buyer model
            mBuyerAddress = new BuyerAddress();

        } else {
            getActivity().getSupportLoaderManager().restartLoader(USER_ADDRESS_LOADER, null, this);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TrackingHelper
                .getInstance(getContext())
                .logEvent(
                        FirebaseAnalytics.Event.VIEW_ITEM,
                        this.getClass().getSimpleName(),
                        "");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAddressID == -1 && m_ID == -1) {
            SharedPreferences shippingPreferences = getActivity().getSharedPreferences(ProductDetailActivity.SHIPPING_SHARED_PREFERENCES, MODE_PRIVATE);
            String pincode = shippingPreferences.getString(ProductDetailActivity.PINCODE_KEY, null);
            if (pincode != null) {
                mPincodeEditText.setText(pincode);
            }
            mAliasEditText.setText("Store");
            getActivity().getSupportLoaderManager().restartLoader(USER_DB_LOADER, null, new BuyerLoaderManager());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAddressID == -1 && m_ID == -1) {
            mListener.fragmentCreated("Add New Address", true);
        } else {
            mListener.fragmentCreated("Edit Address", true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    connectGoogleAPIClient();
                } else {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(getContext(), "Need your permission for location!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void connectGoogleAPIClient() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.reconnect();
        } else {
            startLocationListener();
            if (mGoogleApiClient != null) {
                mGoogleApiClient.reconnect();
            }
        }
    }

    private void startLocationListener() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext(), new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location != null && location.getAccuracy() < MINIMUM_LOCATION_ACCURACY) {

                        getAddressFromLocation(location);
                    } else {
                        startLocationRequest();
                    }
                }
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        }, new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                mProgressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getContext(), "Error google", Toast.LENGTH_SHORT).show();
            }
        }).addApi(LocationServices.API).build();
    }

    private void startLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates states = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        fetchLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    getActivity(),
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            mProgressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(getContext(), "Could not fetch location", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), "Could not fetch location", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        fetchLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), "Need your permission for location!", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), "Could not fetch location", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
        }
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mLocationListener == null) {
                mLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        mLocationCount += 1;
                        if (location.getAccuracy() < MINIMUM_LOCATION_ACCURACY || mLocationCount > 3) {
                            getAddressFromLocation(location);
                            if (mGoogleApiClient != null && mLocationListener != null && mGoogleApiClient.isConnected()) {
                                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
                            }
                        }
                    }
                };
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION);
        }

    }

    private void getAddressFromLocation(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                try {
                    final List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (addresses == null || addresses.size() == 0) {
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                    Toast.makeText(getContext(), "Could not fetch location", Toast.LENGTH_SHORT).show();
                                } else {
                                    final Address address = addresses.get(0);
                                    setDataFromLocation(address);
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(getContext(), "Could not fetch location", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setDataFromLocation(Address address) {
        //TODO : set buyer mobile number in mobile number
        if (address.getPostalCode() != null) {
            mPincodeEditText.setText(address.getPostalCode());
        }
        if (address.getLocality() != null) {
            mCityEditText.setText(address.getLocality());
        } else if (address.getSubAdminArea() != null) {
            mCityEditText.setText(address.getSubAdminArea());
        }
        if (address.getAdminArea() != null) {
            mStateEditText.setText(address.getAdminArea());
        }
        String addressText = "";

        if (address.getSubThoroughfare() != null) {
            addressText += address.getSubThoroughfare() + ", ";
        }
        if (address.getThoroughfare() != null) {
            addressText += address.getThoroughfare() + ", ";
        }
        if (address.getSubLocality() != null) {
            addressText += address.getSubLocality() + ", ";
        }
        if (address.getLocality() != null) {
            addressText += address.getLocality();
        }

        mAddressEditText.setText(addressText);

    }

    private void initReferences(ViewGroup rootView) {
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.loading_indicator);

        mPincodeWrapper = (TextInputLayout) rootView.findViewById(R.id.pincode_wrapper);
        mMobileNumberWrapper = (TextInputLayout) rootView.findViewById(R.id.mobile_number_wrapper);
        mAddressWrapper = (TextInputLayout) rootView.findViewById(R.id.address_wrapper);
        mCityWrapper = (TextInputLayout) rootView.findViewById(R.id.city_wrapper);
        mStateWrapper = (TextInputLayout) rootView.findViewById(R.id.state_wrapper);
        mAliasWrapper = (TextInputLayout) rootView.findViewById(R.id.alias_wrapper);

        mPincodeEditText = (TextInputEditText) rootView.findViewById(R.id.pincode_edit_text);
        mMobileNumberEditText = (TextInputEditText) rootView.findViewById(R.id.mobile_number_edit_text);
        mCityEditText = (TextInputEditText) rootView.findViewById(R.id.city_edit_text);
        mStateEditText = (TextInputEditText) rootView.findViewById(R.id.state_edit_text);
        mAddressEditText = (TextInputEditText) rootView.findViewById(R.id.address_edit_text);
        mLandmarkEditText = (TextInputEditText) rootView.findViewById(R.id.landmark_edit_text);
        mAliasEditText = (TextInputEditText) rootView.findViewById(R.id.alias_edit_text);

        mSaveButton = (TextView) rootView.findViewById(R.id.save_button);
        if (!(mAddressID == -1 && m_ID == -1)){
            mSaveButton.setText("UPDATE");
        }
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAddress();
            }
        });
        if (!(mAddressID == -1 && m_ID == -1)) {
            mSaveButton.setEnabled(false);
        }

        mCurrentLocationButton = (TextView) rootView.findViewById(R.id.current_location_button);
        mCurrentLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLocationCount = 0;
                mProgressBar.setVisibility(View.VISIBLE);
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_ACCESS_FINE_LOCATION);
                } else {
                    connectGoogleAPIClient();
                }
            }
        });
    }

    private void saveAddress() {
        try {
            JSONObject address = new JSONObject();
            address.put(UserAddressTable.COLUMN_ADDRESS_ALIAS, getDataFromView(UserAddressTable.COLUMN_ADDRESS_ALIAS));
            address.put(UserAddressTable.COLUMN_PINCODE, getDataFromView(UserAddressTable.COLUMN_PINCODE));
            address.put(UserAddressTable.COLUMN_CONTACT_NUMBER, getDataFromView(UserAddressTable.COLUMN_CONTACT_NUMBER));
            address.put(UserAddressTable.COLUMN_ADDRESS, getDataFromView(UserAddressTable.COLUMN_ADDRESS));
            address.put(UserAddressTable.COLUMN_LANDMARK, getDataFromView(UserAddressTable.COLUMN_LANDMARK));
            address.put(UserAddressTable.COLUMN_CITY, getDataFromView(UserAddressTable.COLUMN_CITY));
            address.put(UserAddressTable.COLUMN_STATE, getDataFromView(UserAddressTable.COLUMN_STATE));
            address.put(UserAddressTable.COLUMN_SYNCED, 0);
            address.put(UserAddressTable.COLUMN_CLIENT_ID, mBuyerAddress.getClientID());

            //TODO : Update loader parameters from address id to pincode if response received from server
            address.put(UserAddressTable.COLUMN_ADDRESS_ID, mAddressID);
            address.put(UserAddressTable.COLUMN_CREATED_AT, mBuyerAddress.getCreatedAt());
            address.put(UserAddressTable.COLUMN_UPDATED_AT, mBuyerAddress.getUpdatedAt());
            address.put(UserAddressTable.COLUMN_CLIENT_ID, mBuyerAddress.getClientID());
            address.put(UserAddressTable.COLUMN_PRIORITY, mBuyerAddress.getPriority());

            address.put(UserAddressTable._ID, mBuyerAddress.get_ID());

            Intent intent = new Intent(getContext(), UserService.class);
            intent.putExtra(UserAddressTable.TABLE_NAME, address.toString());
            intent.putExtra("TODO", R.string.update_user_address);

            getContext().startService(intent);

            mListener.addressSaved(mAddressID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDataFromView(String key) throws Exception {
        switch (key) {
            case UserAddressTable.COLUMN_CONTACT_NUMBER:
                String mobileNumber = mMobileNumberEditText.getText().toString();
                if (InputValidationHelper.isValidMobileNumber(mMobileNumberWrapper, mobileNumber)) {
                    return mobileNumber;
                } else throw new Exception();
            case UserAddressTable.COLUMN_PINCODE:
                String pincode = mPincodeEditText.getText().toString();
                if (InputValidationHelper.isValidPincode(mPincodeWrapper, pincode)) {
                    return pincode;
                } else throw new Exception();
            case UserAddressTable.COLUMN_ADDRESS:
                String address = mAddressEditText.getText().toString();
                if (InputValidationHelper.isNotEmpty(mAddressWrapper, address, "Please enter address")) {
                    return address;
                } else throw new Exception();
            case UserAddressTable.COLUMN_LANDMARK:
                return mLandmarkEditText.getText().toString();
            case UserAddressTable.COLUMN_ADDRESS_ALIAS:
                String alias = mAliasEditText.getText().toString();
                if (InputValidationHelper.isNotEmpty(mAliasWrapper, alias, "Please enter nickname for address")) {
                    return alias;
                } else throw new Exception();
            case UserAddressTable.COLUMN_CITY:
                String city = mCityEditText.getText().toString();
                if (InputValidationHelper.isNotEmpty(mCityWrapper, city, "Please enter city")) {
                    return city;
                } else throw new Exception();
            case UserAddressTable.COLUMN_STATE:
                String state = mStateEditText.getText().toString();
                if (InputValidationHelper.isNotEmpty(mStateWrapper, state, "Please enter state")) {
                    return state;
                } else throw new Exception();
            default:
                return "";
        }
    }

    private void setViewFromData(BuyerAddress address) {
        mAliasEditText.setText(address.getAlias());
        mMobileNumberEditText.setText(address.getContactNumber());
        mLandmarkEditText.setText(address.getLandmark());
        mCityEditText.setText(address.getCity());
        mStateEditText.setText(address.getState());
        mPincodeEditText.setText(address.getPincode());
        mAddressEditText.setText(address.getAddress());
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<BuyerAddress>> loader) {

    }

    @Override
    public void onLoadFinished(Loader<ArrayList<BuyerAddress>> loader, ArrayList<BuyerAddress> data) {
        if (data != null && data.size() > 0 && mListener != null){
            mProgressBar.setVisibility(View.INVISIBLE);
            mBuyerAddress = data.get(0);
            setViewFromData(mBuyerAddress);
            mSaveButton.setEnabled(true);
        }
    }

    @Override
    public Loader<ArrayList<BuyerAddress>> onCreateLoader(int id, Bundle args) {
        return new BuyerAddressLoader(getContext(), mAddressID, m_ID);
    }

    private class BuyerLoaderManager implements LoaderManager.LoaderCallbacks<Buyer> {
        @Override
        public void onLoaderReset(Loader<Buyer> loader) {

        }

        @Override
        public void onLoadFinished(Loader<Buyer> loader, Buyer data) {
            if (data != null && mMobileNumberEditText.getText().toString().equals("") && mListener != null) {
                mMobileNumberEditText.setText(data.getMobileNumber());
            }
        }

        @Override
        public Loader<Buyer> onCreateLoader(int id, Bundle args) {
            return new ProfileLoader(getContext(), true, false, false);
        }
    }

}
