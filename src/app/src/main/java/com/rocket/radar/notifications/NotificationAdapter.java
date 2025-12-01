package com.rocket.radar.notifications;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.admin.AdminModeManager;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the RecyclerView in the notifications screen.
 *
 * <p>This adapter binds {@link Notification} objects to views, separating them into
 * unread and previously read sections. It handles asynchronous image loading (with an
 * internal cache to prevent flickering), manages read/unread status updates, and
 * orchestrates shared element transitions when navigating to an event details view.</p>
 *
 * <p><strong>Outstanding Issues:</strong>
 * <ul>
 *   <li>The shared element transition logic is complex and duplicated between the Navigation Component
 *       path and the manual FragmentTransaction fallback. This should ideally be unified.</li>
 * </ul>
 * </p>
 */
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NOTIFICATION = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;
    private static final int VIEW_TYPE_EMPTY = 3;

    private final Context context;
    private final List<Notification> notificationList;
    private final NotificationRepository repository;
    private final EventRepository eventRepository;

    private final Map<String, Event> eventCache = new HashMap<>();

    private int lastClickedPosition = -1;
    private int separatorIndex = -1;

    private OnItemClickListener onItemClickListener;

    /**
     * Interface definition for a callback to be invoked when an item in this list has been clicked.
     */
    public interface OnItemClickListener {
        /**
         * Called when an item has been clicked.
         *
         * @param position The position of the item in the adapter.
         */
        void onItemClick(int position);
    }

    /**
     * Registers a callback to be invoked when an item in this RecyclerView has been clicked.
     *
     * @param listener The callback that will run.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * Constructs a new NotificationAdapter.
     *
     * @param context          The context in which the adapter is running.
     * @param notificationList The initial list of notifications to display.
     * @param repository       The repository used to update notification read status.
     * @param eventRepository  The repository used to fetch event details for images and navigation.
     */
    public NotificationAdapter(Context context,
                               List<Notification> notificationList,
                               NotificationRepository repository,
                               EventRepository eventRepository) {
        this.context = context;
        this.notificationList = notificationList;
        this.repository = repository;
        this.eventRepository = eventRepository;
    }

    /**
     * Updates the list of notifications displayed by the adapter.
     * This method recalculates the separator position for read/unread items and refreshes the view.
     *
     * @param newNotifications The new list of notifications.
     */
    public void setNotifications(List<Notification> newNotifications) {
        notificationList.clear();
        notificationList.addAll(newNotifications);
        calculateSeparatorIndex();
        notifyDataSetChanged();
    }

    /**
     * Populates the internal event cache with a bulk list of events.
     * This is typically called by the hosting Fragment after pre-fetching data to ensure
     * images load instantly without individual network calls during binding.
     *
     * @param events The list of Event objects to cache.
     */
    public void updateEventCache(List<Event> events) {
        for (Event event : events) {
            if (event != null && event.getEventId() != null) {
                eventCache.put(event.getEventId(), event);
            }
        }
        // Refresh view to apply cached images
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
        if (separatorIndex == -1 && !notificationList.isEmpty()) {
            separatorIndex = notificationList.size();
        }
    }

    /**
     * Returns the view type of the item at position for the purposes of view recycling.
     *
     * @param position position to query
     * @return integer value identifying the type of the view needed to represent the item at position.
     *         (VIEW_TYPE_NOTIFICATION, VIEW_TYPE_SEPARATOR, or VIEW_TYPE_EMPTY)
     */
    @Override
    public int getItemViewType(int position) {
        if (notificationList.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        }
        if (separatorIndex != -1 && position == separatorIndex) {
            return VIEW_TYPE_SEPARATOR;
        }
        return VIEW_TYPE_NOTIFICATION;
    }

    /**
     * Called when RecyclerView needs a new {@link RecyclerView.ViewHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = inflater.inflate(R.layout.notification_empty_state, parent, false);
            return new EmptyViewHolder(view);
        }
        if (viewType == VIEW_TYPE_SEPARATOR) {
            View view = inflater.inflate(R.layout.notification_separator, parent, false);
            return new SeparatorViewHolder(view);
        }
        View view = inflater.inflate(R.layout.event_notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link RecyclerView.ViewHolder#itemView} to reflect
     * the item at the given position, including handling image loading, text styling for read status,
     * and click listeners.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {

            case VIEW_TYPE_SEPARATOR:
                ((SeparatorViewHolder) holder).separatorText.setText("Previously Read");
                break;

            case VIEW_TYPE_NOTIFICATION:
                int listIndex = position;
                if (separatorIndex != -1 && position > separatorIndex) {
                    listIndex--;
                }

                NotificationViewHolder notificationHolder = (NotificationViewHolder) holder;

                if (listIndex < 0 || listIndex >= notificationList.size()) {
                    holder.itemView.setVisibility(View.GONE);
                    return;
                }
                holder.itemView.setVisibility(View.VISIBLE);

                Notification notification = notificationList.get(listIndex);

                // 1. Text Setup
                notificationHolder.eventTitle.setText(notification.getEventTitle());
                notificationHolder.notificationType.setText(notification.getNotificationType());

                // 2. Transition Names Setup
                String transitionId = notification.getEventId();
                if (transitionId != null && !transitionId.isEmpty()) {
                    androidx.core.view.ViewCompat.setTransitionName(notificationHolder.eventImage, "img_" + transitionId);
                    androidx.core.view.ViewCompat.setTransitionName(notificationHolder.eventTitle, "title_" + transitionId);
                }

                // 3. INSTANT IMAGE LOADING (Using Cache)
                notificationHolder.eventImage.setImageDrawable(null); // Reset first
                notificationHolder.eventImage.setTag(transitionId);

                boolean imageSetFromCache = false;

                // Check cache first
                if (transitionId != null && eventCache.containsKey(transitionId)) {
                    Event cachedEvent = eventCache.get(transitionId);
                    if (cachedEvent != null) {
                        if (cachedEvent.getBannerImageBitmap() != null) {
                            notificationHolder.eventImage.setImageBitmap(cachedEvent.getBannerImageBitmap());
                            imageSetFromCache = true;
                        } else {
                            // Set placeholder if event has no image
                            notificationHolder.eventImage.setImageResource(R.drawable.ic_radar);
                            imageSetFromCache = true;
                        }
                    }
                }

                // Fallback: If not in cache, load async (prevents blank rows if cache fails)
                if (!imageSetFromCache && transitionId != null && !transitionId.isEmpty()) {
                    eventRepository.getEventById(transitionId, new EventRepository.SingleEventListener() {
                        @Override
                        public void onEventLoaded(Event event) {
                            if (event != null && transitionId.equals(notificationHolder.eventImage.getTag())) {
                                if (event.getBannerImageBitmap() != null) {
                                    notificationHolder.eventImage.setImageBitmap(event.getBannerImageBitmap());
                                    // Add to cache for future scrolls
                                    eventCache.put(transitionId, event);
                                }
                            }
                        }
                        @Override
                        public void onError(Exception e) { }
                    });
                }

                // 4. Read Status Visuals
                AdminModeManager adminModeManager = AdminModeManager.getInstance(this.context);
                if (notification.isReadStatus() || adminModeManager.isAdminModeOn()) {
                    notificationHolder.unreadIndicator.setVisibility(View.GONE);
                    notificationHolder.eventTitle.setTypeface(null, Typeface.NORMAL);
                } else {
                    notificationHolder.unreadIndicator.setVisibility(View.VISIBLE);
                    notificationHolder.eventTitle.setTypeface(null, Typeface.BOLD);
                }

                // 5. Click Listener
                notificationHolder.itemView.setOnClickListener(v -> {
                    int clickedPos = holder.getBindingAdapterPosition();
                    lastClickedPosition = clickedPos;

                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(clickedPos);
                    }

                    if (!notification.isReadStatus()) {
                        repository.markNotificationAsRead(notification.getUserNotificationId());
                    }

                    if (transitionId == null || transitionId.isEmpty()) return;

                    // Check cache for navigation event data first
                    if (eventCache.containsKey(transitionId)) {
                        navigateToEvent(v, eventCache.get(transitionId), notificationHolder);
                    } else {
                        // Fetch if missing
                        eventRepository.getEventById(transitionId, new EventRepository.SingleEventListener() {
                            @Override
                            public void onEventLoaded(Event event) {
                                if (event != null) navigateToEvent(v, event, notificationHolder);
                            }
                            @Override
                            public void onError(Exception e) {}
                        });
                    }
                });
                break;
        }
    }

    private void navigateToEvent(View v, Event event, NotificationViewHolder holder) {
        if (event == null) return;
        if (!(context instanceof MainActivity)) return;

        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putSerializable("event", event);

        // --- NEW LOGIC: Check if current user is the organizer ---
        boolean isOrganizer = false;
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId != null && event.getOrganizerId() != null) {
            if (currentUserId.equals(event.getOrganizerId())) {
                isOrganizer = true;
            }
        }
        bundle.putBoolean("is_organizer", isOrganizer);
        // --------------------------------------------------------

        androidx.navigation.fragment.FragmentNavigator.Extras extras =
                new androidx.navigation.fragment.FragmentNavigator.Extras.Builder()
                        .addSharedElement(holder.eventImage, androidx.core.view.ViewCompat.getTransitionName(holder.eventImage))
                        .addSharedElement(holder.eventTitle, androidx.core.view.ViewCompat.getTransitionName(holder.eventTitle))
                        .build();

        try {
            androidx.navigation.Navigation.findNavController(v).navigate(
                    R.id.eventViewFragment,
                    bundle,
                    null,
                    extras
            );
        } catch (Exception e) {
            Log.e("NotificationAdapter", "Nav Component failed, falling back to manual", e);
            fallbackManualNavigation(event, holder);
        }
    }

    private void fallbackManualNavigation(Event event, NotificationViewHolder holder) {
        if (context instanceof MainActivity) {

            // --- NEW LOGIC: Check here as well for the manual fallback ---
            boolean isOrganizer = false;
            String currentUserId = FirebaseAuth.getInstance().getUid();
            if (currentUserId != null && event.getOrganizerId() != null) {
                if (currentUserId.equals(event.getOrganizerId())) {
                    isOrganizer = true;
                }
            }

            EventViewFragment fragment = EventViewFragment.newInstance(event, isOrganizer);

            android.transition.TransitionSet transitionSet = new android.transition.TransitionSet();
            transitionSet.addTransition(new android.transition.ChangeBounds());
            transitionSet.addTransition(new android.transition.ChangeTransform());
            transitionSet.addTransition(new android.transition.ChangeImageTransform());

            fragment.setSharedElementEnterTransition(transitionSet);
            fragment.setSharedElementReturnTransition(transitionSet);

            ((MainActivity) context).getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(holder.eventImage, androidx.core.view.ViewCompat.getTransitionName(holder.eventImage))
                    .addSharedElement(holder.eventTitle, androidx.core.view.ViewCompat.getTransitionName(holder.eventTitle))
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * This includes the actual notifications plus an optional separator item.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        int count = notificationList.size();
        if (separatorIndex != -1) count++;
        return count;
    }

    /**
     * Gets the position of the last item that was clicked.
     *
     * @return The adapter position of the last clicked item.
     */
    public int getLastClickedPosition() {
        return lastClickedPosition;
    }

    /**
     * ViewHolder for displaying a single notification item.
     * Holds references to the event image, title, notification type, and unread indicator.
     */
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

    /**
     * ViewHolder for displaying the separator between unread and read notifications.
     */
    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        TextView separatorText;
        public SeparatorViewHolder(@NonNull View itemView) {
            super(itemView);
            separatorText = itemView.findViewById(R.id.separator_text);
        }
    }

    /**
     * ViewHolder for displaying the empty state when there are no notifications.
     */
    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


}
