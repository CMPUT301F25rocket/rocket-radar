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

/**
 * This is a basic adapter class for a list of profile models.
 * Used when the admin browses the users.
 */
public class UsersListAdapter extends ArrayAdapter<ProfileModel> {
    private Context context;
    private List<ProfileModel> users;

    /**
     * Constructor for the UsersListAdapter class
     * @param context The current context
     * @param users The users to adapt
     */
    public UsersListAdapter(@NonNull Context context, @NonNull List<ProfileModel> users) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

    /**
     * Creates or reuses the view for each user and binds it to the UI.
     * @param position The position of the item within the adapter's data set of the item whose view
     *        we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is
     *        always of the right type (see {@link #getViewTypeCount()} and
     *        {@link #getItemViewType(int)}).
     * @param parent The parent that this view will eventually be attached to
     * @return the converted view
     */
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

