package com.rocket.radar.notifications;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.R;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyViewHolder> {
    Context context;
    private final List<Notification> notificationList;

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    public void setNotifications(List<Notification> newNotifications) {
        this.notificationList.clear();
        this.notificationList.addAll(newNotifications);
        Log.d("NotificationAdapter", "setNotifications: len " + notificationList.size());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.event_notification_item, parent, false);
        Log.d("NotificationAdapter", "onCreateViewHolder: created view " + view.toString());
        Log.d("NotificationAdapter", "onCreateViewHolder: created view with parent " + parent.toString());

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.MyViewHolder holder, int position) {
        // bind data to the rows in recycler view based on position
        Notification notification = notificationList.get(position);
        holder.eventImage.setImageResource(notification.getImage());
        holder.eventTitle.setText(notification.getEventTitle());
        holder.notificationType.setText(notification.getNotificationType());
    }

    @Override
    public int getItemCount() {
        // recycler view wants to know the number of items in the recycler view
        return notificationList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // grabs the view from our event_notification_item.xml file
        ImageView eventImage;
        TextView eventTitle, notificationType;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            notificationType = itemView.findViewById(R.id.update_details_text);

        }
    }
}
