package com.wholdus.www.wholdusbuyerapp.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions;
import com.wholdus.www.wholdusbuyerapp.interfaces.ProfileListenerInterface;
import com.wholdus.www.wholdusbuyerapp.models.Order;
import com.wholdus.www.wholdusbuyerapp.models.Suborder;

import java.util.ArrayList;

/**
 * Created by kaustubh on 8/12/16.
 */

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.MyViewHolder> {

    private ArrayList<Order> mListData;
    private Context mContext;
    private ProfileListenerInterface mProfileListener;

    public OrdersAdapter(Context context, ArrayList<Order> listData){
        mContext = context;
        mListData = listData;
    }

    @Override
    public int getItemCount() {
        return mListData.size();
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Order order = mListData.get(position);
        holder.orderID.setText(order.getDisplayNumber());
        holder.orderStatus.setText(order.getOrderStatusDisplay());
        holder.orderDate.setText(HelperFunctions.getDateFromString(order.getCreatedAt()));
        holder.orderAmount.setText(String.format("%.00f", order.getFinalPrice()));

        ArrayList<Suborder> suborders = order.getSuborders();
        SuborderListViewAdapter suborderAdapter = new SuborderListViewAdapter(mContext, suborders);
        holder.suborderListView.setAdapter(suborderAdapter);
        HelperFunctions.setListViewHeightBasedOnChildren(holder.suborderListView);
        /*
        final int adapterCount = suborderAdapter.getCount();
        LinearLayout layout = new LinearLayout(mContext);
        for (int i = 0; i < adapterCount; i++) {
            View item = suborderAdapter.getView(i, null, null);
            layout.addView(item);
        }*/

        //holder.suborderListView.addView(layout);

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_layout_orders, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView orderID;
        private TextView orderStatus;
        private TextView orderDate;
        private TextView orderAmount;
        private ListView suborderListView;
        private MyViewHolder(View itemView) {
            super(itemView);
            orderID = (TextView) itemView.findViewById(R.id.order_id_text_view);
            orderStatus = (TextView) itemView.findViewById(R.id.order_status_text_view);
            orderDate = (TextView) itemView.findViewById(R.id.order_date_text_view);
            orderAmount = (TextView) itemView.findViewById(R.id.order_amount_text_view);
            suborderListView = (ListView) itemView.findViewById(R.id.suborder_list_view);
        }

        @Override
        public void onClick(View view) {

            int orderID = mListData.get(getAdapterPosition()).getOrderID();

            if (mProfileListener != null){
                mProfileListener.openOrderDetails(orderID);
            }

        }
    }
}
