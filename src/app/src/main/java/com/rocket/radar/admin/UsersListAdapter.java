package com.rocket.radar.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;

import java.util.List;

public class UsersListAdapter extends ArrayAdapter<ProfileModel> {
    private Context context;
    private List<ProfileModel> users;

    public UsersListAdapter(@NonNull Context context, @NonNull List<ProfileModel> users) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_entrant, parent, false);
        }

        ProfileModel user = users.get(position);
        TextView nameTextView = convertView.findViewById(R.id.entrant_name);
        nameTextView.setText(user.getName());

        return convertView;
    }
}

