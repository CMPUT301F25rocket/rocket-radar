package com.rocket.radar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    Context context;
    List<Event> eventList;

    public EventAdapter(Context context, List<Event> eventList) {
        this.context = context;
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.event_list_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventAdapter.MyViewHolder holder, int position) {
        // bind data to the rows in recycler view based on position
        holder.eventImage.setImageResource(eventList.get(position).getImage());
        holder.eventTitle.setText(eventList.get(position).getEventTitle());
        holder.date.setText(eventList.get(position).getDate());
        holder.tagline.setText(eventList.get(position).getTagline());


    }

    @Override
    public int getItemCount() {
        // recycler view wants to know the number of items in the recycler view
        return eventList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // grabs the view from our event_notification_item.xml file
        ImageView eventImage;
        TextView eventTitle, tagline, date;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            eventImage = itemView.findViewById(R.id.event_background_image);
            eventTitle = itemView.findViewById(R.id.event_title_text);
            date = itemView.findViewById(R.id.date_text);
            tagline = itemView.findViewById(R.id.event_tagline_text);

        }
    }
}