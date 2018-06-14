package cabraham.de.slotmachine.tablet2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class BTConnection extends Thread {

    private static final String TAG = "BTConnection";
    private static final UUID SOCKET_UUID = UUID.randomUUID();


    private final Context context;
    private final String targetMacAddress;
    private final MainActivity.SlotMachinePacketCallback callback;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;

    private ObjectOutputStream outputStream;

    public BTConnection(Context context, String targetMacAddress, MainActivity.SlotMachinePacketCallback callback){
        this.context = context;
        this.callback = callback;
        this.targetMacAddress = targetMacAddress;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void run() {

        if(!btAdapter.isEnabled()) {
            Log.i(TAG, "enabling bluetooth");
            btAdapter.enable();
            while(!btAdapter.isEnabled()) {
                try{sleep(20);}catch (InterruptedException ex){}
            }
            Log.i(TAG, "adapter enabled");
        }

        while(true) {
            Log.i(TAG, "running loop");
            try {
                BluetoothDevice remoteDevice = btAdapter.getRemoteDevice(targetMacAddress);
                remoteDevice.setPin("1234".getBytes());
                if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.i(TAG, "no bond");
                    Toast.makeText(context, "Please bond/pair the device manually", Toast.LENGTH_SHORT).show();
                    //we rely on manual pairing
                    return;
                }
                if (btSocket == null || !btSocket.isConnected()) {
                    Log.i(TAG, "connecting with UUID "+SOCKET_UUID);
                    btSocket = remoteDevice.createRfcommSocketToServiceRecord(SOCKET_UUID);
                    btSocket.connect();
                }
                ObjectInputStream o = new ObjectInputStream(btSocket.getInputStream());
                this.outputStream = new ObjectOutputStream(btSocket.getOutputStream());
                while(true) {
                    Log.i(TAG, "reading object...");
                    SlotMachinePacket p = (SlotMachinePacket) o.readObject();
                    Log.i(TAG, "object received: "+p);
                    callback.accept(p);
                }
            } catch (Exception e) {
                Log.e(TAG, "run", e);
            }
        }

    }

    public void send(SlotMachinePacket p){
        try {
            outputStream.writeObject(p);
        } catch(Exception e){
            Log.e(TAG, "run", e);
        }
    }

}
