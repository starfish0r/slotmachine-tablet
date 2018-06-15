package de.cabraham.slotmachine.nxj;

import java.io.OutputStream;

import lejos.nxt.LCD;
import lejos.nxt.Sound;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

public class Main {
	
	public static void main(String[] args) throws Exception {
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
			//conn.setIOMode(NXTConnection.RAW);
			LCD.drawString("OK", 0, 0);
			LCD.refresh();
			OutputStream openOutputStream = conn.openOutputStream();
			openOutputStream.write(1);
			openOutputStream.flush();
			openOutputStream.close();
		}catch(Exception ex){
			LCD.drawString(ex.getMessage(), 0, 15);
		}
		Sound.beep();
	}

}
