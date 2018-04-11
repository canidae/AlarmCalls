package net.exent.alarmcalls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import static net.exent.alarmcalls.AlarmActivity.ALARM_PREFERENCES;

/**
 * Created by canidae on 4/4/18.
 */

public class CallReceiver extends BroadcastReceiver {
    private static final AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build();
    private static int previousSoundLevel;
    private static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(getClass().getName(), "Received action: " + intent.getAction());
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            Log.d(getClass().getName(), "Action not accepted, returning");
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.d(getClass().getName(), "No bundle, returning");
            return;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            Log.d(getClass().getName(), "Unable to access audio manager, returning");
            return;
        }
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(bundle.getString(TelephonyManager.EXTRA_STATE))) {
            String phoneNo = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Cursor cursor = context.getContentResolver()
                    .query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNo)),
                            null,
                            null, null, null);
            String ringtone = null;
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    phoneNo = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.NORMALIZED_NUMBER));
                    ringtone = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.CUSTOM_RINGTONE));
                }
                cursor.close();
            }
            SharedPreferences preferences = context.getSharedPreferences(ALARM_PREFERENCES, 0);
            String data = preferences.getString(phoneNo, null);
            if (data == null)
                return;
            previousSoundLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            double volumePercent = Double.parseDouble(data.substring(0, data.indexOf(';'))) / 100.0;
            int volume = (int) (volumePercent * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.requestAudioFocus(audioFocusRequest);
            mediaPlayer = MediaPlayer.create(context, Uri.parse(ringtone));
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } else if (mediaPlayer != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousSoundLevel, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            mediaPlayer.setLooping(false);
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}
