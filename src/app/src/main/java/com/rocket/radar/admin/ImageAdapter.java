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
 * A RecyclerView.Adapter that displays a list of Bitmap images in a grid layout and
 * supports click and long-click interactions on each image item.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    /**
     * ViewHolder implementation for displaying a single image item.
     * Handles click and long-click events for the associated ImageView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final ImageView imageView;
        private final ImageAdapter adapter;

        /**
         * Creates a new ViewHolder for an ImageView and sets up click listeners.
         * @param view The ImageView representing the item.
         * @param adapter The ImageAdapter this ViewHolder is associated with.
         */
        public ViewHolder(ImageView view, ImageAdapter adapter) {
            super(view);
            imageView = view;
            imageView.setOnClickListener(this);
            imageView.setOnLongClickListener(this);
            this.adapter = adapter;
        }

        /**
         * @return The ImageView used to display the image for this item.
         */
        public ImageView getImageView() {
            return imageView;
        }

        /**
         * Handles a click event by notifying all registered click listeners.
         * @param v The clicked view.
         */
        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                for (var listener : adapter.clickListeners) {
                    listener.onClick(position);
                }
            }
        }

        /**
         * Handles a long-click event by notifying all registered long-click listeners.
         *
         * @param v The long-clicked view.
         * @return false to allow default long-click behavior to continue.
         */
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
     * Creates an ImageAdapter for displaying the given list of Bitmaps.
     *
     * @param images A list of Bitmaps to display in the RecyclerView.
     */
    public ImageAdapter(List<Bitmap> images) {
        this.images = images;
    }

    /**
     * Creates and returns a new ViewHolder for displaying an image item.
     *
     * @param parent The parent ViewGroup into which the new view will be added.
     * @param viewType The type of the new view (unused here).
     * @return A new ViewHolder instance.
     */
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

    /**
     * Binds a Bitmap image to the specified ViewHolder.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the image in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageView imageView = holder.getImageView();
        imageView.setImageBitmap(images.get(position));
    }

    /**
     * Interface for item click listener
     */
    public interface ItemClickListener {
        /**
         * Logic for when click occurs
         * @param position position where click occurs
         */
        void onClick(int position);
    }
    private ArrayList<ItemClickListener> clickListeners = new ArrayList<>();

    /**
     * Interface for item long click listener
     */
    public interface ItemLongClickListener {
        /**
         * Logic for when long click occurs
         * @param position position where long click occurs
         */
        void onItemLongClick(int position);
    }
    private ArrayList<ItemLongClickListener> longClickListeners = new ArrayList<>();

    /**
     * Registers a listener to handle click events on image items.
     * @param clickListener A listener that is notified when an item is clicked.
     */
    public void addOnItemClickListener(ItemClickListener clickListener) {
        clickListeners.add(clickListener);
    }

    /**
     * Registers a listener to handle long-click events on image items.
     * @param longClickListener A listener that is notified when an item is long-pressed.
     */
    public void addOnItemLongClickListener(ItemLongClickListener longClickListener) {
        longClickListeners.add(longClickListener);
    }

    /**
     * Returns the size of images.
     * @return The size of the images.
     */
    @Override
    public int getItemCount() {
        return images.size();
    }
}
