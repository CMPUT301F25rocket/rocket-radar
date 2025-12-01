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

/**
 * RecyclerView adapter for displaying event banner images in a grid layout.
 * This adapter is used by the BrowseImageFragment to show all event images
 * to administrators for review and potential deletion.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    /**
     * ViewHolder that holds an ImageView for each event banner image.
     * Handles click and long-click events on individual images.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final ImageView imageView;
        private final ImageAdapter adapter;

        /**
         * Constructs a ViewHolder for an event image.
         * @param view The ImageView to display the event banner.
         * @param adapter The parent ImageAdapter instance.
         */
        public ViewHolder(ImageView view, ImageAdapter adapter) {
            super(view);
            imageView = view;
            imageView.setOnClickListener(this);
            imageView.setOnLongClickListener(this);
            this.adapter = adapter;
        }

        /**
         * Gets the ImageView contained in this ViewHolder.
         * @return The ImageView displaying the event banner.
         */
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

    /**
     * Constructs an ImageAdapter with a list of event banner images.
     * @param images The list of Bitmap images to display in the grid.
     */
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
        return new ViewHolder(imageView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageView imageView = holder.getImageView();
        imageView.setImageBitmap(images.get(position));
    }

    /**
     * Callback interface for handling item click events.
     */
    public interface ItemClickListener {
        /**
         * Called when an image item is clicked.
         * @param position The position of the clicked item.
         */
        void onClick(int position);
    }
    private ArrayList<ItemClickListener> clickListeners = new ArrayList<>();

    /**
     * Callback interface for handling item long-click events.
     */
    public interface ItemLongClickListener {
        /**
         * Called when an image item is long-clicked.
         * @param position The position of the long-clicked item.
         */
        void onItemLongClick(int position);
    }
    private ArrayList<ItemLongClickListener> longClickListeners = new ArrayList<>();

    /**
     * Registers a click listener for image items.
     * @param clickListener The listener to add.
     */
    public void addOnItemClickListener(ItemClickListener clickListener) {
        clickListeners.add(clickListener);
    }

    /**
     * Registers a long-click listener for image items.
     * @param longClickListener The listener to add.
     */
    public void addOnItemLongClickListener(ItemLongClickListener longClickListener) {
        longClickListeners.add(longClickListener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
