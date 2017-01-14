package com.wholdus.www.wholdusbuyerapp.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.wholdus.www.wholdusbuyerapp.R;
import com.wholdus.www.wholdusbuyerapp.interfaces.ItemClickListener;
import com.wholdus.www.wholdusbuyerapp.models.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.wholdus.www.wholdusbuyerapp.helperClasses.HelperFunctions.getDateFromString;

/**
 * Created by kaustubh on 11/1/17.
 */

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyViewHolder> {
    private ArrayList<Notification> mListData;
    private Context mContext;
    private ItemClickListener mListener;

    public NotificationAdapter(Context context, ArrayList<Notification> listData,final ItemClickListener listener){
        mContext = context;
        mListData = listData;
        mListener = listener;
    }

    @Override
    public int getItemCount() {
        return mListData.size();
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        Notification notification= mListData.get(position);
        holder.mNotificationTitle.setText(notification.getNotificationTitle());
        holder.mNotificationBody.setText(notification.getNotificationBody());
        holder.mNotificationDate.setText(notification.getNotificationTime());
        holder.mListener = mListener;

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_layout_notification, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView mNotificationTitle;
        private TextView mNotificationBody;
        private TextView mNotificationDate;
        private ItemClickListener mListener;

        private MyViewHolder(final View itemView) {
            super(itemView);
            mNotificationTitle = (TextView) itemView.findViewById(R.id.notification_title_text_view);
            mNotificationBody = (TextView) itemView.findViewById(R.id.notification_body_text_view);
            mNotificationDate = (TextView) itemView.findViewById(R.id.notification_date_text_view);
            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {

            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            if (mListener != null) {
                mListener.itemClicked(view, position, -1);
            }

        }
    }
}