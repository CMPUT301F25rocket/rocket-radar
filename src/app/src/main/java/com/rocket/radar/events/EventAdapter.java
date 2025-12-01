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

import java.util.List;

/**
 * RecyclerView adapter for displaying a list of events.
 * Each event is shown with its banner image, title, tagline, and formatted date.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    Context context;
    List<Event> eventList;
    private OnEventListener onEventListener;

    /**
     * Constructs an EventAdapter with event data and a click listener.
     * @param context The context for accessing resources.
     * @param eventList The list of events to display.
     * @param onEventListener The listener for handling event item clicks.
     */
    public EventAdapter(Context context, List<Event> eventList, OnEventListener onEventListener) {
        this.context = context;
        this.eventList = eventList;
        this.onEventListener = onEventListener;
    }

    @NonNull
    @Override
    public EventAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.event_list_item, parent, false);
        return new MyViewHolder(view, onEventListener);
    }

    @Override
    public void onBindViewHolder(@NonNull EventAdapter.MyViewHolder holder, int position) {
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
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * ViewHolder for event list items.
     * Holds references to the views for an event and handles click events.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView eventImage;
        TextView eventTitle, tagline, date;
        OnEventListener onEventListener;

        /**
         * Constructs a ViewHolder for an event item.
         * @param itemView The root view of the event list item layout.
         * @param onEventListener The listener to notify when the item is clicked.
         */
        public MyViewHolder(@NonNull View itemView, OnEventListener onEventListener) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            date = itemView.findViewById(R.id.date_text);
            tagline = itemView.findViewById(R.id.event_tagline_text);

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

    /**
     * Callback interface for handling event item click events.
     */
    public interface OnEventListener {
        /**
         * Called when an event item is clicked.
         * @param position The position of the clicked event in the adapter.
         * @param itemView The view that was clicked.
         */
        void onEventClick(int position, View itemView);
    }
}
