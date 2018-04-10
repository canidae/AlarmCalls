package net.exent.alarmcalls;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Map;
import java.util.TreeMap;

public class AlarmActivity extends Activity {
    public static final String ALARM_PREFERENCES = "phone_numbers";
    private static final int PICK_CONTACT_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        checkPermissions();

        Button editCategoryButton = findViewById(R.id.addContactButton);
        editCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, PICK_CONTACT_CODE);
            }
        });
        RecyclerView recyclerView = findViewById(R.id.contactsView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ContactRecyclerViewAdapter());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (reqCode == PICK_CONTACT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = data.getData();
                if (contactData != null) {
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    if (c == null)
                        return;
                    if (c.moveToNext()) {
                        String phoneNumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                        SharedPreferences preferences = getSharedPreferences(ALARM_PREFERENCES, 0);
                        preferences.edit().putString(phoneNumber, "100;1;1").apply();
                        RecyclerView recyclerView = findViewById(R.id.contactsView);
                        recyclerView.invalidate();
                    }
                    c.close();
                }
            }
        }
    }

    private void checkPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        } else if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 1);
        }
    }

    public class ContactRecyclerViewAdapter extends RecyclerView.Adapter<ContactRecyclerViewAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_entry, parent, false);
            return new ContactRecyclerViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TreeMap<String, ?> sortedMap = new TreeMap<>(getSharedPreferences(ALARM_PREFERENCES, 0).getAll());
            int index = -1;
            for (Map.Entry<String, ?> entry : sortedMap.entrySet()) {
                if (++index == position) {
                    holder.phoneNumberText.setText(entry.getKey());
                    String data = entry.getValue().toString();
                    String[] values = data.split(";");
                    holder.volumeSeekBar.setProgress(Integer.parseInt(values[0]));
                    holder.flashSwitch.setChecked(values[1].equals("1"));
                    holder.vibrateSwitch.setChecked(values[2].equals("1"));
                    return;
                }
            }
        }

        @Override
        public int getItemCount() {
            return getSharedPreferences(ALARM_PREFERENCES, 0).getAll().size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final TextView phoneNumberText;
            final Switch flashSwitch;
            final Switch vibrateSwitch;
            final SeekBar volumeSeekBar;

            ViewHolder(View view) {
                super(view);
                phoneNumberText = view.findViewById(R.id.phoneNumberText);
                flashSwitch = view.findViewById(R.id.flashSwitch);
                vibrateSwitch = view.findViewById(R.id.vibrateSwitch);
                volumeSeekBar = view.findViewById(R.id.volumeSeekBar);

                final SharedPreferences preferences = getSharedPreferences(ALARM_PREFERENCES, 0);

                phoneNumberText.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        preferences.edit().remove(phoneNumberText.getText().toString()).apply();
                        // TODO: update recycler view
                        return true;
                    }
                });
                flashSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        String phoneNumber = phoneNumberText.getText().toString();
                        String data = preferences.getString(phoneNumber, null);
                        if (data != null) {
                            String[] values = data.split(";");
                            data = values[0] + ";" + (isChecked ? "1" : "0") + ";" + values[2];
                            preferences.edit().putString(phoneNumber, data).apply();
                        }
                        // TODO: update recycler view
                    }
                });
                vibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        String phoneNumber = phoneNumberText.getText().toString();
                        String data = preferences.getString(phoneNumber, null);
                        if (data != null) {
                            String[] values = data.split(";");
                            data = values[0] + ";" + values[1] + ";" + (isChecked ? "1" : "0");
                            preferences.edit().putString(phoneNumber, data).apply();
                        }
                        // TODO: update recycler view
                    }
                });
                volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        String phoneNumber = phoneNumberText.getText().toString();
                        String data = preferences.getString(phoneNumber, null);
                        if (data != null) {
                            String[] values = data.split(";");
                            data = progress + ";" + values[0] + ";" + values[1];
                            preferences.edit().putString(phoneNumber, data).apply();
                        }
                        // TODO: update recycler view
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            }
        }
    }
}
