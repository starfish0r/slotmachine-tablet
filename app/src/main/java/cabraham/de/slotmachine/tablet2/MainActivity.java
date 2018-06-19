package cabraham.de.slotmachine.tablet2;


import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.LongStream;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /**
     * the MAC adress of the bluetooth target.
     * Valid Bluetooth hardware addresses must be upper case, in a format such as "00:11:22:33:AA:BB"
     */
    private static final String TARGET_MAC = "00:16:53:0F:30:F1";//"00:11:22:33:AA:BB";

    private boolean mButtonsVisible = true;
    private List<View> lstButtons = null;

    private final Handler mHideHandler = new Handler();
    private final Handler callbackHandler = new Handler();
    private final Handler slotHandler = new Handler();

    private BTConnection btconn = null;
    private int cntHeartbeat = 0;

    private List<TextView> slots = null;

    private static final List<String> GAMECHARS = Arrays.asList("1", "2", "3", "4");
    private static final Long SPINTIME_SLOT1_MS = 5000L;
    private static final Long SPINTIME_SLOT2_MS = 7000L;
    private static final Long SPINTIME_SLOT3_MS = 10000L;
    private static final Long[] SPINTIMES = {SPINTIME_SLOT1_MS, SPINTIME_SLOT2_MS, SPINTIME_SLOT3_MS};
    private static final boolean[] SPINFINISHED = {true, true, true};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        populateUILists();

        mHideHandler.postDelayed(()-> {
            findViewById(R.id.contentLayout).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }, 1000L);

        View btnFullscreen = findViewById(R.id.btnMakeFullScreen);
        View btnSlot1 = findViewById(R.id.slot1);
        btnFullscreen.setOnClickListener((view)->toggle());
        btnSlot1.setOnClickListener((view)->toggle());

        findViewById(R.id.btnRunNumbers).setOnClickListener((view)->startTheGame());
        findViewById(R.id.btnDoSchlonz).setOnClickListener((view)->triggerSchlonz());
        findViewById(R.id.btnConnectBT).setOnClickListener((view)->startConnectBT());

        TextView slot1 = findViewById(R.id.slot1);
        TextView slot2 = findViewById(R.id.slot2);
        TextView slot3 = findViewById(R.id.slot3);
        slots = Arrays.asList(slot1, slot2, slot3);
    }

    private void startConnectBT() {
        btconn = new BTConnection(this, TARGET_MAC, new SlotMachinePacketCallback(){
            @Override
            public void accept(SlotMachinePacket p) {
                Log.i(TAG, "accept "+p.msgType);
                if (p.msgType==SlotMachinePacket.MsgType.HEARTBEAT.ordinal()){
                    callbackHandler.post(()->statusHeartbeat());
                }

                if (p.msgType==SlotMachinePacket.MsgType.STARTGAMEPLZ.ordinal()){
                    callbackHandler.post(()->startTheGame());
                }
            }
        });
        btconn.start();
    }

    private void startTheGame() {
        for(int i=0; i<3; i++) {
            SPINFINISHED[i] = false;
            rotateText(i, new Random().nextInt(GAMECHARS.size()), SPINTIMES[i]);
        }
    }

    private void checkForWin() {
        if(slots.get(0).getText().equals(slots.get(1).getText()) && slots.get(1).getText().equals(slots.get(2).getText())){
            TextView tvStatus = findViewById(R.id.tvStatus);
            tvStatus.setText("WIN");
            triggerSchlonz();
        }
    }

    private void triggerSchlonz() {
        if(btconn != null) {
            btconn.send(new SlotMachinePacket(SlotMachinePacket.MsgType.GAMEWINNER));
        }
    }

    private void rotateText(int slotIndex, int charIndex, Long remainingSpinTime) {
        if(remainingSpinTime <= 0){
            finishRotation(slotIndex);
            return;
        }
        slotHandler.post(() -> {
            String text = GAMECHARS.get(charIndex % GAMECHARS.size());
            slots.get(slotIndex).setText(text);
        });

        //the less remainingspintime, the more delay so the rotation slows down, converging up to 200ms
        double nextDelayD = (((double)SPINTIME_SLOT1_MS-remainingSpinTime) / SPINTIME_SLOT1_MS)*200;
        long nextDelay = (long) nextDelayD;
        long nextDelay2 = nextDelay>50 ? nextDelay : 50; //but with a minimum of 50ms
        //Log.i(TAG, "nextDelay="+nextDelay);
        slotHandler.postDelayed(()->rotateText(slotIndex, charIndex+1, remainingSpinTime-nextDelay2), nextDelay);
    }

    private void finishRotation(int slotIndex) {
        SPINFINISHED[slotIndex] = true;
        for(int i=0;i<3;i++){
            if(!SPINFINISHED[i]){
                return;
            }
        }
        //all spins are done
        checkForWin();
    }

    private void statusHeartbeat() {
        TextView tvStatus = findViewById(R.id.tvStatus);
        tvStatus.setText("heartbeat "+(cntHeartbeat++));
    }

    private void populateUILists() {
        lstButtons = new ArrayList<>(4);
        lstButtons.add(findViewById(R.id.btnMakeFullScreen));
        lstButtons.add(findViewById(R.id.btnRunNumbers));
        lstButtons.add(findViewById(R.id.btnConnectBT));
        lstButtons.add(findViewById(R.id.tvStatus));
    }

    private void toggle() {
        if (mButtonsVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        setButtonListVisibility(View.GONE);
        mButtonsVisible = false;
    }

    private void show() {
        setButtonListVisibility(View.VISIBLE);
        mButtonsVisible = true;
    }

    private void setButtonListVisibility(int visi) {
        for(View v:lstButtons){
            v.setVisibility(visi);
        }
    }

    public Handler getCallbackHandler() {
        return callbackHandler;
    }

    public static abstract class SlotMachinePacketCallback {
        public abstract void accept(SlotMachinePacket p);
    }
}
