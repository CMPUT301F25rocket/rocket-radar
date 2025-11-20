package com.rocket.radar.admin;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;

        public ViewHolder(ImageView view) {
            super(view);
            imageView = view;
        }

        public ImageView getImageView() {
            return imageView;
        }
    }

    List<Bitmap> images;

    public ImageAdapter(List<Bitmap> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // https://stackoverflow.com/questions/6180802/show-imageview-programmatically
        ImageView imageView = new ImageView(parent.getContext());
        int size = parent.getWidth() / 3;
        imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageView imageView = holder.getImageView();
        imageView.setImageBitmap(images.get(position));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
