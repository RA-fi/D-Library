package com.example.dlibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "DLibrary.db";
    private static final int DATABASE_VERSION = 6;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_RESOURCES = "resources";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_DOWNLOADS = "downloads";
    public static final String TABLE_FAVORITES = "favorites";
    public static final String TABLE_NOTICES = "notices";

    // Common Column
    public static final String COLUMN_ID = "id";

    // Users Columns
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_STUDENT_ID = "student_id"; // For student: Student ID; For teacher: Designation; For librarian: Staff ID
    public static final String COLUMN_DEPARTMENT = "department";
    public static final String COLUMN_EMAIL = "email"; // Primary unique credential for everyone
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_ROLE = "role"; // "Student", "Teacher", "Librarian"
    public static final String COLUMN_STATUS = "status"; // "Active", "Blocked"
    public static final String COLUMN_PROFILE_IMAGE_URL = "profile_image_url";

    // Resources / Books Columns
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_TYPE = "type"; // "Book", "Lecture Sheet", "Lab Manual", "Previous Question", "Thesis", "Notice"
    public static final String COLUMN_FILE_PATH = "file_path"; // pdf_files / pptx_files
    public static final String COLUMN_COVER_IMAGE = "cover_image"; // Cover thumbnail or URI
    public static final String COLUMN_COURSE_CODE = "course_code"; // Optional
    public static final String COLUMN_RES_DEPARTMENT = "res_department";
    public static final String COLUMN_UPLOADED_BY = "uploaded_by";
    public static final String COLUMN_APPROVAL_STATUS = "approval_status"; // "Approved", "Pending", "Rejected"
    public static final String COLUMN_UPLOAD_DATE = "upload_date";
    public static final String COLUMN_DOWNLOAD_COUNT = "download_count";
    public static final String COLUMN_FAVORITE_COUNT = "favorite_count";

    // Categories Columns
    public static final String COLUMN_CATEGORY_NAME = "category_name";

    // Downloads Columns
    public static final String COLUMN_USER_EMAIL = "user_email";
    public static final String COLUMN_RESOURCE_ID = "resource_id";
    public static final String COLUMN_DOWNLOAD_DATE = "download_date";

    // Favorites Columns
    // COLUMN_USER_EMAIL, COLUMN_RESOURCE_ID

    // Notices Columns
    public static final String COLUMN_NOTICE_TITLE = "notice_title";
    public static final String COLUMN_NOTICE_CONTENT = "notice_content";
    public static final String COLUMN_NOTICE_POSTED_BY = "posted_by";
    public static final String COLUMN_NOTICE_DATE = "notice_date";

    // SQL Statements
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_STUDENT_ID + " TEXT, " +
                    COLUMN_DEPARTMENT + " TEXT, " +
                    COLUMN_EMAIL + " TEXT UNIQUE, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_ROLE + " TEXT, " +
                    COLUMN_STATUS + " TEXT DEFAULT 'Active', " +
                    COLUMN_PROFILE_IMAGE_URL + " TEXT" +
                    ")";

    private static final String CREATE_TABLE_RESOURCES =
            "CREATE TABLE " + TABLE_RESOURCES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_AUTHOR + " TEXT, " +
                    COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_FILE_PATH + " TEXT, " +
                    COLUMN_COVER_IMAGE + " TEXT, " +
                    COLUMN_COURSE_CODE + " TEXT, " +
                    COLUMN_RES_DEPARTMENT + " TEXT, " +
                    COLUMN_UPLOADED_BY + " TEXT, " +
                    COLUMN_APPROVAL_STATUS + " TEXT DEFAULT 'Approved', " +
                    COLUMN_UPLOAD_DATE + " TEXT, " +
                    COLUMN_DOWNLOAD_COUNT + " INTEGER DEFAULT 0, " +
                    COLUMN_FAVORITE_COUNT + " INTEGER DEFAULT 0" +
                    ")";

    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CATEGORY_NAME + " TEXT UNIQUE" +
                    ")";

    private static final String CREATE_TABLE_DOWNLOADS =
            "CREATE TABLE " + TABLE_DOWNLOADS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_EMAIL + " TEXT, " +
                    COLUMN_RESOURCE_ID + " INTEGER, " +
                    COLUMN_DOWNLOAD_DATE + " TEXT" +
                    ")";

    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_EMAIL + " TEXT, " +
                    COLUMN_RESOURCE_ID + " INTEGER, " +
                    "UNIQUE(" + COLUMN_USER_EMAIL + ", " + COLUMN_RESOURCE_ID + ")" +
                    ")";

    private static final String CREATE_TABLE_NOTICES =
            "CREATE TABLE " + TABLE_NOTICES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NOTICE_TITLE + " TEXT, " +
                    COLUMN_NOTICE_CONTENT + " TEXT, " +
                    COLUMN_NOTICE_POSTED_BY + " TEXT, " +
                    COLUMN_NOTICE_DATE + " TEXT" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_RESOURCES);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_DOWNLOADS);
        db.execSQL(CREATE_TABLE_FAVORITES);
        db.execSQL(CREATE_TABLE_NOTICES);

        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESOURCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTICES);
        onCreate(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        // Official DUET Super Admin Account
        insertUserRaw(db, "DUET Chief Librarian", "Chief Librarian", "Library", "admin@library.duet.ac.bd", "qwer1234@", "Admin", "Active");
        insertUserRaw(db, "System Librarian", "LIB-001", "Library", "admin@duet.ac.bd", "admin123", "Librarian", "Active");

        // Official DUET Academic Departments & Categories (duet.ac.bd)
        insertCategoryRaw(db, "Computer Science & Engineering (CSE)");
        insertCategoryRaw(db, "Electrical & Electronic Engineering (EEE)");
        insertCategoryRaw(db, "Mechanical Engineering (ME)");
        insertCategoryRaw(db, "Civil Engineering (CE)");
        insertCategoryRaw(db, "Industrial & Production Engineering (IPE)");
        insertCategoryRaw(db, "Textile Engineering (TE)");
        insertCategoryRaw(db, "Architecture (Arch)");
        insertCategoryRaw(db, "Chemical & Food Process Engineering (CFPE)");
        insertCategoryRaw(db, "Materials & Metallurgical Engineering (MME)");
        insertCategoryRaw(db, "Mathematics (Math)");
        insertCategoryRaw(db, "Physics (Phy)");
        insertCategoryRaw(db, "Chemistry (Chem)");
        insertCategoryRaw(db, "Humanities & Social Sciences (Hum)");
    }

    private void insertUserRaw(SQLiteDatabase db, String name, String studentId, String department, String email, String password, String role, String status) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_STUDENT_ID, studentId);
        values.put(COLUMN_DEPARTMENT, department);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_ROLE, role);
        values.put(COLUMN_STATUS, status);
        db.insert(TABLE_USERS, null, values);
    }

    private void insertCategoryRaw(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, name);
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // --- User Operations ---

    public boolean registerUser(String name, String studentIdOrDesignation, String department, String email, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_STUDENT_ID, studentIdOrDesignation);
        values.put(COLUMN_DEPARTMENT, department);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_ROLE, role);
        values.put(COLUMN_STATUS, "Active");

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public Cursor checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        return db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);
    }

    public Cursor getUser(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null, COLUMN_EMAIL + " = ?", new String[]{email}, null, null, null);
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null, null, null, null, null, COLUMN_ROLE + " ASC, " + COLUMN_NAME + " ASC");
    }

    public boolean updateUserProfileImage(String email, String imageUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_IMAGE_URL, imageUrl);
        int rows = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }

    public String getUserProfileImage(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_PROFILE_IMAGE_URL}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        String url = "";
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(COLUMN_PROFILE_IMAGE_URL);
            if (idx != -1) url = cursor.getString(idx);
            cursor.close();
        }
        return url != null ? url : "";
    }

    public void ensureSuperAdmin() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME, "DUET Chief Librarian");
            values.put(COLUMN_STUDENT_ID, "Chief Librarian");
            values.put(COLUMN_DEPARTMENT, "Library");
            values.put(COLUMN_EMAIL, "admin@library.duet.ac.bd");
            values.put(COLUMN_PASSWORD, "qwer1234@");
            values.put(COLUMN_ROLE, "Admin");
            values.put(COLUMN_STATUS, "Active");

            int updated = db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{"admin@library.duet.ac.bd"});
            if (updated == 0) {
                db.insert(TABLE_USERS, null, values);
            }
        } catch (Exception ignored) {}
    }

    public boolean updateUserStatus(String email, String status) {
        if (email != null && (email.equalsIgnoreCase("admin@library.duet.ac.bd") || email.equalsIgnoreCase("admin@duet.ac.bd"))) {
            return false; // Super Admin cannot be blocked
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status);
        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{email});
        return result > 0;
    }

    public boolean updateProfile(String email, String name, String studentIdOrDesignation, String department) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_STUDENT_ID, studentIdOrDesignation);
        values.put(COLUMN_DEPARTMENT, department);

        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{email});
        return result > 0;
    }

    public boolean deleteUser(String email) {
        if (email != null && (email.equalsIgnoreCase("admin@library.duet.ac.bd") || email.equalsIgnoreCase("admin@duet.ac.bd"))) {
            return false; // Super Admin cannot be deleted
        }
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_USERS, COLUMN_EMAIL + " = ?", new String[]{email});
        return result > 0;
    }

    // --- Resource / Book Operations ---

    public boolean addResource(String title, String author, String category, String type, String filePath, String coverImage, String courseCode, String dept, String uploadedBy, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_AUTHOR, author);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_FILE_PATH, filePath);
        values.put(COLUMN_COVER_IMAGE, coverImage);
        values.put(COLUMN_COURSE_CODE, courseCode);
        values.put(COLUMN_RES_DEPARTMENT, dept);
        values.put(COLUMN_UPLOADED_BY, uploadedBy);
        values.put(COLUMN_APPROVAL_STATUS, status);
        values.put(COLUMN_UPLOAD_DATE, getCurrentDate());
        values.put(COLUMN_DOWNLOAD_COUNT, 0);
        values.put(COLUMN_FAVORITE_COUNT, 0);

        long result = db.insert(TABLE_RESOURCES, null, values);
        return result != -1;
    }

    public Cursor getResourceById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_RESOURCES, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
    }

    public Cursor searchResources(String typeFilter, String categoryFilter, String deptFilter, String query, String courseCodeQuery, String statusFilter) {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgsList = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isEmpty()) {
            selection.append(COLUMN_APPROVAL_STATUS).append(" = ?");
            selectionArgsList.add(statusFilter);
        }

        if (typeFilter != null && !typeFilter.equals("All") && !typeFilter.isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_TYPE).append(" = ?");
            selectionArgsList.add(typeFilter);
        }

        if (categoryFilter != null && !categoryFilter.equals("All") && !categoryFilter.isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_CATEGORY).append(" = ?");
            selectionArgsList.add(categoryFilter);
        }

        if (deptFilter != null && !deptFilter.equals("All") && !deptFilter.isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_RES_DEPARTMENT).append(" = ?");
            selectionArgsList.add(deptFilter);
        }

        if (courseCodeQuery != null && !courseCodeQuery.trim().isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_COURSE_CODE).append(" LIKE ?");
            selectionArgsList.add("%" + courseCodeQuery.trim() + "%");
        }

        if (query != null && !query.trim().isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append("(").append(COLUMN_TITLE).append(" LIKE ? OR ")
                     .append(COLUMN_AUTHOR).append(" LIKE ? OR ")
                     .append(COLUMN_COURSE_CODE).append(" LIKE ?)");
            String param = "%" + query.trim() + "%";
            selectionArgsList.add(param);
            selectionArgsList.add(param);
            selectionArgsList.add(param);
        }

        String[] args = selectionArgsList.toArray(new String[0]);
        return db.query(TABLE_RESOURCES, null, selection.length() > 0 ? selection.toString() : null, args, null, null, COLUMN_ID + " DESC");
    }

    public Cursor getResourcesByUploader(String uploadedBy) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_RESOURCES, null, COLUMN_UPLOADED_BY + " = ?", new String[]{uploadedBy}, null, null, COLUMN_ID + " DESC");
    }

    public int getUserUploadCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_RESOURCES, new String[]{COLUMN_ID}, COLUMN_UPLOADED_BY + " = ?", new String[]{userEmail}, null, null, null);
        int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) cursor.close();
        return count;
    }

    public int getUserDownloadCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DOWNLOADS, new String[]{COLUMN_ID}, COLUMN_USER_EMAIL + " = ?", new String[]{userEmail}, null, null, null);
        int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) cursor.close();
        return count;
    }

    public int getUserFavoriteCount(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, new String[]{COLUMN_ID}, COLUMN_USER_EMAIL + " = ?", new String[]{userEmail}, null, null, null);
        int count = cursor != null ? cursor.getCount() : 0;
        if (cursor != null) cursor.close();
        return count;
    }

    public Cursor getPendingResources() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_RESOURCES, null, COLUMN_APPROVAL_STATUS + " = ?", new String[]{"Pending"}, null, null, COLUMN_ID + " DESC");
    }

    public boolean updateResourceApproval(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_APPROVAL_STATUS, status);
        int result = db.update(TABLE_RESOURCES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    public boolean deleteResource(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_RESOURCES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // --- Downloads, History & Audit Operations ---

    public void incrementDownloadCount(int resourceId, String userEmail) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_RESOURCES + " SET " + COLUMN_DOWNLOAD_COUNT + " = " + COLUMN_DOWNLOAD_COUNT + " + 1 WHERE " + COLUMN_ID + " = " + resourceId);

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, userEmail);
        values.put(COLUMN_RESOURCE_ID, resourceId);
        values.put(COLUMN_DOWNLOAD_DATE, getCurrentDate());
        db.insert(TABLE_DOWNLOADS, null, values);
    }

    public Cursor getUserDownloadHistory(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT r.*, d." + COLUMN_DOWNLOAD_DATE + " AS download_date_record FROM " + TABLE_RESOURCES + " r " +
                "INNER JOIN " + TABLE_DOWNLOADS + " d ON r." + COLUMN_ID + " = d." + COLUMN_RESOURCE_ID + " " +
                "WHERE d." + COLUMN_USER_EMAIL + " = ? ORDER BY d." + COLUMN_ID + " DESC";
        return db.rawQuery(query, new String[]{userEmail});
    }

    // Librarian Audit View: See who downloaded which books/resources
    public Cursor getAllDownloadLogs() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT d." + COLUMN_ID + " AS download_id, " +
                "u." + COLUMN_NAME + " AS user_name, " +
                "d." + COLUMN_USER_EMAIL + " AS user_email, " +
                "u." + COLUMN_ROLE + " AS user_role, " +
                "r." + COLUMN_TITLE + " AS book_title, " +
                "r." + COLUMN_TYPE + " AS book_type, " +
                "d." + COLUMN_DOWNLOAD_DATE + " AS download_date " +
                "FROM " + TABLE_DOWNLOADS + " d " +
                "LEFT JOIN " + TABLE_USERS + " u ON d." + COLUMN_USER_EMAIL + " = u." + COLUMN_EMAIL + " " +
                "LEFT JOIN " + TABLE_RESOURCES + " r ON d." + COLUMN_RESOURCE_ID + " = r." + COLUMN_ID + " " +
                "ORDER BY d." + COLUMN_ID + " DESC";
        return db.rawQuery(query, null);
    }

    // --- Favorites ---

    public boolean toggleFavorite(String userEmail, int resourceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (isFavorite(userEmail, resourceId)) {
            db.delete(TABLE_FAVORITES, COLUMN_USER_EMAIL + " = ? AND " + COLUMN_RESOURCE_ID + " = ?", new String[]{userEmail, String.valueOf(resourceId)});
            db.execSQL("UPDATE " + TABLE_RESOURCES + " SET " + COLUMN_FAVORITE_COUNT + " = MAX(0, " + COLUMN_FAVORITE_COUNT + " - 1) WHERE " + COLUMN_ID + " = " + resourceId);
            return false;
        } else {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_EMAIL, userEmail);
            values.put(COLUMN_RESOURCE_ID, resourceId);
            db.insert(TABLE_FAVORITES, null, values);
            db.execSQL("UPDATE " + TABLE_RESOURCES + " SET " + COLUMN_FAVORITE_COUNT + " = " + COLUMN_FAVORITE_COUNT + " + 1 WHERE " + COLUMN_ID + " = " + resourceId);
            return true;
        }
    }

    public boolean isFavorite(String userEmail, int resourceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, null, COLUMN_USER_EMAIL + " = ? AND " + COLUMN_RESOURCE_ID + " = ?", new String[]{userEmail, String.valueOf(resourceId)}, null, null, null);
        boolean isFav = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return isFav;
    }

    public Cursor getUserFavorites(String userEmail) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT r.* FROM " + TABLE_RESOURCES + " r " +
                "INNER JOIN " + TABLE_FAVORITES + " f ON r." + COLUMN_ID + " = f." + COLUMN_RESOURCE_ID + " " +
                "WHERE f." + COLUMN_USER_EMAIL + " = ? ORDER BY f." + COLUMN_ID + " DESC";
        return db.rawQuery(query, new String[]{userEmail});
    }

    // --- Categories & Notices ---

    public boolean addCategory(String categoryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, categoryName);
        long result = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return result != -1;
    }

    public List<String> getAllCategories() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, COLUMN_CATEGORY_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(COLUMN_CATEGORY_NAME);
            do {
                if (nameIndex != -1) list.add(cursor.getString(nameIndex));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean addNotice(String title, String content, String postedBy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTICE_TITLE, title);
        values.put(COLUMN_NOTICE_CONTENT, content);
        values.put(COLUMN_NOTICE_POSTED_BY, postedBy);
        values.put(COLUMN_NOTICE_DATE, getCurrentDate());
        long result = db.insert(TABLE_NOTICES, null, values);
        return result != -1;
    }

    public Cursor getAllNotices() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NOTICES, null, null, null, null, null, COLUMN_ID + " DESC");
    }

    public boolean deleteNotice(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_NOTICES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return result > 0;
    }
}
