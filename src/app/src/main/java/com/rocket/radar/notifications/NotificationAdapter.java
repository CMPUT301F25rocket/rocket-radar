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

/**
 * Adapter for the RecyclerView in the notifications screen.
 * This class is responsible for displaying a list of {@link Notification} objects.
 * It handles multiple view types:
 * - A standard notification item.
 * - A separator to distinguish between read and unread notifications.
 * - An empty state message for when there are no notifications.
 * It also manages the visual state for read/unread notifications.
 */
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- NEW VIEW TYPE ADDED ---
    private static final int VIEW_TYPE_NOTIFICATION = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;
    private static final int VIEW_TYPE_EMPTY = 3; // For the "No notifications" message

    private final Context context;
    private final List<Notification> notificationList;
    private final NotificationRepository repository;

    private int separatorIndex = -1;

    /**
     * Constructs a new NotificationAdapter.
     *
     * @param context The current context, used for inflating layouts.
     * @param notificationList The list of notifications to be displayed.
     * @param repository The repository to handle data operations, like marking notifications as read.
     */
    public NotificationAdapter(Context context, List<Notification> notificationList, NotificationRepository repository) {
        this.context = context;
        this.notificationList = notificationList;
        this.repository = repository;
    }

    /**
     * Updates the list of notifications displayed by the adapter.
     * This method clears the existing list, adds the new notifications, recalculates the
     * separator position, and refreshes the RecyclerView.
     *
     * @param newNotifications The new list of notifications to display.
     */
    public void setNotifications(List<Notification> newNotifications) {
        notificationList.clear();
        notificationList.addAll(newNotifications);
        calculateSeparatorIndex();
        notifyDataSetChanged();
    }

    /**
     * Calculates the position for the "Previously Read" separator.
     * The separator is placed just before the first read notification. If all notifications are
     * unread, the separator is placed at the end of the list. If there are no notifications,
     * the separator is not shown.
     */
    private void calculateSeparatorIndex() {
        separatorIndex = -1;
        for (int i = 0; i < notificationList.size(); i++) {
            if (notificationList.get(i).isReadStatus()) {
                separatorIndex = i;
                return;
            }
        }
        // This case handles when there are notifications, but all of them are unread.
        // In this scenario, we still want a separator, but it should be at the end of the list.
        if (separatorIndex == -1 && !notificationList.isEmpty()) {
            separatorIndex = notificationList.size();
        }
    }

    /**
     * Returns the view type for the item at the given position.
     *
     * @param position The position of the item within the adapter's data set.
     * @return An integer representing the view type.
     */
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
        switch (holder.getItemViewType()) {

            case VIEW_TYPE_SEPARATOR:
                ((SeparatorViewHolder) holder).separatorText.setText("Previously Read");
                break;

            case VIEW_TYPE_NOTIFICATION:
                // This is the original binding logic
                int listIndex = position;
                if (separatorIndex != -1 && position > separatorIndex) {
                    listIndex--;
                }

                NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;

                if (listIndex < 0 || listIndex >= notificationList.size()) {
                    Log.e("NotificationAdapter", "CRITICAL BUG: Invalid index. Position: " + position + ", ListIndex: " + listIndex);
                    holder.itemView.setVisibility(View.GONE);
                    return;
                }
                holder.itemView.setVisibility(View.VISIBLE);

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
                break;
        }
    }


    /**
     * Returns the total number of items in the data set held by the adapter.
     * This includes notifications and the separator, if present.
     *
     * @return The total number of items.
     */
    @Override
    public int getItemCount() {
        int count = notificationList.size();
        // If a separator exists (is not -1), we need one extra space for it.
        if (separatorIndex != -1) {
            count++;
        }
        return count;
    }


    /**
     * ViewHolder for a single notification item. Caches view references and binds data.
     */
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImage;
        TextView eventTitle, notificationType;
        View unreadIndicator;

        /**
         * Constructs the ViewHolder.
         * @param itemView The view for a single notification item.
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            notificationType = itemView.findViewById(R.id.update_details_text);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }

    /**
     * ViewHolder for the separator view, which displays a title like "Previously Read".
     */
    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView separatorText;
        public SeparatorViewHolder(@NonNull View itemView) {
            super(itemView);
            separatorText = itemView.findViewById(R.id.separator_text);
        }
    }

    /**
     * ViewHolder for the empty state view, displayed when there are no notifications.
     */
    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        /**
         * Constructs the ViewHolder for the empty state.
         * @param itemView The empty state view.
         */
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
