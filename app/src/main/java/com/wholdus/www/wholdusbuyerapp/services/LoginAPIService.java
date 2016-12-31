package com.wholdus.www.wholdusbuyerapp.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.crash.FirebaseCrash;
import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.databaseContracts.UserProfileContract;
import com.wholdus.www.wholdusbuyerapp.helperClasses.APIConstants;
import com.wholdus.www.wholdusbuyerapp.helperClasses.IntentFilters;
import com.wholdus.www.wholdusbuyerapp.helperClasses.LoginHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.OkHttpHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.TODO;

import org.json.JSONObject;

import okhttp3.Response;

/**
 * Created by aditya on 12/11/16.
 */

public class LoginAPIService extends IntentService {

    public LoginAPIService() {
        super("LoginAPIService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final int todo = intent.getIntExtra("TODO", -1);
        switch (todo) {
            case TODO.LOGIN:
                login(intent);
                break;
            case TODO.REGISTER:
                register(intent);
                break;
            case TODO.FORGOT_PASSWORD:
                forgotPassword();
                break;
            case TODO.RESEND_OTP:
                resendOTP(intent);
                break;
            case TODO.VERIFY_OTP:
                verifyOTP(intent);
        }
    }


    private void register(Intent intent) {
        try {
            JSONObject data = new JSONObject();
            data.put(UserProfileContract.UserTable.COLUMN_MOBILE_NUMBER,
                    intent.getStringExtra(UserProfileContract.UserTable.COLUMN_MOBILE_NUMBER));
            data.put(UserProfileContract.UserTable.COLUMN_NAME,
                    intent.getStringExtra(UserProfileContract.UserTable.COLUMN_NAME));
            data.put(UserProfileContract.UserTable.COLUMN_PASSWORD,
                    intent.getStringExtra(UserProfileContract.UserTable.COLUMN_PASSWORD));
            data.put(UserProfileContract.UserTable.COLUMN_EMAIL,
                    intent.getStringExtra(UserProfileContract.UserTable.COLUMN_EMAIL));

            Response response = OkHttpHelper.makePostRequest(getApplicationContext(),
                    OkHttpHelper.generateUrl(APIConstants.REGISTER_URL), data.toString());
            final int responseCode = response.code();

            switch (responseCode) {
                case 200:
                    JSONObject responseData = new JSONObject(response.body().string());
                    String registrationToken = responseData.getJSONObject("buyer_registration").getString(APIConstants.REGISTRATION_TOKEN_KEY);

                    sendBroadcast(TODO.REGISTER, registrationToken, responseCode);
                    break;
                case 400:
                    sendBroadcast(TODO.REGISTER, getString(R.string.already_registered_error), responseCode);
                    break;
                default:
                    sendBroadcast(TODO.REGISTER, getString(R.string.api_error_message), 500);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(intent.getIntExtra("TODO", -1), e.toString(), 500);
            FirebaseCrash.report(e);
        }
    }

    private void login(Intent intent) {
        try {
            JSONObject loginData = new JSONObject();

            loginData.put(UserProfileContract.UserTable.COLUMN_MOBILE_NUMBER,
                    intent.getStringExtra(UserProfileContract.UserTable.COLUMN_MOBILE_NUMBER));
            loginData.put(UserProfileContract.UserTable.COLUMN_PASSWORD,
                    intent.getStringExtra(UserProfileContract.UserTable.COLUMN_PASSWORD));

            Response response = OkHttpHelper.makePostRequest(getApplicationContext(),
                    OkHttpHelper.generateUrl(APIConstants.LOGIN_URL), loginData.toString());
            final int responseCode = response.code();
            final String responseBody = response.body().string();
            response.body().close();

            switch (responseCode) {
                case 200:
                    JSONObject data = new JSONObject(responseBody);
                    JSONObject buyerLogin = data.getJSONObject(APIConstants.BUYER_LOGIN_KEY);

                    LoginHelper loginHelper = new LoginHelper(this);
                    if (loginHelper.login(buyerLogin)) {
                        sendBroadcast(TODO.LOGIN, "success", responseCode);
                    } else {
                        sendBroadcast(TODO.LOGIN, null, responseCode);
                    }
                    break;
                case 401:
                    sendBroadcast(TODO.LOGIN, getString(R.string.invalid_credentials), responseCode);
                    break;
                case 403:
                    sendBroadcast(TODO.LOGIN, getString(R.string.unregistered_mobile_number), responseCode);
                    break;
                default:
                    sendBroadcast(TODO.LOGIN, getString(R.string.api_error_message), 500);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(TODO.LOGIN, e.toString(), 500);
            FirebaseCrash.report(e);
        }
    }

    private void forgotPassword() {
    }

    private void resendOTP(Intent intent) {
        try {
            JSONObject data = new JSONObject();
            data.put(APIConstants.REGISTRATION_TOKEN_KEY, intent.getStringExtra(APIConstants.REGISTRATION_TOKEN_KEY));

            Response response = OkHttpHelper.makePostRequest(getApplicationContext(),
                    OkHttpHelper.generateUrl(APIConstants.RESEND_OTP_URL), data.toString());
            final String responseBody = response.body().string();
            response.body().close();
            sendBroadcast(TODO.RESEND_OTP, responseBody, response.code());
        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(TODO.RESEND_OTP, e.toString(), 500);
            FirebaseCrash.report(e);
        }
    }

    private void verifyOTP(Intent intent) {
        try {
            JSONObject data = new JSONObject();
            data.put(APIConstants.REGISTRATION_TOKEN_KEY, intent.getStringExtra(APIConstants.REGISTRATION_TOKEN_KEY));
            data.put(APIConstants.OTP_NUMBER_KEY, intent.getStringExtra(APIConstants.OTP_NUMBER_KEY));

            Response response = OkHttpHelper.makePostRequest(getApplicationContext(),
                    OkHttpHelper.generateUrl(APIConstants.VERIFY_OTP_URL), data.toString());
            final int responseCode = response.code();
            final String responseBody = response.body().string();
            response.body().close();

            if (responseCode == 200) {
                JSONObject responseData = new JSONObject(responseBody);

                LoginHelper loginHelper = new LoginHelper(this);
                if (loginHelper.login(responseData)) {
                    sendBroadcast(TODO.VERIFY_OTP, "success", responseCode);
                } else {
                    sendBroadcast(TODO.VERIFY_OTP, null, responseCode);
                }
            } else {
                sendBroadcast(TODO.VERIFY_OTP, responseBody, responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(TODO.VERIFY_OTP, e.toString(), 500);
            FirebaseCrash.report(e);
        }
    }

    private void sendBroadcast(int todo, @Nullable String data, int responseCode) {
        Intent intent = new Intent(IntentFilters.LOGIN_SIGNUP_DATA);
        intent.putExtra("TODO", todo);
        intent.putExtra(APIConstants.RESPONSE_CODE, responseCode);
        intent.putExtra(APIConstants.LOGIN_API_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
