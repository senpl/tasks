package org.tasks.files;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;

import com.google.common.base.Strings;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;

import java.io.File;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastKitKat;

public class FileExplore extends InjectingAppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1000;
    private static final int REQUEST_DIRECTORY_PICKER = 1001;
    private static final int REQUEST_DOCUMENT_PICKER = 1002;

    public static final String EXTRA_URI = "extra_uri"; //$NON-NLS-1$
    public static final String EXTRA_MIME = "extra_mime";
    public static final String EXTRA_DISPLAY_NAME = "extra_display_name"; //$NON-NLS-1$
    public static final String EXTRA_DIRECTORY = "extra_directory"; //$NON-NLS-1$
    public static final String EXTRA_START_PATH = "extra_start_path";
    public static final String EXTRA_DIRECTORY_MODE = "extra_directory_mode"; //$NON-NLS-1$

    @Inject ActivityPermissionRequestor permissionRequestor;

    private boolean directoryMode;
    private String startPath;
    private String mime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            directoryMode = intent.getBooleanExtra(EXTRA_DIRECTORY_MODE, false);
            startPath = intent.getStringExtra(EXTRA_START_PATH);
            mime = intent.getStringExtra(EXTRA_MIME);

            if (permissionRequestor.requestFileWritePermission()) {
                launchPicker();
            }
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private void launchPicker() {
        File path = null;
        if (!Strings.isNullOrEmpty(startPath)) {
            path = new File(startPath);
        }
        if (path == null || !path.exists()) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                path = new File(Environment.getExternalStorageDirectory().toString());
            } else {
                path = Environment.getRootDirectory();
            }
        }

        if (directoryMode) {
            Intent i = new Intent(this, MyFilePickerActivity.class);
            i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
            i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
            startActivityForResult(i, REQUEST_DIRECTORY_PICKER);
        } else {
            if (atLeastKitKat()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(Strings.isNullOrEmpty(mime) ? "*/*" : mime);
                startActivityForResult(intent, REQUEST_DOCUMENT_PICKER);
            } else {
                Intent i = new Intent(this, MyFilePickerActivity.class);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, path.getAbsolutePath());
                startActivityForResult(i, REQUEST_FILE_PICKER);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_FILE_WRITE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchPicker();
                } else {
                    finish();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DOCUMENT_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                String displayName = getFilename(uri);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_URI, uri);
                intent.putExtra(EXTRA_DISPLAY_NAME, displayName);
                setResult(Activity.RESULT_OK, intent);
            }
            finish();
        } else if (requestCode == REQUEST_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                File file = com.nononsenseapps.filepicker.Utils.getFileForUri(uri);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_URI, uri);
                intent.putExtra(EXTRA_DISPLAY_NAME, file.getName());
                setResult(Activity.RESULT_OK, intent);
            }
            finish();
        } else if (requestCode == REQUEST_DIRECTORY_PICKER) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                File file =  com.nononsenseapps.filepicker.Utils.getFileForUri(uri);
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DIRECTORY, file.getAbsolutePath());
                setResult(Activity.RESULT_OK, intent);
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getFilename(Uri uri) {
        if (uri.getScheme().equals("file")) {
            return uri.getLastPathSegment();
        } else {
            Cursor cursor = getContentResolver().query(uri,
                    new String[] {DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return null;
        }
    }
}
