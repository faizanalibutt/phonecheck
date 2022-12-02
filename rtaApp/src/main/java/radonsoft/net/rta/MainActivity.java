package radonsoft.net.rta;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import github.nisrulz.zentone.ToneStoppedListener;
import github.nisrulz.zentone.ZenTone;

public class MainActivity extends AppCompatActivity {

    private EditText editTextFreq;
    private EditText editTextDuration;
    private int freq[] = {500, 1000, 2000, 4000};
    private int duration = 1;
    public boolean isPlaying = false;
    private FloatingActionButton myFab;
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextFreq = (EditText) findViewById(R.id.editTextFreq);
        editTextDuration = (EditText) findViewById(R.id.editTextDuration);
        SeekBar seekBarFreq = (SeekBar) findViewById(R.id.seekBarFreq);

        seekBarFreq.setMax(22000);

        SeekBar seekBarDuration = (SeekBar) findViewById(R.id.seekBarDuration);
        seekBarDuration.setMax(60);

        myFab = (FloatingActionButton) findViewById(R.id.myFAB);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleTonePlay();
            }
        });

        seekBarFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editTextFreq.setText(String.valueOf(progress));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                // Stop Tone
                ZenTone.getInstance().stop();
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                //Do nothing
            }
        });

        seekBarDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editTextDuration.setText(String.valueOf(progress));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                // Stop Tone
                ZenTone.getInstance().stop();
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });
    }
    private void handleTonePlay() {
//    String freqString = editTextFreq.getText().toString();
//    String durationString = editTextDuration.getText().toString();

        String freqString = String.valueOf(400);
        String durationString = String.valueOf(1);

        if (!"".equals(freqString) && !"".equals(durationString)) {
            if (!isPlaying) {
                myFab.setImageResource(R.drawable.ic_stop_white_24dp);
//        freq = Integer.parseInt(freqString);
//        duration = Integer.parseInt(durationString);

                for (int aFreq : freq) {
                    counter++;
                    duration = 2;
                    // Play Tone
                    ZenTone.getInstance().generate(aFreq, duration, 1.0f, RTA.receiver, getApplicationContext(), new ToneStoppedListener() {
                        @Override
                        public void onToneStopped() {
                            myFab.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                            isPlaying = false;
                        }
                    });
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isPlaying = true;
                }
            } else {
                // Stop Tone
                ZenTone.getInstance().stop();
                isPlaying = false;
                myFab.setImageResource(R.drawable.ic_play_arrow_white_24dp);
            }
        } else if ("".equals(freqString)) {
            Toast.makeText(MainActivity.this, "Please enter a frequency!", Toast.LENGTH_SHORT).show();
        } else if ("".equals(durationString)) {
            Toast.makeText(MainActivity.this, "Please enter duration!", Toast.LENGTH_SHORT).show();
        }
    }
}
