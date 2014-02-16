package com.example.ldav1s.android.audiorecordvumeter;

/*
    AudioRecordVuMeter -- simple audio recording audio display Android app

    Copyright 2014 Leo Davis

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import static java.lang.Math.*;
import java.util.Formatter;

public class MainActivity extends Activity {

   private ImageButton mRecordButton = null;
   private AudioRecord mRecorder = null;
   private int mRecorderBufSize;
   private ImageView mVuMeter = null;
   private int mVuMeterResIds[] = null;
   private static final int VU_METER_RES_CNT = 33;
   private static final int RECORDING_SAMPLE_RATE = 44100;

   private void onRecord(boolean start) {
      if (start) {
         startRecording();
      } else {
         stopRecording();
      }
   }

   private void startRecording() {
      mRecorderBufSize = AudioRecord.getMinBufferSize(RECORDING_SAMPLE_RATE,
                                                      AudioFormat.CHANNEL_IN_MONO,
                                                      AudioFormat.ENCODING_PCM_16BIT);
      mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                  RECORDING_SAMPLE_RATE,
                                  AudioFormat.CHANNEL_IN_MONO,
                                  AudioFormat.ENCODING_PCM_16BIT,
                                  mRecorderBufSize);
      mRecorder.startRecording();

      new AudioPoll().execute(mRecorderBufSize);
   }

   private void stopRecording() {
      mRecorder.stop();
   }

   private class AudioPoll extends AsyncTask<Integer, Integer, Void> {
     protected Void doInBackground(Integer... sizeInBytes) {
        short[] buffer = new short[sizeInBytes[0]/2];
        boolean status = true;
        int shortsRead;

        while (status && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
           if ((shortsRead = mRecorder.read(buffer, 0, buffer.length)) > 0) {
              int pmax = 1;
              int nmin = 0;
              final double LOG_2 = log(2.0);
              int i;

              // find the sample with the largest excursion in this slice
              for (i = 0; i < shortsRead; ++i) {
                 if (buffer[i] > 0) {
                    pmax = max(pmax, (int)buffer[i]);
                 } else {
                    nmin = min(nmin, (int)buffer[i]);
                 }
              }
              nmin = -max(nmin, -((int)Short.MAX_VALUE)); // make sure nmin is in [0..Short.MAX_VALUE]
              pmax = max(pmax, nmin);

              // convert the excursion to binary log scale.
              // the Vu Meter images go from vu00 to vu32.
              // BTW, these fine images were from Mixx <www.mixxx.org> ca. May 2006
              // when they were still at <mixx.sourceforge.net>.  I saved them for
              // an unreleased piece of software, and found them to be useful for this.
              double lb_pmax = log((double)pmax)/LOG_2; // lb_pmax [0..15)
              double c_lb_pmax = ceil(lb_pmax); // c_lb_pmax [0..15]
              publishProgress(Integer.valueOf(((int)c_lb_pmax+1)*2 - (int)round(c_lb_pmax - lb_pmax)));
           } else if (shortsRead < 0) {
              // error
              publishProgress(Integer.valueOf(0));
              status = false;
           } else {
              publishProgress(Integer.valueOf(0));
              status = false;
           }
        }
        mRecorder = null;
        return null;
     }

     protected void onProgressUpdate(Integer... progress) {
        mVuMeter.setImageResource(mVuMeterResIds[progress[0]]);
     }
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      mVuMeterResIds = new int[VU_METER_RES_CNT];
      int i;
      for (i = 0; i < VU_METER_RES_CNT; ++i) {
         Formatter vuName = new Formatter(new StringBuilder());
         vuName.format("vu%02d", i);
         mVuMeterResIds[i] = getResources().getIdentifier(vuName.toString(), "drawable", getPackageName());
         vuName.close();
      }

      mVuMeter = (ImageView) findViewById(R.id.vu_meter);
      mRecordButton = (ImageButton) findViewById(R.id.record_button);

      OnClickListener clicker = new OnClickListener() {
            boolean mStartRecording = true;
            final ImageButton b = mRecordButton;
            public void onClick(View v) {
               onRecord(mStartRecording);
               if (mStartRecording) {
                  b.setImageResource(R.drawable.stop);
                  b.setContentDescription(getString(R.string.content_desc_stop));
               } else {
                  b.setImageResource(R.drawable.record);
                  b.setContentDescription(getString(R.string.content_desc_record));
               }
               mStartRecording = !mStartRecording;
            }
         };

      mRecordButton.setOnClickListener(clicker);
   }
	
   @Override
   public void onPause() {
      super.onPause();
      if (mRecorder != null && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
         mRecorder.stop();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);
      return true;
   }
}
