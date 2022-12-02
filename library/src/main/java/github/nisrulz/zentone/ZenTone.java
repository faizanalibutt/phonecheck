package github.nisrulz.zentone;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The type Zen tone.
 */
public class ZenTone {
  private PlayToneThread playToneThread, playToneThread2;
  private boolean isThreadRunning = false;
  private Handler stopThread;
  private Runnable runnable;
  private Timer timer;
  private TimerTask timerTask;

  private static final ZenTone INSTANCE = new ZenTone();

  private ZenTone() {
//    stopThread = new Handler();
  }
  /**
   * Gets instance.
   *
   * @return the instance
   */
  public static ZenTone getInstance() {
    return INSTANCE;
  }

  /**
   * Generate pure tone
   *
   * @param freq the freq
   * @param duration the duration
   * @param volumne the volumne
   * @param toneStoppedListener the tone stopped listener
   */
  public void generate(int freq, final int duration, float volumne, boolean receive, Context context,
      ToneStoppedListener toneStoppedListener) {
    if (!isThreadRunning) {
      stop();
      playToneThread = new PlayToneThread(freq, duration, volumne, receive, context, toneStoppedListener);
      playToneThread.start();
      isThreadRunning = true;
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(duration * 1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          stop();
        }
      }).start();

//      stopThread.postDelayed(new Runnable() {
//        @Override public void run() {
//
//        }
//      }, duration * 1000);
    }
//    else {
//      stop();
//    }
  }

  public void generate2(int freq, final int duration, float volumne, boolean receive, Context context,
                        ToneStoppedListener toneStoppedListener) {
    if (!isThreadRunning) {
      stop();
      playToneThread = new PlayToneThread(freq, duration, volumne, receive, context, toneStoppedListener);
      playToneThread.start();
      isThreadRunning = true;

      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(duration * 1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          stop();
        }
      }).start();
    }
  }

  /**
   * Stop.
   */
  public void stop() {
    if (playToneThread != null) {
      playToneThread.stopTone();
      playToneThread.interrupt();
      playToneThread = null;
      isThreadRunning = false;
      Log.d("Zentone", "Tone Stopped 1");

//      stopThread.removeCallbacks(runnable);

//      if (timer != null){
//        timer.cancel();
//        timer = null;
//      }
    }
  }
  public void stopTone(){
    if (playToneThread != null){
      playToneThread.stopTone2();
      playToneThread.interrupt();
      playToneThread = null;
      isThreadRunning = false;
      Log.d("Zentone", "Tone Stopped 2");
    }
  }
}
