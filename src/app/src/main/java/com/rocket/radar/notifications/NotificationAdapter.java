package com.rocket.radar.notifications;

import android.content.Context;import android.graphics.Typeface;
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

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- NEW VIEW TYPE ADDED ---
    private static final int VIEW_TYPE_NOTIFICATION = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;
    private static final int VIEW_TYPE_EMPTY = 3; // For the "No notifications" message

    private final Context context;
    private final List<Notification> notificationList;
    private final NotificationRepository repository;

    private int separatorIndex = -1;

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
        for (int i = 0; i < notificationList.size(); i++) {
            if (notificationList.get(i).isReadStatus()) {
                separatorIndex = i;
                return;
            }
        }
    }

    // --- MODIFIED ---
    @Override
    public int getItemViewType(int position) {
        // If the list is empty, we only show the empty view type.
        if (notificationList.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        }
        // Otherwise, use the existing logic.
        if (separatorIndex != -1 && position == separatorIndex) {
            return VIEW_TYPE_SEPARATOR;
        }
        return VIEW_TYPE_NOTIFICATION;
    }

    // --- MODIFIED ---
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        // Handle creating the new empty view holder
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = inflater.inflate(R.layout.notification_empty_state, parent, false);
            return new EmptyViewHolder(view);
        }
        if (viewType == VIEW_TYPE_SEPARATOR) {
            View view = inflater.inflate(R.layout.notification_separator, parent, false);
            return new SeparatorViewHolder(view);
        }
        // Default is the notification item
        View view = inflater.inflate(R.layout.event_notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    // --- MODIFIED ---
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Handle each view type


        // This is the original binding logic
        int listIndex = position;
        if (separatorIndex != -1 && position > separatorIndex) {
            listIndex--;
        }

        NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;

        Notification notification = notificationList.get(listIndex);

        notificationHolder.eventTitle.setText(notification.getEventTitle());
        notificationHolder.notificationType.setText(notification.getNotificationType());

        if (notification.isReadStatus()) {
            notificationHolder.unreadIndicator.setVisibility(View.GONE);
            notificationHolder.eventTitle.setTypeface(null, Typeface.NORMAL);
        } else {
            notificationHolder.unreadIndicator.setVisibility(View.VISIBLE);
            notificationHolder.eventTitle.setTypeface(null, Typeface.BOLD);
        }

        notificationHolder.itemView.setOnClickListener(v -> {
            if (!notification.isReadStatus()) {
                repository.markNotificationAsRead(notification.getUserNotificationId());
            }
        });
    }

    // --- MODIFIED ---
    @Override
    public int getItemCount() {
        // If the list is empty, we must return 1 to show our single "Empty" view.
        if (notificationList.isEmpty()) {
            return 1;
        }

        // Otherwise, use the corrected logic from before.
        if (separatorIndex > 0) {
            return notificationList.size() + 1;
        }

        return notificationList.size();
    }

    // --- VIEW HOLDERS (New one added) ---

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

    // --- NEW --- ViewHolder for the empty state message
    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
