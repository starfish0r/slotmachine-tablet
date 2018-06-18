package cabraham.de.slotmachine.tablet2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.UUID;

public class BTConnection extends Thread {

    private static final String TAG = "BTConnection";
    private static final UUID SOCKET_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    //UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //is the one from getUUIDs
    //UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB"); the one from https://stackoverflow.com/questions/20009565/connect-to-android-bluetooth-socket
    //well-known: 00001101-0000-1000-8000-00805F9B34FB
    private final Context context;
    private final String targetMacAddress;
    private final MainActivity.SlotMachinePacketCallback callback;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;

    private OutputStream outputStream;

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
        BluetoothDevice remoteDevice = null;
        try {
            remoteDevice = btAdapter.getRemoteDevice(targetMacAddress);
        } catch (IllegalArgumentException e){
            Log.e(TAG, "error in remote device", e);
            return;
        }

        while(true) {
            Log.i(TAG, "running loop");
            try {
                //remoteDevice.setPin("1234".getBytes());
                /*Log.i(TAG, "bond state "+remoteDevice.getBondState());
                Log.i(TAG, "device name "+remoteDevice.getName());
                Log.i(TAG, "UUIDs "+ Arrays.toString(remoteDevice.getUuids()));
                Log.i(TAG, "class "+remoteDevice.getBluetoothClass());*/
                if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.i(TAG, "no bond");
                    //Toast.makeText(context, "Please bond/pair the device manually", Toast.LENGTH_SHORT).show();
                    //we rely on manual pairing
                    return;
                }
                if (btSocket == null || !btSocket.isConnected()) {
                    Log.i(TAG, "connecting with UUID "+SOCKET_UUID);
                    btSocket = remoteDevice.createRfcommSocketToServiceRecord(SOCKET_UUID);


                    //btSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(SOCKET_UUID);

                    //btSocket = (BluetoothSocket) remoteDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(remoteDevice,1);

                    //Constructor<BluetoothSocket> constructor = BluetoothSocket.class.getDeclaredConstructor(new Class[]{int.class, int.class, boolean.class, boolean.class, BluetoothDevice.class, int.class, ParcelUuid.class});
                    //constructor.setAccessible(true);
                    //btSocket = constructor.newInstance(BluetoothSocket.TYPE_RFCOMM, -1, true, true, remoteDevice, -1, new ParcelUuid(SOCKET_UUID));

                    btSocket.connect();
                    Log.i(TAG, "connected");
                }
                InputStream o = btSocket.getInputStream();
                this.outputStream = btSocket.getOutputStream();
                while(true) {
                    Log.i(TAG, "reading object...");
                    int i = o.read();
                    if(i == -1){
                        return;
                    }
                    SlotMachinePacket.MsgType msgType = SlotMachinePacket.MsgType.valueFromOrdinal(i);
                    if(msgType == SlotMachinePacket.MsgType.HEARTBEAT){
                        outputStream.write(SlotMachinePacket.MsgType.HEARTBEAT.ordinal());
                    }
                    SlotMachinePacket p = new SlotMachinePacket(msgType);
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
            outputStream.write(p.msgType);
        } catch(Exception e){
            Log.e(TAG, "run", e);
        }
    }

}
