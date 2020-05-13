package jason.playbill.actor;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

public class Contact implements Serializable {
    String name;
    String color;
    int port;

    ChannelHandlerContext ctx;

    Contact(){
        this.name = "";
        this.color = "";
        this.port = -1;
        this.ctx = null;
    }

    Contact(Actor actor){
        this.name = actor.getName();
        this.color = actor.getColor();
        this.port = actor.getPort();
    }

    Contact(Contact contact, ChannelHandlerContext ctx) {
        this.name = contact.getName();
        this.color = contact.getColor();
        this.port = contact.getPort();
        this.ctx = ctx;
    }

    void dm(Contact source, String text) {
        Message message = new Message();
        message.setSource(source);
        message.setMessageType(Message.MessageType.dm);
        message.setData(text);

        ctx.writeAndFlush(message);
    }

    public void cueNext(Contact source) {
        Message message = new Message();
        message.setSource(source);
        message.setMessageType(Message.MessageType.nextCue);
        message.setData(null);

        ctx.writeAndFlush(message);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}