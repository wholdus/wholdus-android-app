package com.wholdus.www.wholdusbuyerapp.helperClasses;

import android.text.TextUtils;
import android.util.Log;

import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by aditya on 12/12/16.
 */

public final class FilterClass {
    private FilterClass() {
    }

    public static final int MIN_PRICE_DEFAULT = 0;
    public static final int MAX_PRICE_DEFAULT = 5000;
    private static final String[] mSortString = {
            CatalogContract.ProductsTable.COLUMN_PRODUCT_ID + " DESC ",
            CatalogContract.ProductsTable.COLUMN_MIN_PRICE_PER_UNIT + " ASC ",
            CatalogContract.ProductsTable.COLUMN_MIN_PRICE_PER_UNIT + " DESC "
    };
    private static final String[] mSortServerString = {"latest", "price_ascending", "price_descending"};

    private static ArrayList<Integer> mCategoryIDs= new ArrayList<>();
    private static int mMinPrice = MIN_PRICE_DEFAULT;
    private static int mMaxPrice = MAX_PRICE_DEFAULT;
    private static int mSelectedSort = -1;
    private static HashSet<String> mFabrics = new HashSet<>();
    private static HashSet<String> mColors = new HashSet<>();
    private static HashSet<String> mSizes = new HashSet<>();
    private static HashSet<String> mBrands = new HashSet<>();
    private static boolean mFilterApplied = false;

    public static final String FILTER_FABRIC_KEY = "Fabric";
    public static final String FILTER_COLOUR_KEY = "Colors";
    public static final String FILTER_SIZE_KEY = "Sizes";
    public static final String FILTER_BRAND_KEY = "Brands";
    public static final String FILTER_CATEGORY_KEY = "Category";

    public static int getCategoryID() {
        if (mCategoryIDs.isEmpty()){
            return -1;
        } else {
            return mCategoryIDs.get(0);
        }
    }

    public static void setCategoryID(int id) {
        mCategoryIDs.clear();
        if (id > 0) {
            mCategoryIDs.add(id);
            mFilterApplied = true;
        }
    }

    public static void resetFilter() {
        mFabrics.clear();
        mColors.clear();
        mSizes.clear();
        mBrands.clear();
        mSelectedSort = -1;
        mMinPrice = MIN_PRICE_DEFAULT;
        mMaxPrice = MAX_PRICE_DEFAULT;
        mFilterApplied = false;
    }

    public static void resetCategoryFilter(){
        mCategoryIDs.clear();
    }

    public static void toggleCategoryID(int id) {
        if (id <= 0){
            return;
        }
        if (!mCategoryIDs.contains(id)) {
            mCategoryIDs.add(id);
        } else {
            mCategoryIDs.remove((Integer) id);
        }
    }

    public static boolean hasCategoryID(int id){
        return mCategoryIDs.contains(id);
    }

    public static ArrayList<Integer> getCategoryIDs(){
        if (mCategoryIDs.isEmpty()){
            return null;
        }
        return mCategoryIDs;
    }

    public static boolean isFilterApplied(){
        return mFilterApplied;
    }

    public static void resetFilter(String type) {
        getSelectedItems(type).clear();
    }

    public static void toggleFilterItem(String type, String value) {
        HashSet<String> object = getSelectedItems(type);
        if (object.contains(value)) {
            object.remove(value);
        } else {
            object.add(value);
        }
        if (!object.isEmpty()){
            mFilterApplied = true;
        }
    }

    public static HashSet<String> getSelectedItems(String type) {
        HashSet<String> returnValue;
        switch (type) {
            case FILTER_FABRIC_KEY:
                returnValue = mFabrics;
                break;
            case FILTER_COLOUR_KEY:
                returnValue = mColors;
                break;
            case FILTER_SIZE_KEY:
                returnValue = mSizes;
                break;
            case FILTER_BRAND_KEY:
                returnValue = mBrands;
                break;
            default:
                return null;
        }
        return returnValue;
    }

    public static boolean isItemSelected(String type, String value) {
        HashSet set = getSelectedItems(type);
        if (set == null){
            return false;
        }
        return set.contains(value);
    }

    public static void setPriceFilter(int min, int max) {
        mMaxPrice = max;
        mMinPrice = min;
        if (max != MAX_PRICE_DEFAULT || min != MIN_PRICE_DEFAULT){
            mFilterApplied = true;
        }
    }

    public static int getMinPriceFilter() {
        return mMinPrice;
    }

    public static int getMaxPriceFilter() {
        return mMaxPrice;
    }

    public static int getSelectSort() {
        return mSelectedSort;
    }

    public static void setSelectedSort(int sort) {
        mSelectedSort = sort;
    }

    public static String[] getSortString() {
        if (mSelectedSort == -1){
            return null;
        } else {
            return new String[]{mSortString[mSelectedSort]};
        }
    }

    public static String getSortServerString() {
        if (mSelectedSort == -1){
            return "none";
        } else {
            return mSortServerString[mSelectedSort];
        }
    }

    public static String getFilterString() {
        return GlobalAccessHelper.getUrlStringFromHashMap(getFilterHashMap());
    }

    public static HashMap<String, String> getFilterHashMap() {
        HashMap<String, String> params = new HashMap<>();
        if (mCategoryIDs.size() != 0) {
            params.put("categoryID", TextUtils.join(",", mCategoryIDs));
        }
        if (mBrands.size() != 0) {
            params.put("sellerID", TextUtils.join(",", mBrands));
        }
        if (mFabrics.size() != 0) {
            params.put("fabric", TextUtils.join(",", mFabrics));
        }
        if (mColors.size() != 0) {
            params.put("color", TextUtils.join(",", mColors));
        }
        if (mSizes.size() != 0) {
            params.put("sizes", TextUtils.join(",", mSizes));
        }
        if (mMaxPrice != 5000) {
            params.put("max_price_per_unit", String.valueOf(mMaxPrice));
        }
        if (mMinPrice != 0) {
            params.put("min_price_per_unit", String.valueOf(mMinPrice));
        }
        params.put("product_order_by", getSortServerString());

        return params;
    }
}
