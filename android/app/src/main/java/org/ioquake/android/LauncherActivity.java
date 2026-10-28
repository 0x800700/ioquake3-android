package org.ioquake.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LauncherActivity extends Activity {

    private static final String TAG = "LauncherActivity";
    private static final int REQUEST_CODE_PICK_ZIP = 1001;

    private static final String PREFS_NAME = "q3_launcher_prefs";
    private static final String KEY_DOWNLOAD_URL = "download_url";
    // Default mirror: GitHub Releases asset or community mirror (customizable via UI)
    private static final String DEFAULT_DOWNLOAD_URL = "https://github.com/0x800700/ioquake3-android/releases/download/v1.0.0/q3_resources.zip";

    private Handler mHandler;
    private SharedPreferences mPrefs;

    // UI elements
    private LinearLayout mSetupLayout;
    private LinearLayout mProgressLayout;
    private TextView mStatusTitle;
    private TextView mStatusDetail;
    private ProgressBar mProgressBar;
    private Button mBtnCancel;
    private TextView mUrlView;

    // State
    private volatile boolean mIsCancelled = false;
    private Thread mWorkerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } catch (Throwable ignored) {}

        mHandler = new Handler(Looper.getMainLooper());
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 1. Check if game files are already present
        if (isGameDataReady()) {
            launchGame();
            return;
        }

        // 2. Build setup & downloader UI
        buildUI();
        applyEdgeToEdgeFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyEdgeToEdgeFullscreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyEdgeToEdgeFullscreen();
        }
    }

    private void applyEdgeToEdgeFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }

            View decor = getWindow().peekDecorView();
            if (decor == null) {
                decor = getWindow().getDecorView();
            }

            if (decor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController controller = decor.getWindowInsetsController();
                    if (controller != null) {
                        controller.hide(WindowInsets.Type.systemBars());
                        controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                    }
                    getWindow().setDecorFitsSystemWindows(false);
                } else {
                    decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to apply edge-to-edge fullscreen", t);
        }
    }

    private boolean isGameDataReady() {
        File extFiles = getExternalFilesDir(null);
        if (extFiles == null) return false;
        File baseq3 = new File(extFiles, "baseq3");
        File pak0 = new File(baseq3, "pak0.pk3");
        return pak0.exists() && pak0.length() > 1024 * 1024; // At least 1MB
    }

    private void launchGame() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void buildUI() {
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.parseColor("#0C0C0E"));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(dpToPx(32), dpToPx(24), dpToPx(32), dpToPx(24));

        // Header Title
        TextView title = new TextView(this);
        title.setText("QUAKE III ARENA");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#FFA000")); // Quake Gold/Orange
        title.setShadowLayer(8, 0, 4, Color.parseColor("#99000000"));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Первоначальная настройка игровых ресурсов");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        subtitle.setTextColor(Color.parseColor("#B0BEC5"));
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dpToPx(4);
        subParams.bottomMargin = dpToPx(20);
        container.addView(subtitle, subParams);

        // Setup Layout (Visible initially)
        mSetupLayout = new LinearLayout(this);
        mSetupLayout.setOrientation(LinearLayout.VERTICAL);
        mSetupLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mSetupLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView desc = new TextView(this);
        desc.setText("Для запуска игры требуются базовые архивы (pak0.pk3) и данные модов.\nВыберите способ установки:");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        desc.setTextColor(Color.parseColor("#ECEFF1"));
        desc.setGravity(Gravity.CENTER);
        desc.setLineSpacing(dpToPx(4), 1f);
        mSetupLayout.addView(desc);

        // Buttons row
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnRowParams.topMargin = dpToPx(20);
        btnRowParams.bottomMargin = dpToPx(16);

        // Button 1: Download
        Button btnDownload = createStyledButton("⬇️ Скачать всё по сети", "#D84315", "#BF360C");
        btnDownload.setOnClickListener(v -> startDownload());
        LinearLayout.LayoutParams b1Params = new LinearLayout.LayoutParams(
            0, dpToPx(60), 1f
        );
        b1Params.rightMargin = dpToPx(12);
        buttonsRow.addView(btnDownload, b1Params);

        // Button 2: Import Local ZIP
        Button btnImport = createStyledButton("📁 Выбрать ZIP из памяти", "#37474F", "#263238");
        btnImport.setOnClickListener(v -> pickLocalZipFile());
        LinearLayout.LayoutParams b2Params = new LinearLayout.LayoutParams(
            0, dpToPx(60), 1f
        );
        b2Params.leftMargin = dpToPx(12);
        buttonsRow.addView(btnImport, b2Params);

        mSetupLayout.addView(buttonsRow, btnRowParams);

        // Download URL settings text
        mUrlView = new TextView(this);
        String currentUrl = mPrefs.getString(KEY_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL);
        mUrlView.setText("⚙️ Адрес загрузки: " + currentUrl + "  [Изменить]");
        mUrlView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mUrlView.setTextColor(Color.parseColor("#90A4AE"));
        mUrlView.setGravity(Gravity.CENTER);
        mUrlView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        mUrlView.setOnClickListener(v -> showEditUrlDialog());
        mSetupLayout.addView(mUrlView);

        container.addView(mSetupLayout);

        // Progress Layout (Hidden initially)
        mProgressLayout = new LinearLayout(this);
        mProgressLayout.setOrientation(LinearLayout.VERTICAL);
        mProgressLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mProgressLayout.setVisibility(View.GONE);
        LinearLayout.LayoutParams progLayoutParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progLayoutParams.topMargin = dpToPx(16);

        mStatusTitle = new TextView(this);
        mStatusTitle.setText("Подготовка...");
        mStatusTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mStatusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mStatusTitle.setTextColor(Color.parseColor("#FFD54F"));
        mStatusTitle.setGravity(Gravity.CENTER);
        mProgressLayout.addView(mStatusTitle);

        mStatusDetail = new TextView(this);
        mStatusDetail.setText("Подключение...");
        mStatusDetail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mStatusDetail.setTextColor(Color.parseColor("#CFD8DC"));
        mStatusDetail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dpToPx(4);
        detailParams.bottomMargin = dpToPx(12);
        mProgressLayout.addView(mStatusDetail, detailParams);

        mProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mProgressBar.setIndeterminate(false);
        mProgressBar.setMax(100);
        mProgressBar.setProgress(0);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(16)
        );
        barParams.bottomMargin = dpToPx(16);
        mProgressLayout.addView(mProgressBar, barParams);

        mBtnCancel = createStyledButton("Отмена", "#5D4037", "#3E2723");
        mBtnCancel.setOnClickListener(v -> cancelTask());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            dpToPx(160), dpToPx(44)
        );
        mProgressLayout.addView(mBtnCancel, cancelParams);

        container.addView(mProgressLayout, progLayoutParams);

        scroll.addView(container);
        root.addView(scroll, new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
    }

    private Button createStyledButton(String text, String startColor, String endColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setAllCaps(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(8));
        bg.setColor(Color.parseColor(startColor));
        bg.setStroke(dpToPx(1), Color.parseColor("#FFCC80"));
        btn.setBackground(bg);
        return btn;
    }

    private void showEditUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Настройка зеркала загрузки");
        builder.setMessage("Введите прямой URL адрес к q3_resources.zip:");

        final EditText input = new EditText(this);
        input.setText(mPrefs.getString(KEY_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL));
        input.setSingleLine(true);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                mPrefs.edit().putString(KEY_DOWNLOAD_URL, newUrl).apply();
                mUrlView.setText("⚙️ Адрес загрузки: " + newUrl + "  [Изменить]");
            }
        });
        builder.setNeutralButton("По умолчанию", (dialog, which) -> {
            mPrefs.edit().putString(KEY_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL).apply();
            mUrlView.setText("⚙️ Адрес загрузки: " + DEFAULT_DOWNLOAD_URL + "  [Изменить]");
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void switchToProgressView(String title) {
        mIsCancelled = false;
        mSetupLayout.setVisibility(View.GONE);
        mProgressLayout.setVisibility(View.VISIBLE);
        mStatusTitle.setText(title);
        mStatusDetail.setText("Подготовка...");
        mProgressBar.setProgress(0);
        mProgressBar.setIndeterminate(false);
    }

    private void switchToSetupView() {
        mProgressLayout.setVisibility(View.GONE);
        mSetupLayout.setVisibility(View.VISIBLE);
    }

    private void cancelTask() {
        mIsCancelled = true;
        if (mWorkerThread != null) {
            mWorkerThread.interrupt();
        }
        switchToSetupView();
        Toast.makeText(this, "Операция отменена", Toast.LENGTH_SHORT).show();
    }

    // --- 1. Downloader ---

    private void startDownload() {
        switchToProgressView("Загрузка игровых ресурсов");
        String downloadUrl = mPrefs.getString(KEY_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL);

        mWorkerThread = new Thread(() -> {
            File targetDir = getExternalFilesDir(null);
            File tempZip = new File(getCacheDir(), "q3_download.zip");

            try {
                Log.i(TAG, "Starting download from: " + downloadUrl);
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                int responseCode = conn.getResponseCode();
                // Handle manual redirect if necessary
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) {
                    String redirectUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    conn.connect();
                    responseCode = conn.getResponseCode();
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP ошибка: " + responseCode + " (" + conn.getResponseMessage() + ")");
                }

                long fileLength = conn.getContentLengthLong();
                InputStream in = new BufferedInputStream(conn.getInputStream(), 65536);
                OutputStream out = new FileOutputStream(tempZip);

                byte[] data = new byte[65536];
                long total = 0;
                long startTime = System.currentTimeMillis();
                long lastUpdate = 0;

                int count;
                while ((count = in.read(data)) != -1) {
                    if (mIsCancelled) {
                        in.close();
                        out.close();
                        tempZip.delete();
                        return;
                    }

                    out.write(data, 0, count);
                    total += count;

                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 300) {
                        lastUpdate = now;
                        long elapsed = now - startTime;
                        double speedMBps = elapsed > 0 ? ((double) total / (1024 * 1024)) / (elapsed / 1000.0) : 0;
                        int progress = fileLength > 0 ? (int) (total * 100 / fileLength) : 0;

                        String downloadedStr = String.format(Locale.US, "%.1f", total / (1024.0 * 1024.0));
                        String totalStr = fileLength > 0 ? String.format(Locale.US, "%.1f", fileLength / (1024.0 * 1024.0)) : "?";
                        String speedStr = String.format(Locale.US, "%.1f", speedMBps);

                        mHandler.post(() -> {
                            mProgressBar.setProgress(progress);
                            mStatusDetail.setText("Загружено: " + downloadedStr + " / " + totalStr + " МБ (" + speedStr + " МБ/с)");
                        });
                    }
                }

                out.flush();
                out.close();
                in.close();
                conn.disconnect();

                Log.i(TAG, "Download complete: " + tempZip.length() + " bytes. Starting unzip...");
                extractZipFile(tempZip, targetDir);

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                if (!mIsCancelled) {
                    mHandler.post(() -> {
                        switchToSetupView();
                        new AlertDialog.Builder(LauncherActivity.this)
                            .setTitle("Ошибка загрузки")
                            .setMessage("Не удалось загрузить файлы:\n" + e.getMessage() + "\n\nПроверьте подключение к сети или зеркало загрузки.")
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            } finally {
                if (tempZip.exists()) {
                    tempZip.delete();
                }
            }
        });
        mWorkerThread.start();
    }

    // --- 2. Local ZIP Import (SAF) ---

    private void pickLocalZipFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_ZIP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_ZIP && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                startImportFromUri(uri);
            }
        }
    }

    private void startImportFromUri(Uri uri) {
        switchToProgressView("Импорт локального архива");

        mWorkerThread = new Thread(() -> {
            File targetDir = getExternalFilesDir(null);
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("Не удалось открыть выбранный файл");

                extractZipStream(is, targetDir);

            } catch (Exception e) {
                Log.e(TAG, "Import failed", e);
                if (!mIsCancelled) {
                    mHandler.post(() -> {
                        switchToSetupView();
                        new AlertDialog.Builder(LauncherActivity.this)
                            .setTitle("Ошибка импорта")
                            .setMessage("Не удалось распаковать архив:\n" + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            }
        });
        mWorkerThread.start();
    }

    // --- 3. ZIP Extraction Core ---

    private void extractZipFile(File zipFile, File targetDir) throws Exception {
        FileInputStream fis = new FileInputStream(zipFile);
        extractZipStream(fis, targetDir);
    }

    private void extractZipStream(InputStream inStream, File targetDir) throws Exception {
        mHandler.post(() -> {
            mStatusTitle.setText("Распаковка игровых архивов...");
            mProgressBar.setIndeterminate(true);
        });

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inStream, 65536));
        ZipEntry entry;
        byte[] buffer = new byte[65536];
        int fileCount = 0;

        while ((entry = zis.getNextEntry()) != null) {
            if (mIsCancelled) {
                zis.close();
                return;
            }

            String entryName = entry.getName();
            File destFile = new File(targetDir, entryName);

            // Zip Slip prevention
            String canonicalDest = destFile.getCanonicalPath();
            String canonicalTarget = targetDir.getCanonicalPath();
            if (!canonicalDest.startsWith(canonicalTarget)) {
                throw new SecurityException("Zip Slip обнаружен: " + entryName);
            }

            if (entry.isDirectory()) {
                destFile.mkdirs();
            } else {
                File parent = destFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                final String currentName = entryName;
                mHandler.post(() -> mStatusDetail.setText("Извлечение: " + currentName));

                FileOutputStream fos = new FileOutputStream(destFile);
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    if (mIsCancelled) {
                        fos.close();
                        zis.close();
                        return;
                    }
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                fileCount++;
            }
            zis.closeEntry();
        }
        zis.close();

        Log.i(TAG, "Extracted " + fileCount + " files successfully");

        // Verify pak0.pk3 exists
        if (!isGameDataReady()) {
            throw new Exception("После распаковки pak0.pk3 не найден в папке baseq3!\nУбедитесь, что архив содержит папку baseq3/pak0.pk3.");
        }

        mHandler.post(() -> {
            mStatusTitle.setText("Готово! Запуск Quake III...");
            mStatusDetail.setText("Все ресурсы успешно установлены");
            mProgressBar.setProgress(100);
            mHandler.postDelayed(LauncherActivity.this::launchGame, 1000);
        });
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
