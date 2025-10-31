package com.rocket.radar.notifications;

import android.content.Context;
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
    List<Notification> notificationList;

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.event_notification_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.MyViewHolder holder, int position) {
        // bind data to the rows in recycler view based on position
        holder.eventImage.setImageResource(notificationList.get(position).getImage());
        holder.eventTitle.setText(notificationList.get(position).getEventTitle());
        holder.notificationType.setText(notificationList.get(position).getNotificationType());
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
