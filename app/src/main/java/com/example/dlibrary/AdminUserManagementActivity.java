package com.example.dlibrary;

import android.database.Cursor;
import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminUserManagementActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private FirebaseRepository firebaseRepo;
    private RecyclerView recyclerView;
    private List<UserModel> userList = new ArrayList<>();
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_management);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);
        firebaseRepo = FirebaseRepository.getInstance();

        recyclerView = findViewById(R.id.recycler_users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadUsers();
    }

    private void loadUsers() {
        userList.clear();

        firebaseRepo.fetchAllUsersFromFirebase(new FirebaseRepository.UserListCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> listData) {
                userList.clear();
                for (Map<String, Object> map : listData) {
                    String name = (String) map.get("name");
                    String email = (String) map.get("email");
                    String role = (String) map.get("role");
                    String id = (String) map.get("student_id");
                    String dept = (String) map.get("department");
                    String status = (String) map.get("status");

                    userList.add(new UserModel(name, email, role, id, dept, status));
                }

                if (userList.isEmpty()) {
                    loadUsersFromLocalDb();
                } else {
                    adapter = new UserAdapter(userList);
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Exception e) {
                loadUsersFromLocalDb();
            }
        });
    }

    private void loadUsersFromLocalDb() {
        userList.clear();
        Cursor cursor = dbHelper.getAllUsers();
        if (cursor != null && cursor.moveToFirst()) {
            int nameIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME);
            int emailIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_EMAIL);
            int roleIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_ROLE);
            int idIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_STUDENT_ID);
            int deptIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_DEPARTMENT);
            int statusIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS);

            do {
                String name = nameIdx != -1 ? cursor.getString(nameIdx) : "";
                String email = emailIdx != -1 ? cursor.getString(emailIdx) : "";
                String role = roleIdx != -1 ? cursor.getString(roleIdx) : "";
                String studentId = idIdx != -1 ? cursor.getString(idIdx) : "";
                String dept = deptIdx != -1 ? cursor.getString(deptIdx) : "";
                String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Active";

                userList.add(new UserModel(name, email, role, studentId, dept, status));
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);
    }

    private static class UserModel {
        String name, email, role, id, dept, status;

        UserModel(String name, String email, String role, String id, String dept, String status) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.id = id != null ? id : "N/A";
            this.dept = dept != null ? dept : "";
            this.status = status != null ? status : "Active";
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private List<UserModel> list;

        UserAdapter(List<UserModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_management, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserModel user = list.get(position);
            holder.txtName.setText(user.name);
            holder.txtEmail.setText(user.email);
            holder.txtInfo.setText("Role: " + user.role + " | Info/ID: " + (user.id.isEmpty() ? "N/A" : user.id) + " | Dept: " + user.dept);

            if (user.email != null && (user.email.equalsIgnoreCase("admin@library.duet.ac.bd") || user.email.equalsIgnoreCase("admin@duet.ac.bd"))) {
                holder.txtStatus.setText("PROTECTED SUPER ADMIN");
                holder.txtStatus.setTextColor(Color.parseColor("#FF8F00"));
                holder.btnToggleStatus.setVisibility(View.GONE);
                holder.btnDeleteUser.setVisibility(View.GONE);
                return;
            }

            holder.btnToggleStatus.setVisibility(View.VISIBLE);
            holder.btnDeleteUser.setVisibility(View.VISIBLE);
            holder.txtStatus.setText(user.status);

            if ("Blocked".equalsIgnoreCase(user.status)) {
                holder.txtStatus.setTextColor(Color.RED);
                holder.btnToggleStatus.setText("Unblock User");
            } else {
                holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"));
                holder.btnToggleStatus.setText("Block User");
            }

            holder.btnToggleStatus.setOnClickListener(v -> {
                String newStatus = "Blocked".equalsIgnoreCase(user.status) ? "Active" : "Blocked";
                dbHelper.updateUserStatus(user.email, newStatus);
                firebaseRepo.updateUserStatusInFirebase(user.email, newStatus, null);

                user.status = newStatus;
                notifyItemChanged(position);
                Toast.makeText(AdminUserManagementActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            });

            holder.btnDeleteUser.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    new AlertDialog.Builder(AdminUserManagementActivity.this)
                            .setTitle("Delete User")
                            .setMessage("Are you sure you want to delete user " + user.email + "?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (dbHelper.deleteUser(user.email)) {
                                    list.remove(adapterPos);
                                    notifyItemRemoved(adapterPos);
                                    notifyItemRangeChanged(adapterPos, list.size());
                                    Toast.makeText(AdminUserManagementActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtEmail, txtInfo, txtStatus;
            MaterialButton btnToggleStatus, btnDeleteUser;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txt_user_name);
                txtEmail = itemView.findViewById(R.id.txt_user_email);
                txtInfo = itemView.findViewById(R.id.txt_user_info);
                txtStatus = itemView.findViewById(R.id.txt_user_status);
                btnToggleStatus = itemView.findViewById(R.id.btn_toggle_status);
                btnDeleteUser = itemView.findViewById(R.id.btn_delete_user);
            }
        }
    }
}
