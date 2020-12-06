package com.example.masstouring.mapactivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.masstouring.R;

public class DeleteConfirmationDialog extends DialogFragment {
    private IDeleteConfirmationDialogCallback oCallback;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle(getString(R.string.deleteDialogTitle))
                .setMessage(getString(R.string.deleteDialogMessage))
                .setPositiveButton(getString(R.string.dialogPositive),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                oCallback.onPositiveClick();
                            }
                        })
                .setNegativeButton(getString(R.string.dialogNegative),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                oCallback.onNegativeClick();
                            }
                        })
                .create();
    }

    public void setCallback(IDeleteConfirmationDialogCallback aCallback){
        oCallback = aCallback;
    }

    public interface IDeleteConfirmationDialogCallback{
        void onPositiveClick();
        void onNegativeClick();
    }
}
