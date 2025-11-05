package com.rocket.radar.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rocket.radar.R;
import java.util.List;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private final List<CheckIn> checkInList;
    private final OnEntrantClickListener listener;

    /**
     * Interface for handling clicks on an entrant in the list.
     */
    public interface OnEntrantClickListener {
        void onEntrantClick(CheckIn checkIn);
    }

    public EntrantAdapter(List<CheckIn> checkInList, OnEntrantClickListener listener) {
        this.checkInList = checkInList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        CheckIn checkIn = checkInList.get(position);
        holder.bind(checkIn, listener);
    }

    @Override
    public int getItemCount() {
        return checkInList.size();
    }

    /**
     * ViewHolder for displaying a single entrant's name.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView entrantNameTextView;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure you have a TextView with this ID in your item_entrant.xml layout
            entrantNameTextView = itemView.findViewById(R.id.entrant_name);
        }

        public void bind(final CheckIn checkIn, final OnEntrantClickListener listener) {
            // Display the user's name from the CheckIn object
            entrantNameTextView.setText(checkIn.getUserName());
            // Set a click listener on the entire item view
            itemView.setOnClickListener(v -> listener.onEntrantClick(checkIn));
        }
    }
}
