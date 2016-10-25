package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;

import com.todoroo.astrid.backup.TasksXmlImporter;

import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import javax.inject.Inject;

public class ImportTasksDialog extends InjectingNativeDialogFragment {

    public static ImportTasksDialog newImportTasksDialog(Uri uri, String filename) {
        ImportTasksDialog importTasksDialog = new ImportTasksDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_URI, uri);
        args.putString(EXTRA_FILENAME, filename);
        importTasksDialog.setArguments(args);
        return importTasksDialog;
    }

    private static final String EXTRA_URI = "extra_uri";
    private static final String EXTRA_FILENAME = "extra_filename";

    @Inject TasksXmlImporter xmlImporter;
    @Inject DialogBuilder dialogBuilder;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        Uri uri = arguments.getParcelable(EXTRA_URI);
        String filename = arguments.getString(EXTRA_FILENAME);
        ProgressDialog progressDialog = dialogBuilder.newProgressDialog();
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        setCancelable(false);
        xmlImporter.importTasks(getActivity(), uri, filename, progressDialog);
        return progressDialog;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
