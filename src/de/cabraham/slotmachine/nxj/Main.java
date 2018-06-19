package de.cabraham.slotmachine.nxj;

import java.io.InputStream;
import java.io.OutputStream;

import lejos.nxt.Button;
import lejos.nxt.ButtonListener;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.MotorPort;
import lejos.nxt.Sound;
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

    Button.ESCAPE.addButtonListener(new ButtonListener() {
        public void buttonReleased(Button b) {}
        public void buttonPressed(Button b) {
            running = false;
            //System.exit(0);
        }
      });

    BTConnection conn = null;
    int cnt = 0;
    try {
      while(conn == null) {
        SLCD.setBT("waiting "+cnt);
        conn = Bluetooth.waitForConnection(5000, NXTConnection.RAW);
        //LCD.drawString(String.valueOf(conn), 2, 2);
        cnt++;
      }
      Sound.beep();

      RConsole.println(conn.getAddress());
      SLCD.setBT("conn:"+conn.getAddress());

      OutputStream openOutputStream = conn.openOutputStream();

      BTReadThread readThread = new BTReadThread(openOutputStream, conn.openInputStream());
      readThread.start();

      while(running){
        openOutputStream.write(MsgType.HEARTBEAT.ordinal());
        openOutputStream.flush();
        Thread.sleep(5000);
      }

      Sound.buzz();
      openOutputStream.close();
    } catch(Throwable ex){
      SLCD.setException(ex.getMessage());
      //ex.printStackTrace();
      RConsole.println(ex.getMessage());
    }

    RConsole.close();
  }

  public static void doTheSchlonz(){
    Motor.A.rotate(90, true);
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

    //@Override
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
    }

    public static void setHeartbeat(String string) {
        hearbeat = string;
    }

    public static void setReadStatus(String string) {
        read = string;
    }

    public static void draw(){
        LCD.clear();
        LCD.drawString(bt, 0, 0);
        LCD.drawString(read, 0, 2);
        LCD.drawString(hearbeat, 0, 3);
        LCD.drawString(exception, 0, 4);
    }
      
      
  }
}


