package com.example.dlibrary;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DownloadLogsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private TextView txtEmpty;
    private List<LogModel> logList = new ArrayList<>();
    private LogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_logs);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_download_logs);
        txtEmpty = findViewById(R.id.txt_empty_logs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadLogs();
    }

    private void loadLogs() {
        logList.clear();
        Cursor cursor = dbHelper.getAllDownloadLogs();

        if (cursor != null && cursor.moveToFirst()) {
            int nameIdx = cursor.getColumnIndex("user_name");
            int emailIdx = cursor.getColumnIndex("user_email");
            int roleIdx = cursor.getColumnIndex("user_role");
            int titleIdx = cursor.getColumnIndex("book_title");
            int dateIdx = cursor.getColumnIndex("download_date");

            do {
                String userName = nameIdx != -1 ? cursor.getString(nameIdx) : "User";
                String userEmail = emailIdx != -1 ? cursor.getString(emailIdx) : "";
                String userRole = roleIdx != -1 ? cursor.getString(roleIdx) : "User";
                String bookTitle = titleIdx != -1 ? cursor.getString(titleIdx) : "Resource";
                String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";

                logList.add(new LogModel(userName, userEmail, userRole, bookTitle, date));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (logList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new LogAdapter(logList);
            recyclerView.setAdapter(adapter);
        }
    }

    private static class LogModel {
        String userName, userEmail, userRole, bookTitle, date;

        LogModel(String userName, String userEmail, String userRole, String bookTitle, String date) {
            this.userName = userName;
            this.userEmail = userEmail;
            this.userRole = userRole;
            this.bookTitle = bookTitle;
            this.date = date;
        }
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

        private List<LogModel> list;

        LogAdapter(List<LogModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            LogModel item = list.get(position);
            holder.txtTitle.setText(item.bookTitle);
            holder.txtUserInfo.setText("Downloaded By: " + item.userName + " (" + item.userEmail + ")");
            holder.txtRole.setText("User Role: " + item.userRole);
            holder.txtDate.setText(item.date);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtUserInfo, txtRole, txtDate;

            LogViewHolder(@NonNull View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.log_book_title);
                txtUserInfo = itemView.findViewById(R.id.log_user_info);
                txtRole = itemView.findViewById(R.id.log_user_role);
                txtDate = itemView.findViewById(R.id.log_date);
            }
        }
    }
}
