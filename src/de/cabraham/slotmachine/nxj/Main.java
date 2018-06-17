package de.cabraham.slotmachine.nxj;

import java.io.OutputStream;

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
	    RConsole.println("wat");
	    Button.waitForAnyPress();
	    RConsole.println("wat2");
		BTConnection conn = null;
		int cnt = 0;
		try {
			while(conn == null) {
				LCD.drawString("waiting "+cnt, 0, 0);
				conn = Bluetooth.waitForConnection(5000, NXTConnection.RAW);
				LCD.drawString(String.valueOf(conn), 2, 2);
				cnt++;
			}
			//Sound.beep();
			//conn.setIOMode(NXTConnection.RAW);
			LCD.drawString("OK", 0, 0);
			LCD.refresh();
			RConsole.println(conn.getAddress());
			OutputStream openOutputStream = conn.openOutputStream();
			for(int i=0;i<5;i++) {
				openOutputStream.write(1);
				Thread.sleep(1000);
			}
			openOutputStream.flush();
			openOutputStream.close();
		} catch(Throwable ex){
			LCD.drawString(ex.getMessage(), 0, 15);
			ex.printStackTrace();
			RConsole.println(ex.getMessage());
		}
		
	    RConsole.close();
		Sound.beep();
	}

}
