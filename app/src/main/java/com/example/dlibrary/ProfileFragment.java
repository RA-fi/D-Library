package com.example.dlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int REQUEST_PICK_PROFILE_IMAGE = 201;

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private FirebaseRepository firebaseRepo;

    private ImageView imgAvatar;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());
        dbHelper = new DatabaseHelper(requireContext());
        firebaseRepo = FirebaseRepository.getInstance();

        imgAvatar = view.findViewById(R.id.img_profile_avatar);
        View btnChangeAvatar = view.findViewById(R.id.btn_change_avatar);

        TextView nameTxt = view.findViewById(R.id.profile_name);
        TextView emailTxt = view.findViewById(R.id.profile_email);
        TextView roleTxt = view.findViewById(R.id.profile_role);
        TextView idTxt = view.findViewById(R.id.profile_student_id);
        TextView deptTxt = view.findViewById(R.id.profile_dept);

        updateProfileUI(nameTxt, emailTxt, roleTxt, idTxt, deptTxt);
        loadProfileImage();
        updateActivityBadges(view);

        View.OnClickListener avatarClickListener = v -> openImagePicker();
        imgAvatar.setOnClickListener(avatarClickListener);
        if (btnChangeAvatar != null) btnChangeAvatar.setOnClickListener(avatarClickListener);

        view.findViewById(R.id.edit_profile_btn).setOnClickListener(v -> showEditDialog(nameTxt, emailTxt, roleTxt, idTxt, deptTxt));

        view.findViewById(R.id.btn_download_history).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ResourceListActivity.class);
            intent.putExtra("list_type", "DOWNLOAD_HISTORY");
            intent.putExtra("title", "My Download History");
            startActivity(intent);
        });

        view.findViewById(R.id.btn_my_favorites).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ResourceListActivity.class);
            intent.putExtra("list_type", "FAVORITES");
            intent.putExtra("title", "My Favorite Resources");
            startActivity(intent);
        });

        view.findViewById(R.id.btn_my_uploads).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ManageUploadsActivity.class));
        });

        view.findViewById(R.id.logout_btn).setOnClickListener(v -> {
            sessionManager.logoutUser();
            Intent intent = new Intent(getActivity(), RegistrationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        View headerBg = view.findViewById(R.id.profile_header_bg);
        if (headerBg != null) {
            ThemeHelper.applyHeaderTheme(headerBg, sessionManager);
        }

        View btnDeleteAccount = view.findViewById(R.id.delete_account_btn);
        if (sessionManager.getEmail().equalsIgnoreCase("admin@library.duet.ac.bd") || sessionManager.getEmail().equalsIgnoreCase("admin@duet.ac.bd")) {
            if (btnDeleteAccount != null) btnDeleteAccount.setVisibility(View.GONE);
        } else {
            if (btnDeleteAccount != null) {
                btnDeleteAccount.setVisibility(View.VISIBLE);
                btnDeleteAccount.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Account")
                            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (dbHelper.deleteUser(sessionManager.getEmail())) {
                                    sessionManager.logoutUser();
                                    startActivity(new Intent(getActivity(), RegistrationActivity.class));
                                    if (getActivity() != null) getActivity().finish();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        }

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), REQUEST_PICK_PROFILE_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PROFILE_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadAndSaveProfileImage(imageUri);
        }
    }

    private File getLocalProfileCacheFile() {
        if (getContext() == null) return null;
        String fileName = "profile_" + sessionManager.getEmail().replace("@", "_").replace(".", "_") + ".jpg";
        return new File(requireContext().getFilesDir(), fileName);
    }

    private void uploadAndSaveProfileImage(Uri imageUri) {
        Toast.makeText(requireContext(), "Uploading Profile Picture to Firebase...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            File localCache = getLocalProfileCacheFile();
            byte[] imageBytes = null;
            android.graphics.Bitmap scaledBitmap = null;

            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        // Compress bitmap to avoid OOM
                        scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        imageBytes = baos.toByteArray();

                        if (localCache != null) {
                            try (FileOutputStream out = new FileOutputStream(localCache)) {
                                out.write(imageBytes);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading image bytes: " + e.getMessage());
            }

            final byte[] finalBytes = imageBytes;
            final android.graphics.Bitmap finalBitmap = scaledBitmap;
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (finalBitmap != null && imgAvatar != null) {
                        imgAvatar.clearColorFilter();
                        imgAvatar.setImageBitmap(finalBitmap);
                    }
                    
                    firebaseRepo.uploadProfileImage(imageUri, finalBytes, sessionManager.getEmail(), new FirebaseRepository.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            if (isAdded() && getContext() != null) {
                                dbHelper.updateUserProfileImage(sessionManager.getEmail(), downloadUrl);
                                sessionManager.setProfileImageUrl(downloadUrl);
                                Toast.makeText(requireContext(), "Profile picture saved on Firebase Cloud!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isAdded() && getContext() != null) {
                                Log.e(TAG, "Firebase upload failure: " + e.getMessage());
                                Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                });
            }
        }).start();
    }

    private void loadProfileImage() {
        File localCache = getLocalProfileCacheFile();
        boolean hasLocalCache = false;
        if (localCache != null && localCache.exists() && localCache.length() > 0) {
            Bitmap bitmap = BitmapFactory.decodeFile(localCache.getAbsolutePath());
            if (bitmap != null) {
                imgAvatar.clearColorFilter();
                imgAvatar.setImageBitmap(bitmap);
                hasLocalCache = true;
            }
        }

        final boolean cachedLocally = hasLocalCache;

        // Fetch latest profile picture URL from Firebase Firestore
        firebaseRepo.fetchUserFromFirebase(sessionManager.getEmail(), new FirebaseRepository.UserCallback() {
            @Override
            public void onSuccess(java.util.Map<String, Object> userData) {
                if (userData != null && userData.containsKey("profile_image_url")) {
                    String fbUrl = (String) userData.get("profile_image_url");
                    if (fbUrl != null && !fbUrl.isEmpty() && isAdded() && getContext() != null) {
                        sessionManager.setProfileImageUrl(fbUrl);
                        dbHelper.updateUserProfileImage(sessionManager.getEmail(), fbUrl);
                        fetchImageFromUrl(fbUrl);
                    } else if (!cachedLocally) {
                        showDefaultAvatar();
                    }
                } else if (!cachedLocally) {
                    showDefaultAvatar();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!cachedLocally) {
                    showDefaultAvatar();
                }
            }
        });
    }

    private void showDefaultAvatar() {
        if (imgAvatar == null) return;
        imgAvatar.setImageResource(R.drawable.ic_profile);
        imgAvatar.setColorFilter(Color.parseColor("#1A237E"));
    }

    private void fetchImageFromUrl(String imageUrl) {
        executor.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null) {
                        conn = (HttpURLConnection) new URL(newUrl).openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    }
                }

                try (InputStream in = conn.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    if (bitmap != null && getActivity() != null) {
                        File localCache = getLocalProfileCacheFile();
                        if (localCache != null) {
                            try (FileOutputStream out = new FileOutputStream(localCache)) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                            } catch (Exception ignored) {}
                        }

                        getActivity().runOnUiThread(() -> {
                            if (imgAvatar != null) {
                                imgAvatar.clearColorFilter();
                                imgAvatar.setImageBitmap(bitmap);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching profile image: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::showDefaultAvatar);
                }
            }
        });
    }

    private void updateActivityBadges(View view) {
        if (view == null || dbHelper == null || sessionManager == null) return;
        TextView txtDownloads = view.findViewById(R.id.txt_badge_downloads);
        TextView txtFavorites = view.findViewById(R.id.txt_badge_favorites);
        TextView txtUploads = view.findViewById(R.id.txt_badge_uploads);

        if (txtDownloads != null) {
            int dlCount = dbHelper.getUserDownloadCount(sessionManager.getEmail());
            txtDownloads.setText(String.valueOf(dlCount));
        }
        if (txtFavorites != null) {
            int favCount = dbHelper.getUserFavoriteCount(sessionManager.getEmail());
            txtFavorites.setText(String.valueOf(favCount));
        }
        if (txtUploads != null) {
            int ulCount = dbHelper.getUserUploadCount(sessionManager.getEmail());
            txtUploads.setText(String.valueOf(ulCount));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            updateActivityBadges(getView());
            loadProfileImage();
        }
    }

    private void updateProfileUI(TextView nameTxt, TextView emailTxt, TextView roleTxt, TextView idTxt, TextView deptTxt) {
        nameTxt.setText(sessionManager.getName());
        emailTxt.setText(sessionManager.getEmail());
        roleTxt.setText("Role: " + sessionManager.getRole());

        if (sessionManager.isStudent()) {
            idTxt.setText("Student ID: " + sessionManager.getStudentId());
            idTxt.setVisibility(View.VISIBLE);
        } else if (sessionManager.isTeacher()) {
            idTxt.setText("Designation: " + sessionManager.getStudentId());
            idTxt.setVisibility(View.VISIBLE);
        } else {
            idTxt.setText("Staff ID: " + sessionManager.getStudentId());
            idTxt.setVisibility(View.VISIBLE);
        }
        deptTxt.setText("Department: " + sessionManager.getDepartment());
    }

    private void showEditDialog(TextView nameTxt, TextView emailTxt, TextView roleTxt, TextView idTxt, TextView deptTxt) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText editName = dialogView.findViewById(R.id.edit_name);
        TextInputEditText editDept = dialogView.findViewById(R.id.edit_dept);

        editName.setText(sessionManager.getName());
        editDept.setText(sessionManager.getDepartment());

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText() != null ? editName.getText().toString().trim() : "";
                    String newDept = editDept.getText() != null ? editDept.getText().toString().trim() : "";

                    if (!newName.isEmpty()) {
                        boolean success = dbHelper.updateProfile(sessionManager.getEmail(), newName, sessionManager.getStudentId(), newDept);
                        if (success) {
                            sessionManager.updateSession(newName, newDept);
                            firebaseRepo.updateUserProfileInFirebase(sessionManager.getEmail(), newName, newDept, null);
                            updateProfileUI(nameTxt, emailTxt, roleTxt, idTxt, deptTxt);
                            Toast.makeText(requireContext(), "Profile Updated on App & Firebase", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
