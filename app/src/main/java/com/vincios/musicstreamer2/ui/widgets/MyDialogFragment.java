package com.vincios.musicstreamer2.ui.widgets;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.vincios.musicstreamer2.R;

public class MyDialogFragment extends DialogFragment{
    public static final String DIALOG_TITLE = "title";
    public static final java.lang.String DIALOG_MESSAGE = "message";
    public static final java.lang.String DIALOG_POSITIVE_BUTTON = "positive";
    public static final java.lang.String DIALOG_NEGATIVE_BUTTON = "negative";

    //TODO: https://developer.android.com/guide/topics/ui/dialogs.html#PassingEvents

    /* The activity that creates an instance of this dialog fragment must
         * implement this interface in order to receive event callbacks.
         * Each method passes the DialogFragment in case the host needs to query it. */
    public interface MyDialogListener {
         void onDialogPositiveClick(DialogFragment dialog);
         void onDialogNegativeClick(DialogFragment dialog);
    }

    public void setListener(MyDialogListener mListener) {
        this.mListener = mListener;
    }

    // Use this instance of the interface to deliver action events
    MyDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the MyDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*// Verify that the host activity implements the callback interface
        try {
            // Instantiate the MyDialogListener so we can send events to the host
            mListener = (MyDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement MyDialogListener");
        }*/
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        String title = arguments.getString(DIALOG_TITLE, getResources().getString(R.string.warning));
        String message = arguments.getString(DIALOG_MESSAGE, getResources().getString(R.string.confirm_message));
        String positive = arguments.getString(DIALOG_POSITIVE_BUTTON, getResources().getString(R.string.ok));
        String negative = arguments.getString(DIALOG_NEGATIVE_BUTTON, getResources().getString(R.string.cancel));

        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogPositiveClick(MyDialogFragment.this);
                    }
                })
                .setNegativeButton(negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogNegativeClick(MyDialogFragment.this);
                    }
                });

        return builder.create();
    }
}
