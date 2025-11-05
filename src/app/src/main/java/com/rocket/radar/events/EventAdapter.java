package com.rocket.radar.events;

import android.content.Context;
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

    // Update the constructor to accept the listener
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
        holder.eventImage.setImageResource(eventList.get(position).getImage());
        holder.eventTitle.setText(eventList.get(position).getEventTitle());
        holder.date.setText(eventList.get(position).getFormattedDate());
        holder.tagline.setText(eventList.get(position).getTagline());
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // Modify MyViewHolder to handle clicks
    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView eventImage;
        TextView eventTitle, tagline, date;
        OnEventListener onEventListener; // <- Add listener member

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

    // Define the click listener interface
    public interface OnEventListener {
        void onEventClick(int position);
    }
}
