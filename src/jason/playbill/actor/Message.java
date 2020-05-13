package jason.playbill.actor;

import java.io.Serializable;

public class Message implements Serializable {
    Contact source;
    MessageType messageType;
    Object data;

    //private final Charset charset = StandardCharsets.UTF_8;

    public Contact getSource() {
        return source;
    }

    public void setSource(Contact source) {
        this.source = source;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    enum MessageType {
        dm,
        rollcall,
        confirmation,
        nextCue,
        empty
    }
}
