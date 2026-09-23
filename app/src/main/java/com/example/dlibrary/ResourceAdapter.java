package com.example.dlibrary;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {

    public interface OnResourceActionListener {
        void onItemClick(Resource resource);
        void onDownloadClick(Resource resource);
        void onDeleteClick(Resource resource);
    }

    private Context context;
    private List<Resource> resourceList;
    private OnResourceActionListener listener;
    private boolean showStatus;
    private boolean showDelete;

    public ResourceAdapter(Context context, List<Resource> resourceList, OnResourceActionListener listener) {
        this.context = context;
        this.resourceList = resourceList;
        this.listener = listener;
        this.showStatus = false;
        this.showDelete = false;
    }

    public ResourceAdapter(Context context, List<Resource> resourceList, boolean showStatus, boolean showDelete, OnResourceActionListener listener) {
        this.context = context;
        this.resourceList = resourceList;
        this.showStatus = showStatus;
        this.showDelete = showDelete;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_resource_card, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        Resource res = resourceList.get(position);

        holder.txtTitle.setText(res.getTitle());
        holder.txtAuthor.setText("By " + res.getAuthor() + " | Dept: " + res.getDepartment());
        holder.txtType.setText(res.getType());
        holder.txtCode.setText(res.getCourseCode().isEmpty() ? res.getCategory() : res.getCourseCode());
        holder.txtStats.setText("Downloads: " + res.getDownloadCount() + " • Favs: " + res.getFavoriteCount());

        DatabaseHelper db = new DatabaseHelper(context);
        SessionManager sm = new SessionManager(context);

        boolean isFav = db.isFavorite(sm.getEmail(), res.getId());
        if (holder.btnFavorite != null) {
            holder.btnFavorite.setColorFilter(isFav ? Color.parseColor("#D32F2F") : Color.parseColor("#CCCCCC"));
            holder.btnFavorite.setOnClickListener(v -> {
                boolean newFav = db.toggleFavorite(sm.getEmail(), res.getId());
                res.setFavoriteCount(newFav ? res.getFavoriteCount() + 1 : Math.max(0, res.getFavoriteCount() - 1));
                holder.btnFavorite.setColorFilter(newFav ? Color.parseColor("#D32F2F") : Color.parseColor("#CCCCCC"));
                holder.txtStats.setText("Downloads: " + res.getDownloadCount() + " • Favs: " + res.getFavoriteCount());
                Toast.makeText(context, newFav ? "Added to Favorites!" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            });
        }

        if (showStatus) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText(res.getApprovalStatus());
            if ("Approved".equalsIgnoreCase(res.getApprovalStatus())) {
                holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"));
            } else if ("Pending".equalsIgnoreCase(res.getApprovalStatus())) {
                holder.txtStatus.setTextColor(Color.parseColor("#E65100"));
            } else {
                holder.txtStatus.setTextColor(Color.RED);
            }
        } else {
            holder.txtStatus.setVisibility(View.GONE);
        }

        if (showDelete) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(res);
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Details Button ALWAYS opens ResourceDetailsActivity or calls onItemClick
        holder.btnView.setOnClickListener(v -> openDetails(res));
        holder.itemView.setOnClickListener(v -> openDetails(res));

        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(res);
            } else {
                db.incrementDownloadCount(res.getId(), sm.getEmail());
                res.setDownloadCount(res.getDownloadCount() + 1);
                notifyItemChanged(position);
                Toast.makeText(context, "Downloaded " + res.getTitle() + " (PDF/PPTX saved)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDetails(Resource res) {
        if (listener != null) {
            listener.onItemClick(res);
        }
        Intent intent = new Intent(context, ResourceDetailsActivity.class);
        intent.putExtra("resource_id", res.getId());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return resourceList.size();
    }

    public static class ResourceViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtAuthor, txtType, txtCode, txtStats, txtStatus;
        ImageButton btnFavorite;
        MaterialButton btnView, btnDownload, btnDelete;

        public ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_res_title);
            txtAuthor = itemView.findViewById(R.id.txt_res_author);
            txtType = itemView.findViewById(R.id.txt_res_type);
            txtCode = itemView.findViewById(R.id.txt_res_code);
            txtStats = itemView.findViewById(R.id.txt_res_stats);
            txtStatus = itemView.findViewById(R.id.txt_res_status);
            btnFavorite = itemView.findViewById(R.id.btn_card_favorite);
            btnView = itemView.findViewById(R.id.btn_action_view);
            btnDownload = itemView.findViewById(R.id.btn_action_download);
            btnDelete = itemView.findViewById(R.id.btn_action_delete);
        }
    }
}
