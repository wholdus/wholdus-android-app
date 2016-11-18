package com.wholdus.www.wholdusbuyerapp.services;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.singletons.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by aditya on 12/11/16.
 */

public class UserAPIService extends BaseAPIService {

    private final String LOG_TAG = UserAPIService.class.getSimpleName();
    private final String NAME_KEY = "name";
    private final String MOBILE_NUMBER_KEY = "mobile_number";
    private final String PASSWORD_KEY = "password";
    private String REQUEST_TAG;

    private Context mContext;

    public UserAPIService(Context context) {
        mContext = context;
        REQUEST_TAG = mContext.getString(R.string.user_api_request_tag);
        super.setActivityCompat(mContext);
    }

    public void signup(String name, String mobileNumber, String password) {
        String endPoint = super.generateUrl(mContext.getString(R.string.signup_url));

        try {
            JSONObject userData = new JSONObject();
            userData.put(NAME_KEY, name);
            userData.put(MOBILE_NUMBER_KEY, mobileNumber);
            userData.put(PASSWORD_KEY, password);

            super.volleyStringRequest(Request.Method.POST, endPoint, userData.toString(), REQUEST_TAG);
        } catch (Exception e) {
            Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void login(String mobileNumber, String password) {
        String endPoint = super.generateUrl(mContext.getString(R.string.login_url));

        try {
            JSONObject loginData = new JSONObject();
            loginData.put(MOBILE_NUMBER_KEY, mobileNumber);
            loginData.put(PASSWORD_KEY, password);

            super.volleyStringRequest(Request.Method.POST, endPoint, loginData.toString(), REQUEST_TAG);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    public void forgotPassword(String mobileNumber) {
        String endPoint = super.generateUrl(mContext.getString(R.string.forgot_password_url));

        try {
            JSONObject forgotPasswordData = new JSONObject();
            forgotPasswordData.put(MOBILE_NUMBER_KEY, mobileNumber);
            super.volleyStringRequest(Request.Method.POST, endPoint, forgotPasswordData.toString(), REQUEST_TAG);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    public void cancelRequests() {
        VolleySingleton.getInstance(mContext).cancelPendingRequests(REQUEST_TAG);
    }

    public JSONObject parseLoginResponseData(String response) {

        try {
            JSONObject data = new JSONObject(response);
            return data;
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "Unable to parse login response json", Toast.LENGTH_LONG).show();
            return null;
        }
    }
}