package de.cabraham.slotmachine.nxj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.RConsole;

public class Main {

    private static final NXTRegulatedMotor MOTORPORT_SCHLONZ = Motor.A;
    private static final SensorPort SENSORPORT_LEVER = SensorPort.S1;
    private static final SensorPort SENSORPORT_RESET = SensorPort.S2;
    
    private static final int MOTOR_DEGREES = 70; 
    private static final int MOTOR_SPEED = MOTOR_DEGREES / 2; 
    
    static volatile boolean running = true;
    static volatile boolean hardExit = true;

    public static void main(String[] args) throws Exception {
        RConsole.openUSB(5000);
        //RConsole.println("press any button to start");
        //Button.waitForAnyPress();
        RConsole.println("starting");
        Sound.twoBeeps();

        //button stuff for the hardware buttons
        Button.LEFT.addButtonListener(new ButtonListener() {
            public void buttonReleased(Button b) {}
            public void buttonPressed(Button b) {
                MOTORPORT_SCHLONZ.setSpeed(MOTOR_SPEED);
                MOTORPORT_SCHLONZ.rotate(MOTOR_DEGREES);
            }
        });
        Button.RIGHT.addButtonListener(new ButtonListener() {
            public void buttonReleased(Button b) {}
            public void buttonPressed(Button b) {
                MOTORPORT_SCHLONZ.setSpeed(MOTOR_SPEED);
                MOTORPORT_SCHLONZ.rotate(-MOTOR_DEGREES);
            }
        });
        Button.ESCAPE.addButtonListener(new ButtonListener() {
            public void buttonReleased(Button b) {}
            public void buttonPressed(Button b) {
                if(hardExit){
                    System.exit(0);
                } else {
                    running = false;
                }
            }
        });

        //wait for a bluetooth client
        final BTConnectionManager btm = new BTConnectionManager();
        //this blocks until something connects!
        btm.setup();

        //setup touch sensor on S1 that waits for the lever to be pulled
        TouchSensorListener tslLever = new TouchSensorListener(new TouchSensor(SENSORPORT_LEVER),
                                                               new ButtonListener() {
            long lasttouch;
            public void buttonReleased(Button b) {}
            public void buttonPressed(Button b) {
                long now = System.currentTimeMillis();
                if(now - lasttouch > 5000) {
                    //only send the event max once per 5s, dont spam
                    btm.sendSensorTouched();
                    lasttouch = System.currentTimeMillis();
                }
            }
        });
        tslLever.setDaemon(true);
        tslLever.start();
        
        //setup touch sensor on S2 which makes the Schlonz-klappe float so I can reset it manually
        //3 seconds time
        TouchSensorListener tslReset = new TouchSensorListener(new TouchSensor(SENSORPORT_RESET),
                                                               new ButtonListener() {
            public void buttonReleased(Button b) {}
            public void buttonPressed(Button b) {
                MOTORPORT_SCHLONZ.flt(true);
                doSleep(3000L);
                MOTORPORT_SCHLONZ.stop();
            }
        });
        tslReset.setDaemon(true);
        tslReset.start();
        
        //modifies the button handler of button ESCAPE to set running=false instead of System.exit
        hardExit = false;
        while(running){
            btm.sendHeartbeat();
            doSleep(5000);
        }
        
        btm.shutdown();

        Sound.buzz();
        RConsole.close();
        System.exit(0);
    }

    public static void doTheSchlonz(){
        MOTORPORT_SCHLONZ.setSpeed(MOTOR_SPEED);
        MOTORPORT_SCHLONZ.rotate(MOTOR_DEGREES);
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
                doSleep(20L);
            }
        }
    }
    
    /**
     * sleep without try/catch
     * @param dur
     */
    static void doSleep(long dur){
        try {
            Thread.sleep(dur);
        } catch (InterruptedException e) {}
    }
}


