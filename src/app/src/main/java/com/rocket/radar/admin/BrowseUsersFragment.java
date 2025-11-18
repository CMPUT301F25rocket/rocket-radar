package com.rocket.radar.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BrowseUsersFragment extends Fragment {

    private ListView usersListView;
    private UsersListAdapter adapter;
    private List<ProfileModel> users = new ArrayList<>();
    private AdminRepository adminRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_browse_users, container, false);
        usersListView = view.findViewById(R.id.usersListView);
        adapter = new UsersListAdapter(getContext(), users);
        usersListView.setAdapter(adapter);

        adminRepository = new AdminRepository();

        adminRepository.getAllUsers(result -> {
            users.clear();
            for (ProfileModel user : result) {
                Log.d("BrowseUsersFragment", "User: " + user.getName() + ", UID: " + user.getUid());
            }
            users.addAll(result);
            adapter.notifyDataSetChanged();
        });

        usersListView.setOnItemClickListener((parent, view1, position, id) -> {
            ProfileModel clickedUser = users.get(position);
            if (clickedUser == null || clickedUser.getUid() == null) {
                Log.w("BrowseUsersFragment", "Clicked user or UID is null, cannot navigate.");
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putSerializable("userProfile", clickedUser);

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_browse_users_to_user, bundle);
        });

        return view;
    }
}
