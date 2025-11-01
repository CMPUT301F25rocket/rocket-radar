package com.rocket.radar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rocket.radar.profile.ProfileModel;
import java.util.List;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private final List<ProfileModel> entrants;

    public EntrantAdapter(Context context, List<ProfileModel> entrants) {
        this.entrants = entrants;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        ProfileModel currentEntrant = entrants.get(position);
        holder.bind(currentEntrant);
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * ViewHolder class for each entrant item.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;

        EntrantViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.entrant_name);
        }

        void bind(ProfileModel entrant) {
            nameTextView.setText(entrant.getName());
        }
    }
}
