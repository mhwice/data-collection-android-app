package mcmaster.ilos.datacollectionapp.Utils;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import mcmaster.ilos.datacollectionapp.R;

/* A view holder for the RecycleViews used in the Map Downloading and Trace Uploading activities */
public class DemoViewHolder extends RecyclerView.ViewHolder {

    public TextView title, sub_title;
    public ImageView imageView;
    public ProgressBar progressBar;

    public DemoViewHolder(View view) {
        super(view);
        this.title = view.findViewById(R.id.title);
        this.sub_title = view.findViewById(R.id.sub_title);
        this.imageView = view.findViewById(R.id.download_imageview);
        this.progressBar = view.findViewById(R.id.circle_progress_bar);
    }
}