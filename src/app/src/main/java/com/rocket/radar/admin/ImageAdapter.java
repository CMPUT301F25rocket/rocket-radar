package com.rocket.radar.admin;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final ImageView imageView;
        private final ImageAdapter adapter;

        public ViewHolder(ImageView view, ImageAdapter adapter) {
            super(view);
            imageView = view;
            imageView.setOnClickListener(this);
            imageView.setOnLongClickListener(this);
            this.adapter = adapter;
        }

        public ImageView getImageView() {
            return imageView;
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                for (var listener : adapter.clickListeners) {
                    listener.onClick(position);
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                for (var listener : adapter.longClickListeners) {
                    listener.onItemLongClick(position);
                }
            }
            return false;
        }
    }

    List<Bitmap> images;

    public ImageAdapter(List<Bitmap> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        clickListeners = new ArrayList<>();
        longClickListeners = new ArrayList<>();
        // https://stackoverflow.com/questions/6180802/show-imageview-programmatically
        ImageView imageView = new ImageView(parent.getContext());
        int size = parent.getWidth() / 3;
        imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return new ViewHolder(imageView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageView imageView = holder.getImageView();
        imageView.setImageBitmap(images.get(position));
    }

    public interface ItemClickListener {
        void onClick(int position);
    }
    private ArrayList<ItemClickListener> clickListeners;

    public interface ItemLongClickListener {
        void onItemLongClick(int position);
    }
    private ArrayList<ItemLongClickListener> longClickListeners;

    public void addOnItemClickListener(ItemClickListener clickListener) {
        clickListeners.add(clickListener);
    }

    public void addOnItemLongClickListener(ItemLongClickListener longClickListener) {
        longClickListeners.add(longClickListener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
