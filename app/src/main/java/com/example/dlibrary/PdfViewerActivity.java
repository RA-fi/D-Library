package com.example.dlibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    private static final int REQUEST_PICK_PDF_FILE = 202;

    private String pdfPathOrUrl = "";
    private String pdfTitle = "DUET PDF Reader";
    private boolean isOnlineRead = false;

    private WebView webView;
    private ScrollView scrollPdf;
    private LinearLayout containerPages, layoutLoading, layoutPageControls, layoutEmptyReader;
    private TextView txtLoadingMsg, txtDownloadPercentage, txtPageNumber;
    private ImageButton btnPrev, btnNext;

    private ParcelFileDescriptor fileDescriptor;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private final List<MaterialCardView> pageCardViews = new ArrayList<>();
    private int currentPageIndex = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        isOnlineRead = getIntent().getBooleanExtra("is_online_read", false);
        pdfPathOrUrl = getIntent().getStringExtra("pdf_path");
        if (pdfPathOrUrl == null) pdfPathOrUrl = "";
        pdfTitle = getIntent().getStringExtra("pdf_title");
        if (pdfTitle == null || pdfTitle.isEmpty()) pdfTitle = "DUET PDF Reader";

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(pdfTitle);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();

        if (getIntent().getData() != null) {
            pdfPathOrUrl = getIntent().getData().toString();
        }

        if (!pdfPathOrUrl.isEmpty()) {
            loadDocument();
        } else {
            showEmptyReaderState();
        }
    }

    private void initViews() {
        webView = findViewById(R.id.web_pdf_viewer);
        scrollPdf = findViewById(R.id.scroll_pdf);
        containerPages = findViewById(R.id.container_pages);

        layoutLoading = findViewById(R.id.layout_pdf_loading);
        layoutPageControls = findViewById(R.id.layout_page_controls);
        layoutEmptyReader = findViewById(R.id.layout_empty_reader);

        txtLoadingMsg = findViewById(R.id.txt_loading_msg);
        txtDownloadPercentage = findViewById(R.id.txt_download_percentage);
        txtPageNumber = findViewById(R.id.txt_page_number);

        btnPrev = findViewById(R.id.btn_prev_page);
        btnNext = findViewById(R.id.btn_next_page);

        MaterialButton btnOpenFile = findViewById(R.id.btn_open_file_manager);
        MaterialButton btnOpenUrl = findViewById(R.id.btn_open_url_dialog);
        MaterialButton btnWelcomeOpenFile = findViewById(R.id.btn_welcome_open_file);
        MaterialButton btnWelcomeOpenUrl = findViewById(R.id.btn_welcome_open_url);

        btnPrev.setOnClickListener(v -> scrollToPage(currentPageIndex - 1));
        btnNext.setOnClickListener(v -> scrollToPage(currentPageIndex + 1));
        txtPageNumber.setOnClickListener(v -> showJumpToPageDialog());

        View.OnClickListener openFileListener = v -> openFileManagerPicker();
        if (btnOpenFile != null) btnOpenFile.setOnClickListener(openFileListener);
        if (btnWelcomeOpenFile != null) btnWelcomeOpenFile.setOnClickListener(openFileListener);

        View.OnClickListener openUrlListener = v -> showEnterUrlDialog();
        if (btnOpenUrl != null) btnOpenUrl.setOnClickListener(openUrlListener);
        if (btnWelcomeOpenUrl != null) btnWelcomeOpenUrl.setOnClickListener(openUrlListener);

        scrollPdf.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (pageCardViews.isEmpty()) return;
            for (int i = 0; i < pageCardViews.size(); i++) {
                View card = pageCardViews.get(i);
                if (card.getTop() <= scrollY + (scrollPdf.getHeight() / 3) && card.getBottom() >= scrollY + (scrollPdf.getHeight() / 3)) {
                    currentPageIndex = i;
                    txtPageNumber.setText("Page " + (currentPageIndex + 1) + " / " + pageCardViews.size());
                    btnPrev.setEnabled(currentPageIndex > 0);
                    btnNext.setEnabled(currentPageIndex < pageCardViews.size() - 1);
                    break;
                }
            }
        });
    }

    private void openFileManagerPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            startActivityForResult(intent, REQUEST_PICK_PDF_FILE);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(Intent.createChooser(intent, "Select PDF File"), REQUEST_PICK_PDF_FILE);
        }
    }

    private void showEnterUrlDialog() {
        EditText input = new EditText(this);
        input.setHint("https://... or Firebase Storage PDF URL");
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("🌐 Enter Online PDF URL")
                .setMessage("Paste an online PDF file URL or Firebase Storage link to view in D Library:")
                .setView(input)
                .setPositiveButton("Load PDF", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        pdfPathOrUrl = url;
                        pdfTitle = "Online PDF Document";
                        if (getSupportActionBar() != null) getSupportActionBar().setTitle(pdfTitle);
                        loadDocument();
                    } else {
                        Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJumpToPageDialog() {
        if (pdfRenderer == null || pdfRenderer.getPageCount() <= 0) return;
        int total = pdfRenderer.getPageCount();

        EditText input = new EditText(this);
        input.setHint("Enter page (1 - " + total + ")");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("📖 Go to Page")
                .setMessage("Jump to page number (1 - " + total + "):")
                .setView(input)
                .setPositiveButton("Go", (dialog, which) -> {
                    try {
                        int p = Integer.parseInt(input.getText().toString().trim());
                        if (p >= 1 && p <= total) {
                            scrollToPage(p - 1);
                        } else {
                            Toast.makeText(this, "Page number out of range", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PDF_FILE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}

            pdfPathOrUrl = selectedUri.toString();
            pdfTitle = "Local Document";
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(pdfTitle);
            loadDocument();
        }
    }

    private void loadDocument() {
        layoutEmptyReader.setVisibility(View.GONE);
        if (pdfPathOrUrl.startsWith("http://") || pdfPathOrUrl.startsWith("https://") || pdfPathOrUrl.startsWith("gs://")) {
            downloadAndRenderRemotePdf(pdfPathOrUrl);
        } else if (!pdfPathOrUrl.isEmpty()) {
            loadLocalPdfUri(pdfPathOrUrl);
        } else {
            showEmptyReaderState();
        }
    }

    private void showEmptyReaderState() {
        layoutEmptyReader.setVisibility(View.VISIBLE);
        scrollPdf.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        txtPageNumber.setText("PDF Reader Ready");
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);
    }

    private void loadLocalPdfUri(String uriString) {
        webView.setVisibility(View.GONE);
        layoutPageControls.setVisibility(View.VISIBLE);
        scrollPdf.setVisibility(View.VISIBLE);

        layoutLoading.setVisibility(View.VISIBLE);
        if (isOnlineRead) {
            txtLoadingMsg.setText("Loading Online Reference Material...");
            txtDownloadPercentage.setText("Fetching...");
        } else {
            txtLoadingMsg.setText("Loading Local PDF Document...");
            txtDownloadPercentage.setText("Reading...");
        }

        executor.execute(() -> {
            try {
                Uri uri = Uri.parse(uriString);
                File tempFile = new File(getCacheDir(), "local_reader_" + System.currentTimeMillis() + ".pdf");

                boolean copied = false;
                try (InputStream inputStream = getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    if (inputStream != null) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        copied = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Stream copy error: " + e.getMessage());
                }

                if (!copied || tempFile.length() == 0) {
                    File directFile = new File(uri.getPath() != null ? uri.getPath() : uriString);
                    if (directFile.exists() && directFile.length() > 0) {
                        tempFile = directFile;
                    }
                }

                File finalFile = tempFile;
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutLoading.setVisibility(View.GONE);
                    try {
                        if (finalFile.exists() && finalFile.length() > 0) {
                            closePdfRenderer();
                            fileDescriptor = ParcelFileDescriptor.open(finalFile, ParcelFileDescriptor.MODE_READ_ONLY);
                            pdfRenderer = new PdfRenderer(fileDescriptor);
                            renderAllPages();
                        } else {
                            Toast.makeText(PdfViewerActivity.this, "Could not open local PDF file", Toast.LENGTH_SHORT).show();
                            showEmptyReaderState();
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error rendering local PDF", ex);
                        Toast.makeText(PdfViewerActivity.this, "Error rendering PDF document", Toast.LENGTH_SHORT).show();
                        showEmptyReaderState();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutLoading.setVisibility(View.GONE);
                    showEmptyReaderState();
                });
            }
        });
    }

    private void downloadAndRenderRemotePdf(String urlString) {
        webView.setVisibility(View.GONE);
        layoutPageControls.setVisibility(View.VISIBLE);
        scrollPdf.setVisibility(View.VISIBLE);

        layoutLoading.setVisibility(View.VISIBLE);
        txtLoadingMsg.setText("Fetching PDF from Online Library Cloud...");
        txtDownloadPercentage.setText("0%");

        if (urlString.contains("firebasestorage.googleapis.com") || urlString.startsWith("gs://")) {
            File cacheFile = new File(getCacheDir(), "firebase_pdf_" + System.currentTimeMillis() + ".pdf");
            FirebaseRepository.getInstance().downloadFileFromStorage(urlString, cacheFile, new FirebaseRepository.UploadCallback() {
                @Override
                public void onSuccess(String resultPath) {
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        layoutLoading.setVisibility(View.GONE);
                        try {
                            closePdfRenderer();
                            fileDescriptor = ParcelFileDescriptor.open(new File(resultPath), ParcelFileDescriptor.MODE_READ_ONLY);
                            pdfRenderer = new PdfRenderer(fileDescriptor);
                            renderAllPages();
                        } catch (Exception e) {
                            Log.e(TAG, "Error rendering Firebase PDF, falling back to HTTP", e);
                            loadViaHttpConnection(urlString);
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Firebase Storage direct download failed, falling back to HTTP", e);
                    loadViaHttpConnection(urlString);
                }
            });
            return;
        }

        loadViaHttpConnection(urlString);
    }

    private void loadViaHttpConnection(String urlString) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null) {
                        conn = (HttpURLConnection) new URL(newUrl).openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        conn.setConnectTimeout(20000);
                        conn.setReadTimeout(20000);
                    }
                }

                int fileLength = conn.getContentLength();
                File cacheFile = new File(getCacheDir(), "temp_reader_" + System.currentTimeMillis() + ".pdf");

                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(cacheFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long total = 0;
                    while ((read = in.read(buffer)) != -1) {
                        total += read;
                        out.write(buffer, 0, read);

                        if (fileLength > 0) {
                            int progress = (int) ((total * 100) / fileLength);
                            mainHandler.post(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    txtDownloadPercentage.setText(progress + "%");
                                }
                            });
                        }
                    }
                }

                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutLoading.setVisibility(View.GONE);
                    try {
                        if (cacheFile.exists() && cacheFile.length() > 0) {
                            closePdfRenderer();
                            fileDescriptor = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
                            pdfRenderer = new PdfRenderer(fileDescriptor);
                            renderAllPages();
                        } else {
                            loadGoogleDriveFallback(urlString);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error rendering downloaded PDF", e);
                        loadGoogleDriveFallback(urlString);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Remote download error", e);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutLoading.setVisibility(View.GONE);
                    loadGoogleDriveFallback(urlString);
                });
            }
        });
    }

    private void loadGoogleDriveFallback(String pdfUrl) {
        layoutPageControls.setVisibility(View.GONE);
        scrollPdf.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                layoutLoading.setVisibility(View.GONE);
            }
        });

        try {
            String encodedUrl = URLEncoder.encode(pdfUrl, "UTF-8");
            webView.loadUrl("https://docs.google.com/gview?embedded=true&url=" + encodedUrl);
        } catch (Exception e) {
            showEmptyReaderState();
        }
    }

    private final Object pdfLock = new Object();

    private void renderAllPages() {
        if (pdfRenderer == null) return;

        scrollPdf.setVisibility(View.VISIBLE);
        layoutEmptyReader.setVisibility(View.GONE);
        containerPages.removeAllViews();
        pageCardViews.clear();

        int totalPages = pdfRenderer.getPageCount();
        if (totalPages <= 0) return;

        currentPageIndex = 0;
        txtPageNumber.setText("Page 1 / " + totalPages);
        btnPrev.setEnabled(false);
        btnNext.setEnabled(totalPages > 1);

        executor.execute(() -> {
            for (int i = 0; i < totalPages; i++) {
                final int pageIndex = i;
                if (isFinishing() || isDestroyed()) break;

                Bitmap bitmap = null;
                synchronized (pdfLock) {
                    if (pdfRenderer == null || isFinishing() || isDestroyed()) break;
                    try {
                        PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);
                        int width = (int) (page.getWidth() * 1.25f);
                        int height = (int) (page.getHeight() * 1.25f);
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error rendering page " + pageIndex, e);
                        break;
                    }
                }

                if (bitmap != null) {
                    final Bitmap finalBitmap = bitmap;
                    mainHandler.post(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        MaterialCardView cardView = new MaterialCardView(this);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        cardParams.setMargins(0, 0, 0, 24);
                        cardView.setLayoutParams(cardParams);
                        cardView.setRadius(12f);
                        cardView.setCardElevation(4f);
                        cardView.setStrokeColor(android.graphics.Color.parseColor("#E0E0E0"));
                        cardView.setStrokeWidth(1);

                        ImageView imageView = new ImageView(this);
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
                        imageView.setAdjustViewBounds(true);
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        imageView.setImageBitmap(finalBitmap);

                        cardView.addView(imageView);
                        containerPages.addView(cardView);
                        pageCardViews.add(cardView);
                    });
                }
            }
        });
    }

    private void scrollToPage(int index) {
        if (index < 0 || index >= pageCardViews.size()) return;

        currentPageIndex = index;
        MaterialCardView targetCard = pageCardViews.get(currentPageIndex);
        scrollPdf.post(() -> scrollPdf.smoothScrollTo(0, targetCard.getTop()));

        txtPageNumber.setText("Page " + (currentPageIndex + 1) + " / " + pageCardViews.size());
        btnPrev.setEnabled(currentPageIndex > 0);
        btnNext.setEnabled(currentPageIndex < pageCardViews.size() - 1);
    }

    private void closePdfRenderer() {
        synchronized (pdfLock) {
            try {
                if (currentPage != null) {
                    currentPage.close();
                    currentPage = null;
                }
                if (pdfRenderer != null) {
                    pdfRenderer.close();
                    pdfRenderer = null;
                }
                if (fileDescriptor != null) {
                    fileDescriptor.close();
                    fileDescriptor = null;
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        closePdfRenderer();
        executor.shutdown();
        super.onDestroy();
    }
}
