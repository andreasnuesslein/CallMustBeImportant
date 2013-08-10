package de.noova.callmustbeimportant;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MyPhoneStateListener extends PhoneStateListener {
    private Context context;
    private SharedPreferences settings;

    public MyPhoneStateListener(Context context) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

    }


    // TODO: Replace all this last_callers-file foo with CallLog.Calls - magic
    private String filename = "last_callers";
    public static Boolean phoneRinging = false;
    private boolean didChange = false;
    private int ringMode, currVol, maxVol;

    public void onCallStateChanged(int state, String incomingNumber) {
        if (!settings.getBoolean("service_activated", true)) {
            return;
        }
        int timeout_between_calls = Integer.parseInt(settings.getString("timeout_between_calls", "3"));
        int escalate_1 = Integer.parseInt(settings.getString("escalate_1", "1"));
        int escalate_2 = Integer.parseInt(settings.getString("escalate_2", "2"));


        AudioManager audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (state) {
            // Person answered phone. We expect him/her to be awake then.
            case TelephonyManager.CALL_STATE_OFFHOOK:
                try {
                    File dir = context.getFilesDir();
                    File f = new File(dir, filename);
                    f.delete();
                } catch (Exception e) {
                }

                break;

            case TelephonyManager.CALL_STATE_IDLE:
                phoneRinging = false;
                if (didChange) {
                    audiomanager.setStreamVolume(AudioManager.STREAM_RING, currVol, AudioManager.FLAG_ALLOW_RINGER_MODES);
                    audiomanager.setRingerMode(ringMode);
                }
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                if (!phoneRinging) {
                    phoneRinging = true;
                    long now = System.currentTimeMillis();
                    String newFileContent = "";
                    int hasCalledBefore = 0;

                    // Get old caller list from file.
                    try {
                        FileInputStream fis = context.openFileInput(filename);
                        StringBuffer fileContentBuffer = new StringBuffer("");
                        byte[] buffer = new byte[1];
                        while (fis.read(buffer) != -1) {
                            fileContentBuffer.append(new String(buffer));
                        }
                        fis.close();
                        String oldFileContent = String.valueOf(fileContentBuffer);


                        // Evaluate caller list

                        for (String line : oldFileContent.split("\n")) {
                            String[] zeile = line.split(",");
                            if (((now - Long.parseLong(zeile[0])) / 60000) <= timeout_between_calls) {
                                if (zeile[1].equals(incomingNumber)) hasCalledBefore++;
                                newFileContent += line + "\n";
//                              Log.d("filecontent", line);
                            }
                        }
//                    Log.d("number of tries", String.valueOf(hasCalledBefore));
                    } catch (Exception e) {
                    }

                    // Write new caller list to file.
                    newFileContent += String.valueOf(now) + "," + incomingNumber + "\n";
                    try {
                        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
                        fos.write(newFileContent.getBytes());
                        fos.close();
                    } catch (Exception e) {
                    }


                    if (hasCalledBefore >= escalate_1) {
                        didChange = true;
                        ringMode = audiomanager.getRingerMode();
                        currVol = audiomanager.getStreamVolume(AudioManager.STREAM_RING);
                        maxVol = audiomanager.getStreamMaxVolume(AudioManager.STREAM_RING);
                        audiomanager.setStreamVolume(AudioManager.STREAM_RING, currVol + hasCalledBefore, AudioManager.FLAG_SHOW_UI);

                        if (hasCalledBefore >= escalate_2)
                            audiomanager.setStreamVolume(AudioManager.STREAM_RING, maxVol, AudioManager.FLAG_SHOW_UI);
                    }
                }

                break;
        }
    }

}