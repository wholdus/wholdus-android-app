package com.wholdus.www.wholdusbuyerapp.loaders;

import android.content.Context;
import android.database.Cursor;

import com.wholdus.www.wholdusbuyerapp.databaseContracts.CatalogContract;
import com.wholdus.www.wholdusbuyerapp.databaseHelpers.CatalogDBHelper;
import com.wholdus.www.wholdusbuyerapp.helperClasses.FilterClass;
import com.wholdus.www.wholdusbuyerapp.models.GridProductModel;

import java.util.ArrayList;

/**
 * Created by aditya on 15/12/16.
 */

public class GridProductsLoader extends AbstractLoader<ArrayList<GridProductModel>> {

    private static final String[] columns = {
            CatalogContract.ProductsTable._ID,
            CatalogContract.ProductsTable.COLUMN_NAME,
            CatalogContract.ProductsTable.COLUMN_PRODUCT_ID,
            CatalogContract.ProductsTable.COLUMN_MIN_PRICE_PER_UNIT,
            CatalogContract.ProductsTable.COLUMN_FABRIC_GSM,
            CatalogContract.ProductsTable.COLUMN_IMAGE_COUNT,
            CatalogContract.ProductsTable.COLUMN_IMAGE_NAME,
            CatalogContract.ProductsTable.COLUMN_IMAGE_NUMBERS,
            CatalogContract.ProductsTable.COLUMN_IMAGE_PATH,
            CatalogContract.ProductsTable.COLUMN_RESPONSE_CODE,
    };

    private ArrayList<Integer> mResponseCodes;

    public GridProductsLoader(Context context, ArrayList<Integer> responseCodes) {
        super(context);
        mResponseCodes = responseCodes;
    }

    @Override
    public ArrayList<GridProductModel> loadInBackground() {

        CatalogDBHelper catalogDBHelper = new CatalogDBHelper(getContext());

        ArrayList<Integer> categoryID = new ArrayList<>();
        int selectedCategory = FilterClass.getCategoryID();
        if (selectedCategory != -1) {
            categoryID.add(FilterClass.getCategoryID());
        }
        String[] sortString = FilterClass.getSortString();
        if (mResponseCodes.size() == 1 && sortString == null){
            sortString = new String[] {CatalogContract.ProductsTable.COLUMN_BUYER_PRODUCT_RESPONSE_UPDATED_AT + " DESC "};
        }
        Cursor cursor = catalogDBHelper.getProductData(null,
                null,
                null,
                null,
                null,
                null,
                FilterClass.getSelectedItems(FilterClass.FILTER_BRAND_KEY),
                categoryID,
                FilterClass.getMinPriceFilter(),
                FilterClass.getMaxPriceFilter(),
                FilterClass.getSelectedItems(FilterClass.FILTER_FABRIC_KEY),
                FilterClass.getSelectedItems(FilterClass.FILTER_COLOUR_KEY),
                FilterClass.getSelectedItems(FilterClass.FILTER_SIZE_KEY),
                mResponseCodes,
                0,
                1,
                -1,
                -1,
                sortString, // ORDER BY
                -1,
                -1,
                columns);
        return GridProductModel.getGridProducts(cursor);
    }
}
