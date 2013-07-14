package de.noova.callmustbeimportant;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MyPhoneStateListener extends PhoneStateListener {
    private Context context;

    public MyPhoneStateListener(Context context) {
        this.context = context;
    }

    // TODO: Replace all this last_callers-file foo with CallLog.Calls - magic
    private String filename = "last_callers";
    public static Boolean phoneRinging = false;
    private boolean didChange = false;
    private int ringMode, currVol, maxVol;

    public void onCallStateChanged(int state, String incomingNumber) {

        AudioManager amanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (state) {
            // Person answered phone. We expect him/her to be awake then.
            case TelephonyManager.CALL_STATE_OFFHOOK:
                try{
                File dir = context.getFilesDir();
                File f = new File(dir, filename);
                f.delete();
                } catch (Exception e) {}

                break;

            case TelephonyManager.CALL_STATE_IDLE:
                phoneRinging = false;
                if(didChange) {
                    amanager.setStreamVolume(AudioManager.STREAM_RING,currVol,AudioManager.FLAG_ALLOW_RINGER_MODES);
                    amanager.setRingerMode(ringMode);
                }
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                if(!phoneRinging) {
                    phoneRinging=true;
                    long now = System.currentTimeMillis();
                    String newFileContent ="";
                    int hasCalledBefore=0;

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

                        for(String line: oldFileContent.split("\n")) {
                            String[] zeile = line.split(",");
                            if( ((now - Long.parseLong(zeile[0])) / 60000) <=3) {
                                if(zeile[1].equals(incomingNumber)) hasCalledBefore++;
                                newFileContent+= line+"\n";
//                              Log.d("filecontent", line);
                            }
                        }
//                    Log.d("number of tries", String.valueOf(hasCalledBefore));
                    } catch (Exception e) { }

                    // Write new caller list to file.
                    newFileContent += String.valueOf(now) + "," + incomingNumber+"\n";
                    try {
                        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
                        fos.write(newFileContent.getBytes());
                        fos.close();
                    } catch (Exception e) { }


//                    if("+49308145867610".equals(incomingNumber)) {
                    if(hasCalledBefore>0) {
                        didChange=true;
                        ringMode = amanager.getRingerMode();
                        currVol = amanager.getStreamVolume(AudioManager.STREAM_RING);
                        maxVol = amanager.getStreamMaxVolume(AudioManager.STREAM_RING);
                        amanager.setStreamVolume(AudioManager.STREAM_RING,currVol+1,AudioManager.FLAG_SHOW_UI);
                        if(hasCalledBefore>1) amanager.setStreamVolume(AudioManager.STREAM_RING,maxVol,AudioManager.FLAG_SHOW_UI);
                    }
                }

            break;
        }
    }

}