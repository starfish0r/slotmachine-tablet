package cabraham.de.slotmachine.tablet2;

import java.io.Serializable;

class SlotMachinePacket implements Serializable {
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
