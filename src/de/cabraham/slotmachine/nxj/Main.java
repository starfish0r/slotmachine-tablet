package de.cabraham.slotmachine.nxj;

import java.io.OutputStream;

import de.cabraham.slotmachine.nxj.Main.SlotMachinePacket.MsgType;
import de.cabraham.slotmachine.nxj.Main.SlotMachinePacket.MsgType;
import de.cabraham.slotmachine.nxj.Main.SlotMachinePacket.MsgType;
import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.Sound;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.RConsole;

public class Main {
  
  public static void main(String[] args) throws Exception {
    RConsole.openUSB(10000);
    RConsole.println("press any button to start");
    Button.waitForAnyPress();
    RConsole.println("starting");


    //BT kram auslagern
    //sensor ansteuern
    //schlonz implementieren
    //http://www.lejos.org/nxt/nxj/tutorial/ListenersAndEvents/Listeners_Events.htm


    BTConnection conn = null;
    int cnt = 0;
    try {
      while(conn == null) {
        LCD.drawString("waiting "+cnt, 0, 0);
        conn = Bluetooth.waitForConnection(5000, NXTConnection.RAW);
        LCD.drawString(String.valueOf(conn), 2, 2);
        cnt++;
      }
      Sound.beep();

      RConsole.println(conn.getAddress());
      LCD.drawString("conn:+"conn.getAddress(), 0, 0);
      LCD.refresh();

      OutputStream openOutputStream = conn.openOutputStream();

      BTReadThread readThread = new BTReadThread(openOutputStream, conn.openInputStream());
      readThread.start();

      while(true){
        openOutputStream.write(SlotMachinePacket.MsgType.HEARTBEAT);
        Thread.sleep(5000);
      }

      openOutputStream.flush();
      openOutputStream.close();
    } catch(Throwable ex){
      LCD.drawString(ex.getMessage(), 0, 15);
      //ex.printStackTrace();
      RConsole.println(ex.getMessage());
    }

    RConsole.close();
    Sound.beep();
  }

  public static doTheSchlonz(){
    //do async
  }


  class BTReadThread extends Thread {
    private OutputStream os;
    private InputStream is;
    public BTReadThread(OutputStream os, InputStream is) {
      this.is = is;
      this.os = os;
    }

    public void run() {
      try {
        while (true) {
          int cntHeartbeat = 0;
          int i = is.read();
          if (i == -1) {
            RConsole.println("read gave -1");
            return;
          }
          de.cabraham.slotmachine.nxj.Main.SlotMachinePacket.MsgType msgType = SlotMachinePacket.MsgType
              .valueFromOrdinal(i);
          switch (msgType) {
            case SlotMachinePacket.MsgType.HEARTBEAT:
              //cool: heartbeat reply
              LCD.drawString(cntHeartbeat++, 5, 0);
              break;
            case GAMEWINNER:
              Main.doTheSchlonz();
              break;
          }
        }
      } catch (Exception ex){
        RConsole.println(ex.getMessage());
      }

    }
  }

  public static  class SlotMachinePacket {
    SlotMachinePacket(MsgType msg){
      msgType = msg.ordinal();
    }
    int msgType;

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

    @Override
    public String toString() {
      MsgType msgType = MsgType.valueFromOrdinal(this.msgType);
      return String.valueOf(msgType);
    }
  }

}
