package jason.playbill.actor;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.awt.geom.Area;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import io.netty.util.concurrent.Future;
import jason.playbill.actor.logger.ActorLogger;
import jason.playbill.playscript.Playscript;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static jason.playbill.ConsoleColors.ANSI_RESET;

//todo: figure out what all you need to do

/**
 * The Actor class, which is the base class for each process that is to represent a character.
 */
public class Actor {
    final private ActorLogger logger = ActorLogger.create(Actor.class);
    /**
     * The group of ports that <u>all</u> actors will use and check for other actors.
     */
    final public int[] ports = {4000, 4001, 4002, 4003};
    final public String[] names = {"Lexa", "Xander", "Fate", "CallMeKey"};
    /**
     * A latch to keep the program from progressing too far before finishing connecting to other actors.
     */
    private final CountDownLatch connecting = new CountDownLatch(ports.length);
    /**
     * A latch to keep the program from progressing too far before setting up its own server.
     */
    private final CountDownLatch servStart = new CountDownLatch(1);

    private CountDownLatch reciting = new CountDownLatch(1);

    private CountDownLatch messageWaiting;
    /**
     * The Hostname that the actor will be connecting to.
     */
    private final String host = "localhost";
    /**
     * The ensemble of other active actors available for contact.
     */
    private static ArrayList<Contact> ensemble = new ArrayList<>();
    /**
     * The group of threads that this actor is actively connected to other servers through.
     */
    private static ArrayList<Thread> clients = new ArrayList<>();
    private EventLoopGroup clientGroup;
    private EventLoopGroup serverBossGroup;
    private EventLoopGroup serverWorkerGroup;

    Playscript currentScript;

    private String name;
    private String color;
    private int port;

    /**
     * Instantiates a new Actor.
     *
     * @param name  the new actor's name.
     * @param color the new actor's main color.
     * @param port  the port on which to open the actor's server.
     */
    public Actor(String name, String color, int port) {
        logger.actorDebug("");
        logger.actorDebug("Instantiating actor [{}] on port [{}]...", name, port);

        try {
            this.name = name;
            this.color = color;
            this.port = port;

            clientOpen(ports, this);
            connecting.await();

            if (findContact(name) != null) {
                throw new EnsembleCollisionException("There's already an actor with the name " + name);
            }

            if (findContact(port) != null) {
                throw new EnsembleCollisionException("There's already an actor on the port " + port);
            }

            Thread scriptReader = new Thread(new ScriptReader(this, 0, 0));
            Thread server = new Thread(new Server(this));
            server.start();
            servStart.await();
            logger.actorDebug("Successfully instantiated [{}].", name);
            scriptReader.start();
        } catch (Exception e) {
            logger.actorError(e);
            logger.trace(e);
        }
    }

    /**
     * Client open.
     *
     * @param ports the ports
     * @param owner the owner
     */
    public void clientOpen(int[] ports, Actor owner) {
        clientGroup = new NioEventLoopGroup();
        for (int port:ports) {
            Client runnable = new Client(owner, port);
            Thread client = new Thread(runnable);
            client.start();
            clients.add(client);
        }
    }

    /**
     * Client connect.
     *
     * @param owner  the owner
     * @param target the target
     */
    public void clientConnect(Actor owner, int target) {
        try {
            if (target == port) {
                connecting.countDown();
                return;
            }
            logger.actorDebug("Initializing a client for [{}] to roll-call port {}...", owner.getName(), target);
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(clientGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new ObjectEncoder());
                            p.addLast(new ObjectDecoder(Integer.MAX_VALUE,
                                    ClassResolvers.cacheDisabled(null)));
                            p.addLast(new ClientHandler(owner));

                            logger.actorDebug("Pipeline from [{}] to port {} initialized.", owner.getName(), target);
                        }
                    });
            logger.actorDebug("Bootstrap {} configured.", clientBootstrap.hashCode());

            logger.actorDebug("Roll-calling port {}...", target);
            ChannelFuture future = clientBootstrap.connect(host, target);
            future.channel().closeFuture().sync();
            logger.actorDebug("Client roll-calling port {} has closed.", target);
            connecting.countDown();
        } catch (Exception e) {
            logger.actorError(e);
            e.printStackTrace();
        }
    }

    /**
     * Server open.
     *
     * @param owner the owner
     * @throws InterruptedException the interrupted exception
     */
    public void serverOpen(Actor owner) throws InterruptedException {
        logger.actorDebug("Initializing server for [{}]...", owner.getName());
        serverBossGroup = new NioEventLoopGroup();
        serverWorkerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(serverBossGroup, serverWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ObjectDecoder(Integer.MAX_VALUE,
                                ClassResolvers.cacheDisabled(null)));
                        p.addLast(new ObjectEncoder());
                        p.addLast(new ServerHandler(owner));
                        logger.actorDebug("Channel pipeline [{}] to Server [{}] has been initialized.", ch.id(), owner.getName());
                    }
                });
        ChannelFuture f = serverBootstrap.bind(port);
        servStart.countDown();
        logger.actorDebug("Server for [{}] bound to port {}.", owner.getName(), port);
        f.channel().closeFuture().sync();
    }

    /**
     * Server close.
     */
    public void serverClose() {
        Future<?> workerShutdown = serverWorkerGroup.shutdownGracefully();
        Future<?> bossShutdown = serverBossGroup.shutdownGracefully();
        if (workerShutdown.isDone()){
            logger.actorDebug("Worker group shut down for [{}]", this.getName());
        } else {
            logger.actorError("Worker group for [{}] failed to shut down.", this.getName());
        }

        if (bossShutdown.isDone()){
            logger.actorDebug("Boss group shut down for {}", this.name);
        } else {
            logger.actorError("Boss group for [{}] failed to shut down.", this.getName());
        }
    }

    /**
     * Client close.
     */
    public void clientClose() {
        Future<?> clintShutdown = clientGroup.shutdownGracefully();

        if (clintShutdown.isDone()){
            logger.actorDebug("Client group shut down for {}", this.name);
        } else {
            logger.actorError("Client group for [{}] failed to shut down.", this.getName());
        }
    }

    /**
     * Actor exits the stage.
     */
    public void exit() throws InterruptedException {
        reciting.await();
        this.serverClose();
        this.clientClose();
        logger.actorDebug("Exit actor [{}].", this.getName());
    }

    /**
     * Speaks to the user.
     *
     * @param line the line
     */
    void speaks(String line) {
        System.out.println(color + line + ANSI_RESET);
        logger.actorInfo("[{}] spoke \"{}\" to the user.", this.getName(), line);
    }

    /**
     * Direct message another actor.
     *
     * @param line       the line
     * @param targetName the target name
     */
    public void dm(String line, String targetName) {
        Contact target = findContact(targetName);

        if (target != null) {
            target.dm(new Contact(this), line);
            logger.actorInfo("[{}] direct-messaged \"{}\" to [{}].", this.getName(), line, targetName);
        } else {
            logger.actorError("[{}] tried to direct-message [{}] but couldn't find them.", this.getName(), targetName);
            throw new NullPointerException("There isn't any actor by the name " + targetName);
        }
    }

    public void monologue(@NotNull JSONObject text) throws InterruptedException {
        int lineNum = 1;
        JSONObject line = text.getJSONObject(String.valueOf(lineNum));
        boolean speaking = true;

        while (speaking){
            this.speaks((line).getString("text"));
            Thread.sleep((line).getInt("delay"));

            lineNum++;
            try {
                line = text.getJSONObject(String.valueOf(lineNum));
            } catch (JSONException e){
                speaking = false;
            }
        }
    }

    /**
     * Gets an actor's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets an actor's representative color.
     *
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * Gets the port an actor is listening on.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Finds a contact in the actor's ensemble list by name.
     *
     * @param contactName the contact name
     * @return the contact
     */
    public Contact findContact(String contactName) {
        Optional<Contact> contact = ensemble.stream().filter(p -> p.getName().equals(contactName)).findAny();
        return contact.orElse(null);
    }

    /**
     * Finds a contact in the actor's ensemble list by port.
     *
     * @param contactPort the contact port
     * @return the contact
     */
    public Contact findContact(int contactPort) {
        Optional<Contact> contact = ensemble.stream().filter(p -> p.getPort() == (contactPort)).findAny();
        return contact.orElse(null);
    }

    @Override
    public String toString() {
        return "Actor " + color + name + ANSI_RESET + ", port " + port;
    }

    class ScriptReader implements Runnable {
        Playscript script;
        int currentEp;
        int currentAct;
        JSONObject initial;

        Actor owner;

        ScriptReader(Actor owner, int ep, int act) throws IOException {
            currentEp = ep;
            currentAct = act;
            script = new Playscript(currentEp, currentAct);
            this.owner = owner;

            initial = script.getInitialCue();
        }

        @Override
        public void run() {
            try {
                goToCue(initial.getString("scene"), initial.getString("cue"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //todo: remove this suppression when you're done filling out switch statements
        @SuppressWarnings("DuplicateBranchesInSwitch")
        public void goToCue(String sceneName, String cueName) throws InterruptedException {
            logger.actorInfo("[{}] going to cue {}/{}.", owner.getName(), sceneName, cueName);

            JSONObject cue = script.getDirection(sceneName, cueName);

            JSONObject presencesJson = cue.getJSONObject("actors");
            Playscript.Presence tempPre;
            Playscript.Presence myPresence = null;
            ArrayList<Playscript.Presence> otherPresences = new ArrayList<>();

            for(String name:names){
                tempPre = presencesJson.getEnum(Playscript.Presence.class, name);
                if(name.equals(owner.getName())){
                    myPresence = tempPre;
                } else {
                    otherPresences.add(tempPre);
                }
            }

            Playscript.DirectionType type = cue.getEnum(Playscript.DirectionType.class, "type");

            switch (Objects.requireNonNull(myPresence)) {
                case offstage -> {
                    owner.exit();
                }
                case idle -> {
                    return;
                }
                case leading -> {
                    //todo: presence case
                }
                case responding -> {
                    //todo: presence case
                }
                case listening -> {
                    //todo: presence case
                }
            }

            switch (type) {
                case monologue:
                    owner.monologue(cue.getJSONObject("text"));
                    JSONObject cuesTo = cue.getJSONObject("cuesTo");
                    goToCue(cuesTo.getString("scene"), cuesTo.getString("cue"));
                    break;
                case conversation:
                    //todo dialogue direction
                    break;
                case enter:
                    //todo enter direction
                    break;
                case exit:
                    //todo exit direction
                    break;
                case readReg:
                    //todo readreg direction
                    break;
                case writeReg:
                    //todo writereg direction
                    break;
                case readFile:
                    //todo readfile direction
                    break;
                case writeFile:
                    //todo writefile direction
                    break;
            }
        }
    }

    /**
     * The Client handler for incoming response messages.
     */
    class ClientHandler extends ChannelInboundHandlerAdapter {
        /**
         * The Actor who owns this client.
         */
        Actor owner;

        /**
         * Instantiates a new Client handler.
         *
         * @param owner the owner
         */
        ClientHandler(Actor owner) {
            logger.actorDebug("A new client handler has been instantiated by [{}].", owner.getName());
            this.owner = owner;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            logger.actorDebug("ChannelHandler {} has connected to its target.", this.hashCode());

            Message message = new Message();
            message.setSource(new Contact(owner));
            message.setMessageType(Message.MessageType.rollcall);
            message.setData(null);
            ctx.writeAndFlush(message);

            InetSocketAddress target = (InetSocketAddress)ctx.channel().remoteAddress();
            logger.actorInfo("Roll-call sent from [{}] to port {}.", owner.getName(), target.getPort());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Message receivedResponse = (Message) msg;
            Contact source = receivedResponse.getSource();

            switch (receivedResponse.getMessageType()) {
                case dm:
                    break;
                case rollcall:
                    logger.actorInfo("[{}] received a roll-call response from [{}], who is on port {}.",
                            owner.getName(), source.getName(), source.getPort());
                    ensemble.add(new Contact(source, ctx));
                    connecting.countDown();
                    break;
                case confirmation:
                    logger.actorInfo("[{}] received a confirmation from [{}] about {}.",
                            owner.getName(), source.getName(), receivedResponse.getData());
                    messageWaiting.countDown();
                    break;
            }
        }
    }

    /**
     * The Server handler for incoming request messages.
     */
    class ServerHandler extends ChannelInboundHandlerAdapter {
        /**
         * The Actor who owns this server.
         */
        Actor owner;

        /**
         * Instantiates a new Server handler.
         *
         * @param owner the owner
         */
        ServerHandler(Actor owner) {
            logger.actorDebug("[{}]'s server handler has been instantiated.", owner.getName());
            this.owner = owner;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Message received = (Message) msg;
            Contact source = received.getSource();

            Message response = new Message();
            response.setSource(new Contact(owner));

            switch (received.getMessageType()) {
                case dm:
                    logger.actorInfo("[{}] received the direct-message \"{}\" from [{}].",
                            owner.getName(), received.getData(), source.getName());
                    response.setMessageType(Message.MessageType.confirmation);
                    response.setData("message");
                    break;
                case rollcall:
                    logger.actorInfo("[{}] received a roll-call request from [{}], who is on port {}.",
                            owner.getName(), source.getName(), source.getPort());
                    ensemble.add(new Contact((Contact) received.getData(), ctx));
                    response.setMessageType(Message.MessageType.rollcall);
                    break;
                case confirmation:
                    break;
            }

            ctx.writeAndFlush(response);
        }
    }

    /**
     * The Server thread class for an actor to listen with.
     */
    class Server implements Runnable {
        /**
         * The Actor who owns this server.
         */
        Actor owner;

        /**
         * Instantiates a new Server.
         *
         * @param owner the owner
         */
        Server(Actor owner) {
            this.owner = owner;
        }

        public void run() {
            try {
                serverOpen(owner);
            } catch (Exception e) {
                logger.actorError(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * The Client thread class for an actor to contact others through.
     */
    class Client implements Runnable {
        /**
         * The Actor who owns this client.
         */
        Actor owner;
        /**
         * The other actor who this Client is connected to.
         */
        Contact contact;
        /**
         * The port which this Client intends to connect to.
         */
        int target;

        /**
         * Instantiates a new Client.
         *
         * @param owner  the owner
         * @param target the target
         */
        Client(Actor owner, int target) {
            this.owner = owner;
            this.target = target;
        }

        public void run() {
            try {
                clientConnect(owner, target);
            } catch (Exception e) {
                logger.actorError(e);
                e.printStackTrace();
            }
        }
    }
}