package cabraham.de.slotmachine.tablet2;

/**
 * This class was intended to be serializable so that i could send instances using Object(Input|Output)Streams.
 * Unfortunately the jvm on the NXT brick does not seem to support that. So now I send the ordinal of MsgType.
 */
class SlotMachinePacket {
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
