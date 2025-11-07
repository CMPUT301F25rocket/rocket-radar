/**
 * EventAdapter is a custom RecyclerView.Adapter that takes a list of Event objects
 * and binds them to the `event_list_item` layout. It's responsible for creating
 * ViewHolders for each item and populating them with the event's data.
 *
 * This adapter also implements an OnEventListener interface to handle click events on
 * individual items, delegating the action to the hosting Fragment or Activity.
 */
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

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    Context context;
    List<Event> eventList;
    private OnEventListener onEventListener; // <- Add listener member

    /**
     * Constructs the EventAdapter.
     *
     * @param context         The context from which the adapter is created.
     * @param eventList       The list of Event objects to display.
     * @param onEventListener The listener that will handle item clicks.
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
        // Pass listener to ViewHolder
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
                // Fallback to default image if bitmap conversion fails
                holder.eventImage.setImageResource(event.getImage());
            }
        } else {
            // Use default image resource if no banner blob exists
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
     * A ViewHolder that describes an item view and metadata about its place within the RecyclerView.
     * It also implements View.OnClickListener to handle clicks on each item.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView eventImage;
        TextView eventTitle, tagline, date;
        OnEventListener onEventListener; // <- Add listener member

        /**
         * Constructs the MyViewHolder.
         *
         * @param itemView        The view for a single list item.
         * @param onEventListener The listener to be notified of click events.
         */
        public MyViewHolder(@NonNull View itemView, OnEventListener onEventListener) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            date = itemView.findViewById(R.id.date_text);
            tagline = itemView.findViewById(R.id.event_tagline_text);

            this.onEventListener = onEventListener;
            itemView.setOnClickListener(this); // Set the click listener on the whole item
        }

        @Override
        public void onClick(View v) {
            onEventListener.onEventClick(getAdapterPosition());
        }
    }

    /**
     * An interface for receiving click events from items in the RecyclerView.
     * The hosting Activity or Fragment must implement this interface to respond
     * to user interactions.
     */
    public interface OnEventListener {
        /**
         * Called when a view has been clicked.
         * @param position The position of the clicked item in the adapter.
         */
        void onEventClick(int position);
    }
}
