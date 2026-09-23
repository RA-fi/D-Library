package com.example.dlibrary;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NoticesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private FirebaseRepository firebaseRepo;

    private View cardAdminPost;
    private TextInputEditText inputTitle, inputContent;
    private MaterialButton btnPost;
    private RecyclerView recyclerView;

    private final List<NoticeModel> noticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private ListenerRegistration liveListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📢 DUET Library Official Notices");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        firebaseRepo = FirebaseRepository.getInstance();

        cardAdminPost = findViewById(R.id.card_admin_post_notice);
        inputTitle = findViewById(R.id.input_notice_title);
        inputContent = findViewById(R.id.input_notice_content);
        btnPost = findViewById(R.id.btn_post_notice);
        recyclerView = findViewById(R.id.recycler_notices);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (sessionManager.isAdmin()) {
            cardAdminPost.setVisibility(View.VISIBLE);
            btnPost.setOnClickListener(v -> handlePostNotice());
        } else {
            cardAdminPost.setVisibility(View.GONE);
        }

        loadNotices();
        setupRealtimeNoticeListener();
    }

    private void handlePostNotice() {
        String title = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
        String content = inputContent.getText() != null ? inputContent.getText().toString().trim() : "";

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please enter title and content", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        btnPost.setText("Posting Notice...");

        publishNoticeToFirebase(title, content);
    }

    private void publishNoticeToFirebase(String title, String content) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        dbHelper.addNotice(title, content, sessionManager.getEmail());

        firebaseRepo.postNotice(title, content, "", sessionManager.getEmail(), date, new FirebaseRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                onNoticePostComplete(title, content);
            }

            @Override
            public void onFailure(Exception e) {
                onNoticePostComplete(title, content);
            }
        });
    }

    private void onNoticePostComplete(String title, String content) {
        btnPost.setEnabled(true);
        btnPost.setText("Publish Notice & Notify All Users");

        inputTitle.setText("");
        inputContent.setText("");

        NotificationHelper.showNoticeNotification(this, "NEW NOTICE: " + title, content);
        Toast.makeText(this, "Notice published & broadcasted to all users!", Toast.LENGTH_LONG).show();

        loadNotices();
    }

    private void setupRealtimeNoticeListener() {
        liveListenerRegistration = firebaseRepo.listenForLiveNotices((title, content, pdfUrl, postedBy, date) -> {
            if (title != null && !title.isEmpty()) {
                NotificationHelper.showNoticeNotification(NoticesActivity.this, title, content);
                loadNotices();
            }
        });
    }

    private void loadNotices() {
        noticeList.clear();
        Cursor cursor = dbHelper.getAllNotices();

        if (cursor != null && cursor.moveToFirst()) {
            int titleIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTICE_TITLE);
            int contentIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTICE_CONTENT);
            int postedByIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTICE_POSTED_BY);
            int dateIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTICE_DATE);

            do {
                String title = titleIdx != -1 ? cursor.getString(titleIdx) : "";
                String content = contentIdx != -1 ? cursor.getString(contentIdx) : "";
                String postedBy = postedByIdx != -1 ? cursor.getString(postedByIdx) : "";
                String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";

                noticeList.add(new NoticeModel(title, content, "", postedBy, date));
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter = new NoticeAdapter(noticeList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (liveListenerRegistration != null) {
            liveListenerRegistration.remove();
        }
    }

    private static class NoticeModel {
        String title, content, pdfUrl, postedBy, date;

        NoticeModel(String title, String content, String pdfUrl, String postedBy, String date) {
            this.title = title;
            this.content = content;
            this.pdfUrl = pdfUrl;
            this.postedBy = postedBy;
            this.date = date;
        }
    }

    private class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

        private final List<NoticeModel> list;

        NoticeAdapter(List<NoticeModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
            return new NoticeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
            NoticeModel item = list.get(position);
            holder.txtTitle.setText(item.title);
            holder.txtContent.setText(item.content);
            holder.txtPostedBy.setText("Posted By: " + item.postedBy);
            holder.txtDate.setText(item.date);

            View.OnClickListener readListener = v -> showFullNoticeDialog(item);
            holder.btnRead.setOnClickListener(readListener);
            holder.itemView.setOnClickListener(readListener);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class NoticeViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtContent, txtPostedBy, txtDate;
            MaterialButton btnRead;

            NoticeViewHolder(@NonNull View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txt_notice_title);
                txtContent = itemView.findViewById(R.id.txt_notice_content);
                txtPostedBy = itemView.findViewById(R.id.txt_notice_posted_by);
                txtDate = itemView.findViewById(R.id.txt_notice_date);
                btnRead = itemView.findViewById(R.id.btn_read_full_notice);
            }
        }
    }

    private void showFullNoticeDialog(NoticeModel notice) {
        new AlertDialog.Builder(this)
                .setTitle("📢 " + notice.title)
                .setMessage("Date: " + notice.date + "\nPosted By: " + notice.postedBy + "\n\n" + notice.content)
                .setPositiveButton("Close", null)
                .show();
    }
}
