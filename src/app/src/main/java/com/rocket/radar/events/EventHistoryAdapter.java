package com.rocket.radar.events;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;

import java.util.List;

public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.MyViewHolder> {
    Context context;
    List<Event> eventList;
    private ProfileModel currentUserProfile;
    private OnEventListener onEventListener;

    public EventHistoryAdapter(Context context, List<Event> eventList, ProfileModel currentUserProfile, OnEventListener onEventListener) {
        this.context = context;
        this.eventList = eventList;
        this.currentUserProfile = currentUserProfile;
        this.onEventListener = onEventListener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.event_history_item, parent, false);
        return new MyViewHolder(view, onEventListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Event event = eventList.get(position);

        // Display event banner image if available, otherwise use default resource
        if (event.getBannerImageBlob() != null) {
            Bitmap bannerBitmap = event.getBannerImageBitmap();
            if (bannerBitmap != null) {
                holder.eventImage.setImageBitmap(bannerBitmap);
            } else {
                holder.eventImage.setImageResource(event.getImage());
            }
        } else {
            holder.eventImage.setImageResource(event.getImage());
        }

        holder.eventTitle.setText(event.getEventTitle());
        holder.date.setText(event.getFormattedDate());
        holder.tagline.setText(event.getTagline());

        if (currentUserProfile != null) {
            String eventId = event.getEventId();
            String status = "Status: You did not attend."; // Default fallback

            // Priority check: Attending > Invited > Waitlisted
            // Since this is history, "Attending" usually means they were selected to go.

            if (currentUserProfile.getAttendingEventIds() != null && currentUserProfile.getAttendingEventIds().contains(eventId)) {
                // Option 1: They were on the final list
                status = "Status: You were selected for this event.";
            }
            else if (currentUserProfile.getOnInvitedEventIds() != null && currentUserProfile.getOnInvitedEventIds().contains(eventId)) {
                // Option 2: Invited (Selected) but maybe didn't accept/decline in time, or just stayed in invited state
                status = "Status: You were chosen to participate.";
            }
            else if (currentUserProfile.getOnWaitlistEventIds() != null && currentUserProfile.getOnWaitlistEventIds().contains(eventId)) {
                // Option 3: Still on waitlist when event ended
                status = "Status: You were not selected.";
            }

            holder.status.setText(status);
        } else {
            holder.status.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView eventImage;
        TextView eventTitle, tagline, date, status;
        OnEventListener onEventListener;

        public MyViewHolder(@NonNull View itemView, OnEventListener onEventListener) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            date = itemView.findViewById(R.id.date_text);
            tagline = itemView.findViewById(R.id.event_tagline_text);
            status = itemView.findViewById(R.id.event_status_text);

            this.onEventListener = onEventListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && onEventListener != null) {
                onEventListener.onEventClick(position, itemView);
            }
        }
    }

    public interface OnEventListener {
        void onEventClick(int position, View itemView);
    }
}
