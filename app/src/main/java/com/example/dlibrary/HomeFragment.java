package com.example.dlibrary;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private ListenerRegistration liveNoticeRegistration;

    private TextView welcomeMsg, subtitleMsg, txtDashboardTitle, txtPendingBadge;
    private TextView lblCard1, lblCard2, lblCard3, lblCard4, lblCard5, lblCard6, lblCard7, lblCard8;
    private ImageView imgCard1, imgCard2, imgCard3, imgCard4, imgCard5, imgCard6, imgCard7, imgCard8;
    private View card1, card2, card3, card4, card5, card6, card7, card8, cardGlobalSearch;
    private RecyclerView recyclerFeatured;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private void runOnBackground(Runnable runnable) {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor();
        }
        executor.execute(runnable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());
        dbHelper = new DatabaseHelper(requireContext());

        initViews(view);
        setupRoleDashboard();
        loadFeaturedBooks();
        setupRealtimeNoticeListener();

        return view;
    }

    private void initViews(View view) {
        welcomeMsg = view.findViewById(R.id.welcome_msg);
        subtitleMsg = view.findViewById(R.id.subtitle_msg);
        txtDashboardTitle = view.findViewById(R.id.txt_dashboard_title);
        txtPendingBadge = view.findViewById(R.id.txt_pending_badge);

        cardGlobalSearch = view.findViewById(R.id.card_global_search);
        card1 = view.findViewById(R.id.card_books);
        card2 = view.findViewById(R.id.card_lecture_sheets);
        card3 = view.findViewById(R.id.card_action3);
        card4 = view.findViewById(R.id.card_action4);
        card5 = view.findViewById(R.id.card_action5);
        card6 = view.findViewById(R.id.card_action6);
        card7 = view.findViewById(R.id.card_action7);
        card8 = view.findViewById(R.id.card_action8);

        View headerBg = view.findViewById(R.id.header_gradient_bg);
        if (headerBg != null) {
            ThemeHelper.applyHeaderTheme(headerBg, sessionManager);
        }

        lblCard1 = view.findViewById(R.id.lbl_card1);
        lblCard2 = view.findViewById(R.id.lbl_card2);
        lblCard3 = view.findViewById(R.id.lbl_card3);
        lblCard4 = view.findViewById(R.id.lbl_card4);
        lblCard5 = view.findViewById(R.id.lbl_card5);
        lblCard6 = view.findViewById(R.id.lbl_card6);
        lblCard7 = view.findViewById(R.id.lbl_card7);
        lblCard8 = view.findViewById(R.id.lbl_card8);

        imgCard1 = view.findViewById(R.id.img_card1);
        imgCard2 = view.findViewById(R.id.img_card2);
        imgCard3 = view.findViewById(R.id.img_card3);
        imgCard4 = view.findViewById(R.id.img_card4);
        imgCard5 = view.findViewById(R.id.img_card5);
        imgCard6 = view.findViewById(R.id.img_card6);
        imgCard7 = view.findViewById(R.id.img_card7);
        imgCard8 = view.findViewById(R.id.img_card8);

        recyclerFeatured = view.findViewById(R.id.recycler_featured_books);
        recyclerFeatured.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadHomeProfileAvatar(view);

        View btnNotification = view.findViewById(R.id.btn_home_notification);
        View badgeUnread = view.findViewById(R.id.badge_unread_notice);

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                if (badgeUnread != null) badgeUnread.setVisibility(View.GONE);
                startActivity(new Intent(getActivity(), NoticesActivity.class));
            });
        }

        view.findViewById(R.id.home_logout_btn).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                if (nav != null) nav.setSelectedItemId(R.id.navigation_profile);
            }
        });

        cardGlobalSearch.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                com.google.android.material.bottomnavigation.BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                if (nav != null) nav.setSelectedItemId(R.id.navigation_search);
            }
        });
    }

    private void setupRoleDashboard() {
        welcomeMsg.setText("Welcome, " + sessionManager.getName());
        subtitleMsg.setText(sessionManager.getRole() + " • " + sessionManager.getDepartment());

        if (sessionManager.isAdmin()) {
            txtDashboardTitle.setText("Librarian Dashboard");
            setupLibrarianDashboard();
        } else if (sessionManager.isTeacher()) {
            txtDashboardTitle.setText("Teacher Dashboard");
            setupTeacherDashboard();
        } else {
            txtDashboardTitle.setText("Student Dashboard");
            setupStudentDashboard();
        }
    }

    private void setupStudentDashboard() {
        lblCard1.setText("Digital Books");
        lblCard2.setText("Lecture Sheets");
        lblCard3.setText("Lab Manuals");
        lblCard4.setText("Past Questions");
        lblCard5.setText("Thesis Papers");
        lblCard6.setText("E-Newspapers");
        lblCard7.setText("PDF Reader");
        lblCard8.setText("Library ID");

        imgCard1.setImageResource(R.drawable.ic_book);
        imgCard2.setImageResource(R.drawable.ic_lecture);
        imgCard3.setImageResource(R.drawable.ic_lab);
        imgCard4.setImageResource(R.drawable.ic_history);
        imgCard5.setImageResource(R.drawable.ic_thesis);
        imgCard6.setImageResource(R.drawable.ic_notice);
        imgCard7.setImageResource(R.drawable.ic_book);
        imgCard8.setImageResource(R.drawable.ic_id_card);

        card1.setOnClickListener(v -> openResourceList("Book", "Digital Books"));
        card2.setOnClickListener(v -> openResourceList("Lecture Sheet", "Lecture Sheets"));
        card3.setOnClickListener(v -> openResourceList("Lab Manual", "Lab Manuals"));
        card4.setOnClickListener(v -> openResourceList("Previous Question", "Past Exam Questions"));
        card5.setOnClickListener(v -> startActivity(new Intent(getActivity(), ThesisListActivity.class)));
        card6.setOnClickListener(v -> startActivity(new Intent(getActivity(), NewspaperActivity.class)));
        card7.setOnClickListener(v -> startActivity(new Intent(getActivity(), PdfViewerActivity.class)));
        card8.setOnClickListener(v -> startActivity(new Intent(getActivity(), UniversityIdActivity.class)));
    }

    private void setupTeacherDashboard() {
        lblCard1.setText("Upload Lecture");
        lblCard2.setText("Upload Lab Manual");
        lblCard3.setText("Search Books");
        lblCard4.setText("Manage Uploads");
        lblCard5.setText("Past Questions");
        lblCard6.setText("E-Newspapers");
        lblCard7.setText("PDF Reader");
        lblCard8.setText("Library ID");

        imgCard1.setImageResource(R.drawable.ic_lecture);
        imgCard2.setImageResource(R.drawable.ic_lab);
        imgCard3.setImageResource(R.drawable.ic_book);
        imgCard4.setImageResource(R.drawable.ic_profile);
        imgCard5.setImageResource(R.drawable.ic_history);
        imgCard6.setImageResource(R.drawable.ic_notice);
        imgCard7.setImageResource(R.drawable.ic_book);
        imgCard8.setImageResource(R.drawable.ic_id_card);

        card1.setOnClickListener(v -> openUploadScreen("Lecture Sheet"));
        card2.setOnClickListener(v -> openUploadScreen("Lab Manual"));
        card3.setOnClickListener(v -> openResourceList("Book", "Books Library"));
        card4.setOnClickListener(v -> startActivity(new Intent(getActivity(), ManageUploadsActivity.class)));
        card5.setOnClickListener(v -> openResourceList("Previous Question", "Past Exam Questions"));
        card6.setOnClickListener(v -> startActivity(new Intent(getActivity(), NewspaperActivity.class)));
        card7.setOnClickListener(v -> startActivity(new Intent(getActivity(), PdfViewerActivity.class)));
        card8.setOnClickListener(v -> startActivity(new Intent(getActivity(), UniversityIdActivity.class)));
    }

    private void setupLibrarianDashboard() {
        lblCard1.setText("Approval Queue");
        lblCard2.setText("User Management");
        lblCard3.setText("Upload Resource");
        lblCard4.setText("Categories");
        lblCard5.setText("Download Logs");
        lblCard6.setText("Notices & Content");
        lblCard7.setText("E-Newspapers");
        lblCard8.setText("PDF Reader");

        imgCard1.setImageResource(R.drawable.ic_approval);
        imgCard2.setImageResource(R.drawable.ic_users);
        imgCard3.setImageResource(R.drawable.ic_book);
        imgCard4.setImageResource(R.drawable.ic_category);
        imgCard5.setImageResource(R.drawable.ic_history);
        imgCard6.setImageResource(R.drawable.ic_notice);
        imgCard7.setImageResource(R.drawable.ic_notice);
        imgCard8.setImageResource(R.drawable.ic_book);

        runOnBackground(() -> {
            Cursor pendingCursor = dbHelper.getPendingResources();
            int pendingCount = pendingCursor != null ? pendingCursor.getCount() : 0;
            if (pendingCursor != null) pendingCursor.close();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && txtPendingBadge != null) {
                        if (pendingCount > 0) {
                            txtPendingBadge.setText(pendingCount + " Pending");
                            txtPendingBadge.setVisibility(View.VISIBLE);
                        } else {
                            txtPendingBadge.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        card1.setOnClickListener(v -> startActivity(new Intent(getActivity(), ResourceApprovalActivity.class)));
        card2.setOnClickListener(v -> startActivity(new Intent(getActivity(), AdminUserManagementActivity.class)));
        card3.setOnClickListener(v -> openUploadScreen("Book"));
        card4.setOnClickListener(v -> startActivity(new Intent(getActivity(), CategoryManagementActivity.class)));
        card5.setOnClickListener(v -> startActivity(new Intent(getActivity(), DownloadLogsActivity.class)));
        card6.setOnClickListener(v -> startActivity(new Intent(getActivity(), NoticesActivity.class)));
        card7.setOnClickListener(v -> startActivity(new Intent(getActivity(), NewspaperActivity.class)));
        card8.setOnClickListener(v -> startActivity(new Intent(getActivity(), PdfViewerActivity.class)));
    }

    private void setupRealtimeNoticeListener() {
        liveNoticeRegistration = FirebaseRepository.getInstance().listenForLiveNotices((title, content, pdfUrl, postedBy, date) -> {
            if (isAdded() && getContext() != null && title != null && !title.isEmpty()) {
                if (getView() != null) {
                    View badgeUnread = getView().findViewById(R.id.badge_unread_notice);
                    if (badgeUnread != null) {
                        badgeUnread.setVisibility(View.VISIBLE);
                    }
                }
                NotificationHelper.showNoticeNotification(requireContext(), title, content);
            }
        });
    }

    private void openUploadScreen(String defaultType) {
        Intent intent = new Intent(getActivity(), UploadResourceActivity.class);
        intent.putExtra("default_type", defaultType);
        startActivity(intent);
    }

    private void openResourceList(String type, String title) {
        Intent intent = new Intent(getActivity(), ResourceListActivity.class);
        intent.putExtra("list_type", "TYPE_FILTER");
        intent.putExtra("filter_value", type);
        intent.putExtra("title", title);
        startActivity(intent);
    }

    private void loadFeaturedBooks() {
        if (getContext() == null || dbHelper == null) return;
        runOnBackground(() -> {
            List<Resource> books = new ArrayList<>();
            Cursor cursor = dbHelper.searchResources("Book", "All", "All", "", "", "Approved");
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
                    String status = statusIdx != -1 ? cursor.getString(statusIdx) : "Approved";
                    String date = dateIdx != -1 ? cursor.getString(dateIdx) : "";
                    int downloads = dlIdx != -1 ? cursor.getInt(dlIdx) : 0;
                    int favorites = favIdx != -1 ? cursor.getInt(favIdx) : 0;

                    books.add(new Resource(id, title, author, cat, type, file, code, dept, uploader, status, date, downloads, favorites));
                } while (cursor.moveToNext());
                cursor.close();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null && recyclerFeatured != null) {
                        ResourceAdapter adapter = new ResourceAdapter(requireContext(), books, null);
                        recyclerFeatured.setAdapter(adapter);
                    }
                });
            }
        });
    }

    private void loadHomeProfileAvatar(View view) {
        if (view == null || sessionManager == null) return;
        ImageView imgAvatar = view.findViewById(R.id.img_home_avatar);
        if (imgAvatar == null) return;

        String email = sessionManager.getEmail();
        if (email.isEmpty()) return;

        java.io.File localCache = new java.io.File(requireContext().getFilesDir(), "profile_" + email.replace("@", "_").replace(".", "_") + ".jpg");
        boolean hasLocalCache = false;
        if (localCache.exists() && localCache.length() > 0) {
            hasLocalCache = true;
            runOnBackground(() -> {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(localCache.getAbsolutePath());
                if (bitmap != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (imgAvatar != null) {
                            imgAvatar.clearColorFilter();
                            imgAvatar.setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }

        final boolean cachedLocally = hasLocalCache;

        // Fetch latest profile picture URL from Firebase Firestore
        FirebaseRepository.getInstance().fetchUserFromFirebase(email, new FirebaseRepository.UserCallback() {
            @Override
            public void onSuccess(java.util.Map<String, Object> userData) {
                if (userData != null && userData.containsKey("profile_image_url")) {
                    String fbUrl = (String) userData.get("profile_image_url");
                    if (fbUrl != null && !fbUrl.isEmpty() && isAdded() && getContext() != null) {
                        sessionManager.setProfileImageUrl(fbUrl);
                        if (dbHelper != null) dbHelper.updateUserProfileImage(email, fbUrl);
                        fetchHomeAvatarFromUrl(fbUrl, imgAvatar, localCache);
                    } else if (!cachedLocally) {
                        showHomeDefaultAvatar(imgAvatar);
                    }
                } else if (!cachedLocally) {
                    showHomeDefaultAvatar(imgAvatar);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!cachedLocally) {
                    showHomeDefaultAvatar(imgAvatar);
                }
            }
        });
    }

    private void showHomeDefaultAvatar(ImageView imgAvatar) {
        if (imgAvatar == null) return;
        imgAvatar.setImageResource(R.drawable.ic_profile);
        imgAvatar.setColorFilter(android.graphics.Color.WHITE);
    }

    private void fetchHomeAvatarFromUrl(String imageUrl, ImageView imgAvatar, java.io.File localCache) {
        runOnBackground(() -> {
            try {
                java.net.URL u = new java.net.URL(imageUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP || responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null) {
                        conn = (java.net.HttpURLConnection) new java.net.URL(newUrl).openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    }
                }

                try (java.io.InputStream in = conn.getInputStream()) {
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
                    if (bitmap != null && getActivity() != null) {
                        if (localCache != null) {
                            try (java.io.FileOutputStream out = new java.io.FileOutputStream(localCache)) {
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showHomeDefaultAvatar(imgAvatar));
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isAdmin()) {
            setupLibrarianDashboard();
        }
        if (getView() != null) {
            loadHomeProfileAvatar(getView());
        }
        loadFeaturedBooks();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (liveNoticeRegistration != null) {
            liveNoticeRegistration.remove();
        }
        executor.shutdown();
    }
}
