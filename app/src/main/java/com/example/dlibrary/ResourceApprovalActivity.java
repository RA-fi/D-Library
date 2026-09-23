package com.example.dlibrary;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceApprovalActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private FirebaseRepository firebaseRepo;

    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private final List<Resource> pendingList = new ArrayList<>();
    private ApprovalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_approval);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Pending Approvals Queue");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        firebaseRepo = FirebaseRepository.getInstance();

        recyclerView = findViewById(R.id.recycler_approval);
        txtEmpty = findViewById(R.id.txt_empty_approval);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadPendingResources();
    }

    private void loadPendingResources() {
        pendingList.clear();
        Cursor cursor = dbHelper.getPendingResources();

        if (cursor != null && cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID);
            int titleIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE);
            int authorIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_AUTHOR);
            int catIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_CATEGORY);
            int typeIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE);
            int fileIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_FILE_PATH);
            int codeIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_COURSE_CODE);
            int deptIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_RES_DEPARTMENT);
            int byIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_UPLOADED_BY);
            int statusIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_APPROVAL_STATUS);
            int dateIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_UPLOAD_DATE);
            int dlIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_DOWNLOAD_COUNT);
            int favIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_FAVORITE_COUNT);

            do {
                int id = idIdx != -1 ? cursor.getInt(idIdx) : 0;
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String author = authorIdx != -1 ? cursor.getString(authorIdx) : "";
                String cat = catIdx != -1 ? cursor.getString(catIdx) : "";
                String type = typeIdx != -1 ? cursor.getString(typeIdx) : "";
                String file = fileIdx != -1 ? cursor.getString(fileIdx) : "";
                String code = codeIdx != -1 ? cursor.getString(codeIdx) : "";
                String dept = deptIdx != -1 ? cursor.getString(deptIdx) : "";
                String uploader = byIdx != -1 ? cursor.getString(byIdx) : "";
                String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Pending";
                String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";
                int downloads = dlIdx != -1 ? cursor.getInt(dlIdx) : 0;
                int favorites = favIdx != -1 ? cursor.getInt(favIdx) : 0;

                pendingList.add(new Resource(id, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites));
            } while (cursor.moveToNext());
            cursor.close();
        }

        updateUI();

        // Also sync pending resources from Firebase Firestore
        firebaseRepo.fetchPendingResourcesFromFirebase(new FirebaseRepository.ResourceListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> firebaseResources) {
                if (firebaseResources != null) {
                    for (Map<String, Object> map : firebaseResources) {
                        String title = (String) map.get("title");
                        String author = (String) map.get("author");
                        String cat = (String) map.get("category");
                        String type = (String) map.get("type");
                        String file = (String) map.get("file_path");
                        String code = (String) map.get("course_code");
                        String dept = (String) map.get("res_department");
                        String uploader = (String) map.get("uploaded_by");

                        if (title != null && !title.isEmpty()) {
                            boolean exists = false;
                            for (Resource r : pendingList) {
                                if (r.getTitle().equalsIgnoreCase(title) && r.getUploadedBy().equalsIgnoreCase(uploader)) {
                                    exists = true;
                                    break;
                                }
                            }

                            if (!exists) {
                                dbHelper.addResource(title, author, cat, type, file, "", code, dept, uploader, "Pending");
                                Cursor newCursor = dbHelper.getPendingResources();
                                if (newCursor != null && newCursor.moveToFirst()) {
                                    int newId = newCursor.getInt(newCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                                    newCursor.close();
                                    pendingList.add(new Resource(newId, title, author, cat, type, file, code, dept, uploader, "Pending", "", 0, 0));
                                }
                            }
                        }
                    }
                }
                updateUI();
            }

            @Override
            public void onFailure(Exception e) {
                updateUI();
            }
        });
    }

    private void updateUI() {
        if (pendingList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (adapter == null) {
                adapter = new ApprovalAdapter(pendingList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private class ApprovalAdapter extends RecyclerView.Adapter<ApprovalAdapter.ApprovalViewHolder> {

        private final List<Resource> list;

        ApprovalAdapter(List<Resource> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ApprovalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resource_approval, parent, false);
            return new ApprovalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ApprovalViewHolder holder, int position) {
            Resource res = list.get(position);
            holder.txtTitle.setText(res.getTitle());
            holder.txtType.setText(res.getType());
            holder.txtCode.setText(res.getCourseCode().isEmpty() ? res.getCategory() : res.getCourseCode());
            holder.txtUploader.setText("By: " + res.getAuthor() + " (" + res.getUploadedBy() + ") | Dept: " + res.getDepartment());

            holder.btnApprove.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    if (dbHelper.updateResourceApproval(res.getId(), "Approved")) {
                        firebaseRepo.updateResourceApprovalInFirebase(res.getTitle(), res.getUploadedBy(), "Approved");

                        list.remove(adapterPos);
                        notifyItemRemoved(adapterPos);
                        notifyItemRangeChanged(adapterPos, list.size());
                        Toast.makeText(ResourceApprovalActivity.this, "Thesis/Resource Approved!", Toast.LENGTH_SHORT).show();
                        if (list.isEmpty()) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                }
            });

            holder.btnReject.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    if (dbHelper.updateResourceApproval(res.getId(), "Rejected")) {
                        firebaseRepo.updateResourceApprovalInFirebase(res.getTitle(), res.getUploadedBy(), "Rejected");

                        list.remove(adapterPos);
                        notifyItemRemoved(adapterPos);
                        notifyItemRangeChanged(adapterPos, list.size());
                        Toast.makeText(ResourceApprovalActivity.this, "Thesis/Resource Rejected", Toast.LENGTH_SHORT).show();
                        if (list.isEmpty()) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ApprovalViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtType, txtCode, txtUploader;
            MaterialButton btnApprove, btnReject;

            ApprovalViewHolder(@NonNull View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txt_app_title);
                txtType = itemView.findViewById(R.id.txt_app_type);
                txtCode = itemView.findViewById(R.id.txt_app_code);
                txtUploader = itemView.findViewById(R.id.txt_app_uploader);
                btnApprove = itemView.findViewById(R.id.btn_approve);
                btnReject = itemView.findViewById(R.id.btn_reject);
            }
        }
    }
}
