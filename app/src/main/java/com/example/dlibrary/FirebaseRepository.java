package com.example.dlibrary;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";
    private static FirebaseRepository instance;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    public interface NoticeListener {
        void onNoticeReceived(String title, String content, String pdfUrl, String postedBy, String date);
    }

    public interface UploadCallback {
        void onSuccess(String result);
        void onFailure(Exception e);
    }

    public interface UserCallback {
        void onSuccess(Map<String, Object> userData);
        void onFailure(Exception e);
    }

    public interface UserListCallback {
        void onSuccess(List<Map<String, Object>> userList);
        void onFailure(Exception e);
    }

    private FirebaseRepository() {
        try {
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance("gs://d-library-bf709.firebasestorage.app");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage());
        }
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

    // --- Firebase User Management ---

    public void saveUserToFirebase(String name, String studentId, String department, String email, String password, String role, String status, UploadCallback callback) {
        if (db == null) {
            if (callback != null) callback.onFailure(new Exception("Firebase not initialized"));
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("student_id", studentId);
        userMap.put("department", department);
        userMap.put("email", email);
        userMap.put("password", password);
        userMap.put("role", role);
        userMap.put("status", status != null ? status : "Active");
        userMap.put("created_at", System.currentTimeMillis());

        db.collection("users")
                .document(email)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(email);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void fetchUserFromFirebase(String email, UserCallback callback) {
        if (db == null) {
            if (callback != null) callback.onFailure(new Exception("Firebase not initialized"));
            return;
        }

        db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        if (callback != null) callback.onSuccess(documentSnapshot.getData());
                    } else {
                        if (callback != null) callback.onFailure(new Exception("User not found in Firebase"));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void updateUserStatusInFirebase(String email, String status, UploadCallback callback) {
        if (db == null) return;
        db.collection("users").document(email)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess("Updated");
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void updateUserProfileInFirebase(String email, String name, String department, UploadCallback callback) {
        if (db == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("department", department);

        db.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess("Updated");
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void fetchAllUsersFromFirebase(UserListCallback callback) {
        if (db == null) {
            if (callback != null) callback.onFailure(new Exception("Firebase not initialized"));
            return;
        }

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (doc.getData() != null) {
                            list.add(doc.getData());
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // --- Realtime Notice Sync & Posting ---

    public void postNotice(String title, String content, String pdfUrl, String postedBy, String date, UploadCallback callback) {
        if (db == null) {
            if (callback != null) callback.onFailure(new Exception("Firebase Firestore not initialized"));
            return;
        }

        Map<String, Object> noticeMap = new HashMap<>();
        noticeMap.put("title", title);
        noticeMap.put("content", content);
        noticeMap.put("pdfUrl", pdfUrl != null ? pdfUrl : "");
        noticeMap.put("postedBy", postedBy);
        noticeMap.put("date", date);
        noticeMap.put("timestamp", System.currentTimeMillis());

        db.collection("notices")
                .add(noticeMap)
                .addOnSuccessListener(documentReference -> {
                    if (callback != null) callback.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public ListenerRegistration listenForLiveNotices(NoticeListener listener) {
        if (db == null) return null;

        return db.collection("notices")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Notice listen error", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        String title = doc.getString("title");
                        String content = doc.getString("content");
                        String pdfUrl = doc.getString("pdfUrl");
                        String postedBy = doc.getString("postedBy");
                        String date = doc.getString("date");

                        if (listener != null) {
                            listener.onNoticeReceived(title, content, pdfUrl, postedBy, date);
                        }
                    }
                });
    }

    public interface ResourceListCallback {
        void onSuccess(List<Map<String, Object>> resources);
        void onFailure(Exception e);
    }

    public void publishResourceToFirebase(String title, String author, String category, String type, String filePath, String courseCode, String dept, String uploadedBy, String status, UploadCallback callback) {
        if (db == null) return;

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("title", title);
        resMap.put("author", author);
        resMap.put("category", category);
        resMap.put("type", type);
        resMap.put("file_path", filePath != null ? filePath : "");
        resMap.put("course_code", courseCode != null ? courseCode : "");
        resMap.put("res_department", dept != null ? dept : "");
        resMap.put("uploaded_by", uploadedBy != null ? uploadedBy : "");
        resMap.put("approval_status", status != null ? status : "Pending");
        resMap.put("upload_date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        resMap.put("timestamp", System.currentTimeMillis());

        String safeUser = (uploadedBy != null ? uploadedBy : "user").replace("@", "_").replace(".", "_");
        String docId = safeUser + "_" + System.currentTimeMillis();
        db.collection("resources").document(docId)
                .set(resMap)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void fetchPendingResourcesFromFirebase(ResourceListCallback callback) {
        if (db == null) {
            if (callback != null) callback.onFailure(new Exception("Firestore not initialized"));
            return;
        }

        db.collection("resources")
                .whereEqualTo("approval_status", "Pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc.getData() != null) {
                                Map<String, Object> data = new HashMap<>(doc.getData());
                                data.put("doc_id", doc.getId());
                                list.add(data);
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void fetchApprovedResourcesFromFirebase(ResourceListCallback callback) {
        if (db == null) {
            if (callback != null) callback.onFailure(new Exception("Firestore not initialized"));
            return;
        }

        db.collection("resources")
                .whereEqualTo("approval_status", "Approved")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc.getData() != null) {
                                Map<String, Object> data = new HashMap<>(doc.getData());
                                data.put("doc_id", doc.getId());
                                list.add(data);
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public ListenerRegistration listenForLiveResources(ResourceListCallback callback) {
        if (db == null) return null;

        return db.collection("resources")
                .whereEqualTo("approval_status", "Approved")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        if (callback != null) callback.onFailure(e);
                        return;
                    }

                    List<Map<String, Object>> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc.getData() != null) {
                                Map<String, Object> data = new HashMap<>(doc.getData());
                                data.put("doc_id", doc.getId());
                                list.add(data);
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                });
    }

    public void updateResourceApprovalInFirebase(String title, String uploadedBy, String status) {
        if (db == null) return;
        db.collection("resources")
                .whereEqualTo("title", title)
                .whereEqualTo("uploaded_by", uploadedBy)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            doc.getReference().update("approval_status", status);
                        }
                    }
                });
    }

    public void addCategoryToFirebase(String categoryName, UploadCallback callback) {
        if (db == null) return;
        Map<String, Object> catMap = new HashMap<>();
        catMap.put("name", categoryName);
        catMap.put("timestamp", System.currentTimeMillis());

        db.collection("categories").document(categoryName.replace("/", "_"))
                .set(catMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(categoryName);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void fetchCategoriesFromFirebase(UserListCallback callback) {
        if (db == null) return;
        db.collection("categories")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Map<String, Object>> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc.getData() != null) {
                                list.add(doc.getData());
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // --- Firebase Storage PDF Upload & Download ---

    public void uploadPdfFile(Uri fileUri, String folderName, UploadCallback callback) {
        if (storage == null) {
            if (callback != null) callback.onFailure(new Exception("Firebase Storage not initialized"));
            return;
        }

        if (fileUri == null) {
            if (callback != null) callback.onFailure(new Exception("File URI cannot be null"));
            return;
        }

        String extension = ".pdf";
        String str = fileUri.toString().toLowerCase();
        if (str.contains(".pptx")) {
            extension = ".pptx";
        } else if (str.contains(".ppt")) {
            extension = ".ppt";
        }

        String fileName = folderName + "/" + System.currentTimeMillis() + extension;
        StorageReference ref = storage.getReference().child(fileName);

        // Standard putFile from the provided URI
        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (callback != null) callback.onSuccess(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void uploadProfileImage(Uri fileUri, byte[] imageBytes, String userEmail, UploadCallback callback) {
        if (storage == null) {
            try {
                storage = FirebaseStorage.getInstance();
            } catch (Exception e) {
                if (callback != null) callback.onFailure(new Exception("Firebase Storage not initialized"));
                return;
            }
        }

        String fileName = "profile_images/" + userEmail.replace("@", "_").replace(".", "_") + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        com.google.firebase.storage.UploadTask uploadTask;
        if (imageBytes != null && imageBytes.length > 0) {
            uploadTask = ref.putBytes(imageBytes);
        } else if (fileUri != null) {
            uploadTask = ref.putFile(fileUri);
        } else {
            if (callback != null) callback.onFailure(new Exception("Invalid image data"));
            return;
        }

        uploadTask.addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            String downloadUrl = uri.toString();
            if (db != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("profile_image_url", downloadUrl);
                db.collection("users").document(userEmail)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge());
            }
            if (callback != null) callback.onSuccess(downloadUrl);
        })).addOnFailureListener(e -> {
            if (callback != null) callback.onFailure(e);
        });
    }

    public void uploadProfileImage(Uri fileUri, String userEmail, UploadCallback callback) {
        uploadProfileImage(fileUri, null, userEmail, callback);
    }

    public void downloadFileFromStorage(String pdfUrl, File destinationFile, UploadCallback callback) {
        if (storage == null) {
            if (callback != null) callback.onFailure(new Exception("Firebase Storage not initialized"));
            return;
        }

        try {
            StorageReference ref = storage.getReferenceFromUrl(pdfUrl);
            ref.getFile(destinationFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        if (callback != null) callback.onSuccess(destinationFile.getAbsolutePath());
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onFailure(e);
                    });
        } catch (Exception e) {
            if (callback != null) callback.onFailure(e);
        }
    }
}
