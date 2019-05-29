package com.mathspp.appludus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

public class HelpDialogFragment extends DialogFragment {
    public static final int LIST_HELP = 0;
    public static final int INFO_HELP = 1;
    public static final int MAP_HELP = 2;
    public static final int DEFAULT_HELP = 3;
    // this array should match the order of the *_HELP integers
    public static final int[] STRING_IDS = {R.string.help_text_on_list, R.string.help_text_on_info,
                                            R.string.help_text_on_map, R.string.help_text_default};
    private static final String HELP_ID_TAG = "helpid";

    public static HelpDialogFragment newInstance(int helpCaseId) {
        HelpDialogFragment fragment = new HelpDialogFragment();

        Bundle args = new Bundle();
        args.putInt(HELP_ID_TAG, helpCaseId);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        int helpCase = getArguments() == null ? DEFAULT_HELP : getArguments().getInt(HELP_ID_TAG);
        int stringId = STRING_IDS[helpCase];
        builder.setMessage(stringId).setPositiveButton(R.string.ok_button_text, null);
        return builder.create();
    }
}
