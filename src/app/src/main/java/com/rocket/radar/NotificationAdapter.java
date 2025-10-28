package com.rocket.radar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * An adapter for displaying a list of Notification objects in a RecyclerView.
 * This class is responsible for creating and binding the views for each notification item.
 * It supports different view types, although currently only one is implemented.
 * <p>
 * Author: Braden Woods
 */
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Define integer constants for each view type. This is a best practice for adapters
    // that need to display different kinds of layouts in the same list.
    private static final int VIEW_TYPE_EVENT = 1;
    // TODO: Add other view type constants here if new notification layouts are created.
    // e.g., private static final int VIEW_TYPE_GENERIC_MESSAGE = 2;

    // The data source for the adapter. This list holds all notifications to be displayed.
    private final List<Notification> notificationList;

    // A listener to handle click events on items. This follows the delegate pattern,
    // allowing the Fragment or Activity to decide what happens on a click.
    private final OnNotificationClickListener clickListener;

    /**
     * A public interface that the hosting UI component (like NotificationFragment) must implement
     * to receive click events from items in the RecyclerView.
     */
    public interface OnNotificationClickListener {
        /**
         * Called when a notification item is clicked.
         * @param notification The Notification object associated with the clicked item.
         * @param position The adapter position of the clicked item.
         */
        void onNotificationClicked(Notification notification, int position);
    }

    /**
     * Constructor for the NotificationAdapter.
     *
     * @param notificationList The list of notifications to display.
     * @param clickListener The listener that will handle item clicks, typically 'this' from the hosting fragment.
     */
    public NotificationAdapter(List<Notification> notificationList, OnNotificationClickListener clickListener) {
        this.notificationList = notificationList;
        this.clickListener = clickListener;
    }

    /**
     * Called by the RecyclerView to determine the view type for a given position.
     * This allows us to use different layouts for different notification types.
     */
    @Override
    public int getItemViewType(int position) {
        // Get the notification for the given position
        Notification notification = notificationList.get(position);

        // Use the safe enum getter from our model
        NotificationType type = notification.getTypeEnum();

        // A switch statement is a clean, scalable way to map enum types to view types.
        switch (type) {
            case EVENT_DETAILS:
            case WAITLIST_STATUS:
            case SELECTED_ENTRANTS:
            case CANCELLED_ENTRANTS:
                // All these types will currently use the same visual layout.
                return VIEW_TYPE_EVENT;

            case GENERIC_MESSAGE:
            default:
                // We can have a different layout for generic messages in the future,
                // but for now, we'll reuse the event layout.
                return VIEW_TYPE_EVENT;
        }
    }

    /**
     * Called by the RecyclerView when it needs a new ViewHolder to represent an item.
     * This method inflates the XML layout file and returns a new ViewHolder instance.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // We check the viewType passed by getItemViewType() to decide which layout to inflate.
        if (viewType == VIEW_TYPE_EVENT) {
            View view = inflater.inflate(R.layout.event_notification_item, parent, false);
            return new EventNotificationViewHolder(view);
        }

        // TODO: Add 'else if' blocks here for future view types.
        // For example:
        // else if (viewType == VIEW_TYPE_GENERIC_MESSAGE) {
        //     View view = inflater.inflate(R.layout.generic_notification_item, parent, false);
        //     return new GenericMessageViewHolder(view);
        // }

        // As a fallback, create an empty view to prevent the app from crashing if an
        // unknown viewType is encountered. This should ideally never be reached.
        return new RecyclerView.ViewHolder(new View(parent.getContext())) {};
    }

    /**
     * Called by the RecyclerView to display the data at a specified position.
     * This method fetches the correct data from the list and populates the ViewHolder's views.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Get the specific notification object for this position.
        Notification notification = notificationList.get(position);

        // Check the type of the ViewHolder to ensure we're binding to the correct one.
        if (holder instanceof EventNotificationViewHolder) {
            // Cast the generic ViewHolder to our specific EventNotificationViewHolder.
            EventNotificationViewHolder eventHolder = (EventNotificationViewHolder) holder;
            // Call the holder's bind method, passing the data and the click listener.
            eventHolder.bind(notification, clickListener);
        }
        // TODO: Add 'else if' blocks here for future ViewHolder types.
    }

    /**
     * Returns the total number of items in the data set.
     */
    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }


    // =================================================================================
    // ViewHolder for the 'event_notification_item.xml' layout
    // =================================================================================

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     * This specific holder manages the views inside the `event_notification_item.xml` layout.
     */
    public static class EventNotificationViewHolder extends RecyclerView.ViewHolder {
        // Declare the views that make up the item layout.
        private final ImageView eventBackgroundImage;
        private final TextView eventTitleText;
        private final TextView updateDetailsText;

        public EventNotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find each view in the layout. This is done only once when the holder is created,
            // which is much more efficient than calling findViewById() repeatedly.
            eventBackgroundImage = itemView.findViewById(R.id.event_background_image);
            eventTitleText = itemView.findViewById(R.id.event_title_text);
            updateDetailsText = itemView.findViewById(R.id.update_details_text);
        }

        /**
         * Binds a Notification object's data to the views in this ViewHolder.
         * This method is called from onBindViewHolder for each item.
         *
         * @param notification The notification object containing the data to display.
         * @param listener     The click listener to be attached to the item view.
         */
        public void bind(final Notification notification, final OnNotificationClickListener listener) {
            // 1. Populate the text views.
            eventTitleText.setText(notification.getTitle());
            updateDetailsText.setText(notification.getDescription());

            // 2. Set the click listener for the entire item.
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Pass the adapter position when an item is clicked
                    listener.onNotificationClicked(notification, getAdapterPosition());
                }
            });

            // 3. Load the image using Glide.
            String imageUrl = notification.getImageUrl();

            // Always check if the URL is valid before trying to load it.
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop() // Scales the image to fill the ImageView, cropping if necessary.
                        // TODO: Create a real placeholder and error drawable in your res/drawable folder.
                        .placeholder(R.drawable.ic_launcher_background) // Shown while the image is loading.
                        .error(R.drawable.ic_launcher_background)       // Shown if the image fails to load.
                        .into(eventBackgroundImage);
            } else {
                // If there's no image URL, set a default placeholder image directly.
                // This prevents Glide from trying to load a null or empty string.
                // TODO: Replace with a better default image.
                eventBackgroundImage.setImageResource(R.drawable.ic_launcher_background);
            }

            // 4. Set the visual state based on whether the notification has been read.
            // This is crucial for user experience.
            if (notification.isRead()) {
                // Make the item appear "faded" or "dimmed" if it has been read.
                itemView.setAlpha(0.6f);
            } else {
                // IMPORTANT: Always have an 'else' block to reset the state.
                // RecyclerView reuses views, so an item marked as read might be reused
                // for an unread item later. This ensures it's fully opaque again.
                itemView.setAlpha(1.0f);
            }
        }
    }
}
