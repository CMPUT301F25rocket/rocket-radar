package com.rocket.radar.notifications;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rocket.radar.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NOTIFICATION = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;

    private final Context context;
    private final List<Notification> notificationList;
    private final NotificationRepository repository;

    private int separatorIndex = -1; // The position where the separator should be

    public NotificationAdapter(Context context, List<Notification> notificationList, NotificationRepository repository) {
        this.context = context;
        this.notificationList = notificationList;
        this.repository = repository;
    }

    public void setNotifications(List<Notification> newNotifications) {
        notificationList.clear();
        notificationList.addAll(newNotifications);
        calculateSeparatorIndex();
        notifyDataSetChanged();
    }

    private void calculateSeparatorIndex() {
        separatorIndex = -1;
        // Find the index of the first item that is marked as read
        for (int i = 0; i < notificationList.size(); i++) {
            if (notificationList.get(i).isReadStatus()) {
                separatorIndex = i;
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        // If a separator is needed and the current position is its calculated index, return separator type
        if (separatorIndex != -1 && position == separatorIndex) {
            return VIEW_TYPE_SEPARATOR;
        }
        return VIEW_TYPE_NOTIFICATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_SEPARATOR) {
            View view = inflater.inflate(R.layout.notification_separator, parent, false);
            return new SeparatorViewHolder(view);
        }
        // Otherwise, it's a normal notification item
        View view = inflater.inflate(R.layout.event_notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // If it's a separator, configure its text
        if (holder.getItemViewType() == VIEW_TYPE_SEPARATOR) {
            SeparatorViewHolder separatorHolder = (SeparatorViewHolder) holder;
            // If the separator is at the top (index 0), it means there are no new notifications
            if (separatorIndex == 0) {
                separatorHolder.separatorText.setText("No New Notifications");
            } else {
                separatorHolder.separatorText.setText("Previously Read");
            }
            return;
        }

        // Adjust index to account for the separator's presence in the list
        int listIndex = position;
        if (separatorIndex != -1 && position > separatorIndex) {
            listIndex = position - 1;
        }

        NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;
        Notification notification = notificationList.get(listIndex);

        notificationHolder.eventTitle.setText(notification.getEventTitle());
        notificationHolder.notificationType.setText(notification.getNotificationType());
        // You can add image loading logic here, e.g., with Glide or Picasso
        // notificationHolder.eventImage.setImageResource(notification.getImage());

        // Differentiate read/unread items visually
        if (notification.isReadStatus()) {
            notificationHolder.unreadIndicator.setVisibility(View.GONE);
            notificationHolder.eventTitle.setTypeface(null, Typeface.NORMAL);
        } else {
            notificationHolder.unreadIndicator.setVisibility(View.VISIBLE);
            notificationHolder.eventTitle.setTypeface(null, Typeface.BOLD);
        }

        // Set click listener to mark the item as read
        notificationHolder.itemView.setOnClickListener(v -> {
            if (!notification.isReadStatus()) {
                repository.markNotificationAsRead(notification.getUserNotificationId());
            }
        });
    }

    @Override
    public int getItemCount() {
        int listSize = notificationList.size();
        // If a separator is present, the total item count is the list size + 1
        if (listSize > 0 && separatorIndex != -1) {
            return listSize + 1;
        }
        return listSize;
    }

    // ViewHolder for the notification item
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView eventTitle, notificationType;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            notificationType = itemView.findViewById(R.id.update_details_text);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }

    // ViewHolder for the separator
    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView separatorText;
        public SeparatorViewHolder(@NonNull View itemView) {
            super(itemView);
            separatorText = itemView.findViewById(R.id.separator_text);
        }
    }
}
