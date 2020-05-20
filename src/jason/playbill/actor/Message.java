package jason.playbill.actor;

import java.io.Serializable;

//todo: label
public class Message implements Serializable {
    //todo: label
    Contact source;
    //todo: label
    MessageType messageType;
    //todo: label
    Object data;

    //todo: label
    public Contact getSource() {
        return source;
    }

    //todo: label
    public void setSource(Contact source) {
        this.source = source;
    }

    //todo: label
    public MessageType getMessageType() {
        return messageType;
    }

    //todo: label
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    //todo: label
    public Object getData() {
        return data;
    }

    //todo: label
    public void setData(Object data) {
        this.data = data;
    }

    //todo: label
    enum MessageType {
        dm,
        rollcall,
        confirmation,
        nextCue,
        empty
    }
}
