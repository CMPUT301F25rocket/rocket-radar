package com.rocket.radar.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.R;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Fragment that displays a list of notifications for the current user.
 *
 * <p>This fragment observes a LiveData stream from {@link NotificationRepository} to
 * show real-time updates (like event invitations or lottery wins). It implements a
 * "bulk fetch" mechanism to pre-load {@link Event} data for all notifications before
 * displaying them, ensuring that images and titles are available immediately to the adapter.</p>
 *
 * <p><strong>Outstanding Issues:</strong>
 * <ul>
 *   <li>The bulk fetch logic in {@link #observeNotifications()} is nested and slightly complex;
 *       it could be refactored into a ViewModel or Repository method to separate data logic from the UI controller.</li>
 * </ul>
 * </p>
 */
public class NotificationFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;
    private Button backButton;

    private NotificationAdapter adapter;
    private NotificationRepository notificationRepository;
    private RecyclerView.AdapterDataObserver adapterObserver;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This implementation inflates the notification list layout and initializes UI references.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);
        notificationRepository = new NotificationRepository();
        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);
        return view;
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This method sets up the RecyclerView, click listeners, and begins observing data.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupClickListeners();
        observeNotifications();
    }

    private void setupRecyclerView() {
        EventRepository eventRepository = new EventRepository();

        adapter = new NotificationAdapter(
                getContext(),
                new ArrayList<>(),
                notificationRepository,
                eventRepository
        );

        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);

        adapterObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }
            void checkEmpty() {
                if (adapter.getItemCount() == 0) {
                    emptyNotificationsTextView.setVisibility(View.VISIBLE);
                    notificationRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyNotificationsTextView.setVisibility(View.GONE);
                    notificationRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        };
        adapter.registerAdapterDataObserver(adapterObserver);
        checkEmpty();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void observeNotifications() {
        notificationRepository.getMyNotifications().observe(getViewLifecycleOwner(), newNotifications -> {
            Log.d("NotificationFragment", "Data updated. " + newNotifications.size() + " notifications.");

            newNotifications.sort((n1, n2) -> {
                int readCompare = Boolean.compare(n1.isReadStatus(), n2.isReadStatus());
                if (readCompare != 0) return readCompare;
                if (n1.getTimestamp() != null && n2.getTimestamp() != null) {
                    return n2.getTimestamp().compareTo(n1.getTimestamp());
                }
                return 0;
            });

            // 1. PRE-FETCH: Identify all Event IDs needed
            List<String> eventIdsToFetch = new ArrayList<>();
            for (Notification n : newNotifications) {
                if (n.getEventId() != null && !eventIdsToFetch.contains(n.getEventId())) {
                    eventIdsToFetch.add(n.getEventId());
                }
            }

            if (eventIdsToFetch.isEmpty()) {
                adapter.setNotifications(newNotifications);
                return;
            }

            // 2. Bulk Fetch Events
            EventRepository eventRepo = new EventRepository();
            List<Event> loadedEvents = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(eventIdsToFetch.size());

            for (String id : eventIdsToFetch) {
                eventRepo.getEventById(id, new EventRepository.SingleEventListener() {
                    @Override
                    public void onEventLoaded(Event event) {
                        if (event != null) {
                            loadedEvents.add(event);
                        }
                        checkCompletion();
                    }

                    @Override
                    public void onError(Exception e) {
                        checkCompletion();
                    }

                    private void checkCompletion() {
                        if (counter.decrementAndGet() == 0) {
                            // All events fetched!
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // 3. Update Adapter with Cache AND Notifications
                                    adapter.updateEventCache(loadedEvents);
                                    adapter.setNotifications(newNotifications);
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    private void checkEmpty() {
        if (adapter != null && emptyNotificationsTextView != null && notificationRecyclerView != null) {
            if (adapter.getItemCount() == 0) {
                emptyNotificationsTextView.setVisibility(View.VISIBLE);
                notificationRecyclerView.setVisibility(View.GONE);
            } else {
                emptyNotificationsTextView.setVisibility(View.GONE);
                notificationRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.
     * This implementation unregisters the data observer to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null && adapterObserver != null) {
            adapter.unregisterAdapterDataObserver(adapterObserver);
        }
    }
}
