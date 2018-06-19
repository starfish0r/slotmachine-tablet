package de.cabraham.slotmachine.nxj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.MotorPort;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.RConsole;

public class Main {

    static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        RConsole.openUSB(10000);
        RConsole.println("press any button to start");
        Button.waitForAnyPress();
        RConsole.println("starting");


        //BT kram auslagern
        //sensor ansteuern
        //schlonz implementieren
        //http://www.lejos.org/nxt/nxj/tutorial/ListenersAndEvents/Listeners_Events.htm

        final BTConnectionManager btm = new BTConnectionManager();
        btm.setup();

        TouchSensor ts = new TouchSensor(SensorPort.S1);
        TouchSensorListener tsl = new TouchSensorListener(ts, new ButtonListener() {
            long lasttouch;
            public void buttonReleased(Button b) {
            }
            public void buttonPressed(Button b) {
                long now = System.currentTimeMillis();
                if(now - lasttouch > 5000) {
                    //only send the event max once per 5s, dont spam
                    btm.sendSensorTouched();
                    lasttouch = System.currentTimeMillis();
                }
            }
        });
        tsl.setDaemon(true);
        tsl.start();

        Button.ESCAPE.addButtonListener(new ButtonListener() {
            public void buttonReleased(Button b) {}
            public void buttonPressed(Button b) {
                running = false;
            }
        });

        while(running){
            btm.sendHeartbeat();
            Thread.sleep(5000);
        }
        
        btm.shutdown();

        Sound.buzz();
        RConsole.close();
        System.exit(0);
    }

    public static void doTheSchlonz(){
        Motor.A.rotate(360, true);
    }

    static class BTConnectionManager {
        BTConnection conn = null;
        OutputStream openOutputStream;

        public void setup() {
            int cnt = 0;
            try {
                while(conn == null) {
                    SLCD.setBT("waiting "+cnt);
                    conn = Bluetooth.waitForConnection(5000, NXTConnection.RAW);
                    cnt++;
                }
                Sound.beep();

                RConsole.println(conn.getAddress());
                SLCD.setBT("conn:"+conn.getAddress());

                openOutputStream = conn.openOutputStream();

                BTReadThread readThread = new BTReadThread(openOutputStream, conn.openInputStream());
                readThread.start();

            } catch(Throwable ex){
                SLCD.setException(ex.getMessage());
                //ex.printStackTrace();
                RConsole.println(ex.getMessage());
            }
        }
        
        public void shutdown() {
            try {
                openOutputStream.close();
            } catch (IOException e) {}
        }

        public void sendHeartbeat(){
            try {
                openOutputStream.write(MsgType.HEARTBEAT.ordinal());
                openOutputStream.flush();
            } catch(Throwable ex){
                RConsole.println(ex.getMessage());
                SLCD.setException(ex.getMessage());
            }
        }

        public void sendSensorTouched(){
            try {
                openOutputStream.write(MsgType.STARTGAMEPLZ.ordinal());
                openOutputStream.flush();
            } catch(Throwable ex){
                RConsole.println(ex.getMessage());
                SLCD.setException(ex.getMessage());
            }
        }
    }

    static class BTReadThread extends Thread {
        private OutputStream os;
        private InputStream is;
        public BTReadThread(OutputStream os, InputStream is) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            SLCD.setReadStatus("read ready");
            try {
                int cntHeartbeat = 0;
                while (true) {
                    int i = is.read();
                    SLCD.setReadStatus("rcv "+i);
                    if (i == -1) {
                        SLCD.setReadStatus("read gave -1");
                        RConsole.println("read gave -1");
                        return;
                    }
                    MsgType msgType = MsgType.valueFromOrdinal(i);
                    SLCD.setReadStatus("rcv "+msgType);
                    switch (msgType) {
                    case HEARTBEAT:
                        //cool: heartbeat reply
                        SLCD.setHeartbeat(""+cntHeartbeat);
                        cntHeartbeat++;
                        break;
                    case GAMEWINNER:
                        Main.doTheSchlonz();
                        break;
                    default:
                        break;
                    }
                }
            } catch (Throwable ex){
                SLCD.setException(ex.getMessage());
                RConsole.println(ex.getMessage());
            }

        }
    }

    public static class SlotMachinePacket {
        SlotMachinePacket(MsgType msg){
            msgType = msg.ordinal();
        }
        int msgType;

        @Override
        public String toString() {
            MsgType msgType = MsgType.valueFromOrdinal(this.msgType);
            return String.valueOf(msgType);
        }
    }

    public enum MsgType {
        HEARTBEAT,
        STARTGAMEPLZ,
        GAMEWINNER;

        public static MsgType valueFromOrdinal(int msgType) {
            for(MsgType t:values()){
                if(msgType==t.ordinal()){
                    return t;
                }
            }
            return null;
        }
    }

    public static class SLCD {

        private static String bt;
        private static String read;
        private static String hearbeat;
        private static String exception;

        public static void setBT(String string) {
            bt = string;
            draw();
        }

        public static void setException(String message) {
            exception = message;
            draw();
        }

        public static void setHeartbeat(String string) {
            hearbeat = string;
            draw();
        }

        public static void setReadStatus(String string) {
            read = string;
            draw();
        }

        public static void draw(){
            LCD.clear();
            LCD.drawString(String.valueOf(bt), 0, 0);
            LCD.drawString(String.valueOf(read), 0, 2);
            LCD.drawString(String.valueOf(hearbeat), 0, 3);
            LCD.drawString(String.valueOf(exception), 0, 4);
        }

    }

    public static class TouchSensorListener extends Thread {
        private TouchSensor ts;
        private ButtonListener bl;

        public TouchSensorListener(TouchSensor ts, ButtonListener buttonListener) {
            this.ts = ts;
            this.bl = buttonListener;
        }
        @Override
        public void run(){
            boolean pressed = false;
            for (;;) {
                boolean current = ts.isPressed();
                if(pressed != current){
                    pressed = current;
                    if(current) {
                        bl.buttonPressed(null);
                    } else {
                        bl.buttonReleased(null);
                    }
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {}
            }
        }
    }
}


