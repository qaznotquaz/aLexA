package jason.playbill.actor;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

//todo: label
public class Contact implements Serializable {
    //todo: label
    String name;
    //todo: label
    String color;
    //todo: label
    int port;

    //todo: label
    ChannelHandlerContext ctx;

    //todo: label; is this necessary?
    Contact(){
        this.name = "";
        this.color = "";
        this.port = -1;
        this.ctx = null;
    }

    //todo: label
    Contact(Actor actor){
        this.name = actor.getName();
        this.color = actor.getColor();
        this.port = actor.getPort();
    }

    //todo: label
    Contact(Contact contact, ChannelHandlerContext ctx) {
        this.name = contact.getName();
        this.color = contact.getColor();
        this.port = contact.getPort();
        this.ctx = ctx;
    }

    //todo: label
    void dm(Contact source, String text) {
        Message message = new Message();
        message.setSource(source);
        message.setMessageType(Message.MessageType.dm);
        message.setData(text);

        ctx.writeAndFlush(message);
    }

    //todo: label
    //fixme: cuing other actors should direct them to a specific cue.
    public void cueNext(Contact source) {
        Message message = new Message();
        message.setSource(source);
        message.setMessageType(Message.MessageType.nextCue);
        message.setData(null);

        ctx.writeAndFlush(message);
    }

    //todo: label
    public String getName() {
        return name;
    }

    //todo: label
    public void setName(String name) {
        this.name = name;
    }

    //todo: label
    public String getColor() {
        return color;
    }

    //todo: label
    public void setColor(String color) {
        this.color = color;
    }

    //todo: label
    public int getPort() {
        return port;
    }

    //todo: label
    public void setPort(int port) {
        this.port = port;
    }
}