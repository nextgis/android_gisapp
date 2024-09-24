package com.nextgis.mobile.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.nextgis.mobile.R;

import java.lang.ref.WeakReference;

public class CustomPreference extends Preference {

    private WeakReference<TextView>  uidView = new WeakReference<>(null);
    String uid = "";

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.copy_uid_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textViewUID = (TextView) holder.findViewById(R.id.preference_uid);
        uidView = new WeakReference<>(textViewUID);
        if (uidView.get() != null)
            uidView.get().setText(uid);

        ImageView imageView = (ImageView) holder.findViewById(R.id.preference_icon);
        imageView.setImageResource(com.nextgis.maplibui.R.drawable.ic_action_content_copy);

        imageView.setOnClickListener(v -> {
            final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            final ClipData clip = ClipData.newPlainText("uid", uid);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), R.string.copied, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void setLayoutResource(int layoutResId) {
        super.setLayoutResource(layoutResId);
    }



    public void setUID(final String uid){
        this.uid = uid;
        if (uidView.get() != null)
            uidView.get().setText(uid);
    }
}
