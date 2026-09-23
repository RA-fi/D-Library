package com.example.dlibrary;

public class Resource {
    private int id;
    private String title;
    private String author;
    private String category;
    private String type;
    private String filePath;
    private String coverImage;
    private String courseCode;
    private String department;
    private String uploadedBy;
    private String approvalStatus;
    private String uploadDate;
    private int downloadCount;
    private int favoriteCount;

    public Resource(int id, String title, String author, String category, String type, String filePath, String coverImage, String courseCode, String department, String uploadedBy, String approvalStatus, String uploadDate, int downloadCount, int favoriteCount) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.type = type;
        this.filePath = filePath;
        this.coverImage = coverImage;
        this.courseCode = courseCode;
        this.department = department;
        this.uploadedBy = uploadedBy;
        this.approvalStatus = approvalStatus;
        this.uploadDate = uploadDate;
        this.downloadCount = downloadCount;
        this.favoriteCount = favoriteCount;
    }

    public Resource(int id, String title, String author, String category, String type, String filePath, String courseCode, String department, String uploadedBy, String approvalStatus, String uploadDate, int downloadCount, int favoriteCount) {
        this(id, title, author, category, type, filePath, "", courseCode, department, uploadedBy, approvalStatus, uploadDate, downloadCount, favoriteCount);
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public String getFilePath() { return filePath; }
    public String getCoverImage() { return coverImage; }
    public String getCourseCode() { return courseCode; }
    public String getDepartment() { return department; }
    public String getUploadedBy() { return uploadedBy; }
    public String getApprovalStatus() { return approvalStatus; }
    public String getUploadDate() { return uploadDate; }
    public int getDownloadCount() { return downloadCount; }
    public int getFavoriteCount() { return favoriteCount; }

    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
}
