package mcmaster.ilos.datacollectionapp.DownloadMaps;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;

import mcmaster.ilos.datacollectionapp.R;
import mcmaster.ilos.datacollectionapp.Utils.DemoViewHolder;

public class Download_RecyclerView_Adapter extends RecyclerView.Adapter<DemoViewHolder> implements Filterable {

    private Context context;
    private SparseBooleanArray mSelectedItemsIds;
    private ArrayList<Download_Item_Model> originalData;
    private ArrayList<Download_Item_Model> filteredData;

    public Download_RecyclerView_Adapter(Context context, ArrayList<Download_Item_Model> arrayList) {
        this.context = context;
        this.originalData = arrayList;
        this.filteredData = arrayList;

        mSelectedItemsIds = new SparseBooleanArray();
    }

    void updateDataset(ArrayList<Download_Item_Model> arrayList) {
        this.originalData = arrayList;
        this.filteredData = arrayList;
    }

    @Override
    public int getItemCount() {
        return (null != filteredData ? filteredData.size() : 0);
    }

    @Override
    public void onBindViewHolder(DemoViewHolder holder, int position) {
        holder.title.setText(filteredData.get(position).getTitle());

        if (filteredData.get(position).getDownloading()) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
            holder.sub_title.setText(R.string.downloading);

        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.sub_title.setText(filteredData.get(position).getSubTitle());
            if (filteredData.get(position).getDownloaded()) {
                holder.imageView.setVisibility(View.VISIBLE);
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
        }

        holder.title.setTextColor(mSelectedItemsIds.get(position) ? context.getResources().getColor(R.color.turq, null) : context.getResources().getColor(R.color.white, null));
        holder.sub_title.setTextColor(mSelectedItemsIds.get(position) ? context.getResources().getColor(R.color.turq, null) : context.getResources().getColor(R.color.white, null));
        holder.imageView.setColorFilter(mSelectedItemsIds.get(position) ? context.getResources().getColor(R.color.turq, null) : context.getResources().getColor(R.color.raspberry, null));
        holder.itemView.setBackgroundColor(mSelectedItemsIds.get(position) ? context.getResources().getColor(R.color.raspberry, null) : context.getResources().getColor(R.color.turq, null));
    }

    @Override
    public DemoViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater mInflater = LayoutInflater.from(viewGroup.getContext());
        ViewGroup mainGroup = (ViewGroup) mInflater.inflate(R.layout.item_row, viewGroup, false);
        return new DemoViewHolder(mainGroup);
    }

    void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    void selectView(int position, boolean value) {
        if (value) {
            mSelectedItemsIds.put(position, value);
        } else {
            mSelectedItemsIds.delete(position);
        }

        notifyDataSetChanged();
    }

    int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    public ArrayList<Download_Item_Model> getFilteredData() {
        return this.filteredData;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults results = new FilterResults();

                if (charSequence == null || charSequence.length() == 0) {
                    results.values = originalData;
                    results.count = originalData.size();
                }
                else {
                    ArrayList<Download_Item_Model> filterResultsData = new ArrayList();

                    for (Download_Item_Model data : originalData) {

                        String lowerData = data.getTitle().toLowerCase();
                        String lowerCharSequence = charSequence.toString().toLowerCase();
                        if (lowerData.contains(lowerCharSequence)) {
                            filterResultsData.add(data);
                        }
                    }

                    results.values = filterResultsData;
                    results.count = filterResultsData.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                /* This is just a convoluted way to cast 'filterResults.values' as an ArrayList<DownloadableMap> */
                Object filteredDataObject = filterResults.values;
                ArrayList<Download_Item_Model> mapList = new ArrayList<>();
                if (filteredDataObject instanceof ArrayList<?>) {
                    ArrayList<?> al = (ArrayList<?>) filteredDataObject;
                    if (al.size() > 0) {
                        for (int i = 0; i < al.size(); i++) {
                            Object o = al.get(i);
                            if (o instanceof Download_Item_Model) {
                                Download_Item_Model v = (Download_Item_Model) o;
                                mapList.add(v);
                            }
                        }
                    }
                }
                filteredData = mapList;
                notifyDataSetChanged();
            }
        };
    }
}