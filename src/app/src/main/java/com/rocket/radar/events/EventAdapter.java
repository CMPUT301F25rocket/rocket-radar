package com.rocket.radar.events;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.R;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    Context context;
    List<Event> eventList;
    private OnEventListener onEventListener;

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

        // We append the event ID to the static name to make it unique for this specific row
        ViewCompat.setTransitionName(holder.eventImage, "img_" + event.getEventId());
        ViewCompat.setTransitionName(holder.eventTitle, "title_" + event.getEventId());
        ViewCompat.setTransitionName(holder.date, "date_" + event.getEventId());


        holder.eventTitle.setText(event.getEventTitle());
        holder.date.setText(event.getFormattedDate());
        holder.tagline.setText(event.getTagline());
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView eventImage;
        TextView eventTitle, tagline, date;
        OnEventListener onEventListener;

        public MyViewHolder(@NonNull View itemView, OnEventListener onEventListener) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            date = itemView.findViewById(R.id.date_text);
            tagline = itemView.findViewById(R.id.event_tagline_text);

            this.onEventListener = onEventListener;
            itemView.setOnClickListener(this);
        }

        // 2. Update the onClick method in MyViewHolder class
        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && onEventListener != null) {
                // Pass 'date' (which is already defined in your ViewHolder)
                onEventListener.onEventClick(position, itemView, eventImage, eventTitle, date);
            }
        }
    }

    // 1. Update the Interface definition at the bottom of the file
    public interface OnEventListener {
        // Add TextView dateView to the parameters
        void onEventClick(int position, View itemView, ImageView imageView, TextView titleView, TextView dateView);
    }
}
