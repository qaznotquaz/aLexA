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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import jason.playbill.actor.logger.ActorLogger;
import jason.playbill.playscript.Playscript;
import org.json.JSONException;
import org.json.JSONObject;

import static jason.playbill.ConsoleColors.ANSI_RESET;

/**
 * The Actor class, which is the base class for each process that is to represent a character.
 */
public class Actor {
    final private ActorLogger logger = ActorLogger.create(Actor.class);
    /**
     * The group of ports that <u>all</u> actors will use and check for other actors.
     */
    final public int[] ports = {4000, 4001, 4002, 4003};
    /**
     * A latch to keep the program from progressing too far before finishing connecting to other actors.
     */
    private final CountDownLatch connecting = new CountDownLatch(ports.length);
    /**
     * A latch to keep the program from progressing too far before setting up its own server.
     */
    private final CountDownLatch servStart = new CountDownLatch(1);

    //private CountDownLatch messageWaiting;
    private final Object scriptSync = new Object();
    private final Object waitingForFriendsSync = new Object();
    private final Object leavingSync;
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

    //todo: label
    private Thread scriptReader;

    //todo: label
    private String name;
    //todo: label
    private String color;
    //todo: label
    private int port;

    //todo: label
    private final int formatWidth = 80;

    /**
     * Instantiates a new Actor.
     *
     * @param name  the new actor's name.
     * @param color the new actor's main color.
     * @param port  the port on which to open the actor's server.
     */
    public Actor(String name, String color, int port, Object leavingSync) {
        this.leavingSync = leavingSync;
        logger.actorDebug("");
        logger.actorDebug("Instantiating actor [{}] on port [{}]...", name, port);

        try {
            this.name = name;
            this.color = color;
            this.port = port;

            clientGroup = new NioEventLoopGroup();
            for (int target:ports) {
                clientOpen(this, target);
            }
            connecting.await();

            if (findContact(name) != null) {
                throw new EnsembleCollisionException("There's already an actor with the name " + name);
            }

            if (findContact(port) != null) {
                throw new EnsembleCollisionException("There's already an actor on the port " + port);
            }

            scriptReader = new Thread(new ScriptReader(this, 0, 0));
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

    //todo: better label
    /**
     * Open a new client
     * @param owner     the clients' owner
     * @param target    the target port to attempt to connect to
     */
    public void clientOpen(Actor owner, int target){
        Client runnable = new Client(owner, target);
        Thread client = new Thread(runnable);
        client.start();
        clients.add(client);
    }

    //todo: better label
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

    //todo: better label
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

    //todo: better label
    /**
     * Server close.
     */
    public void serverClose() {
        serverWorkerGroup.shutdownGracefully();
        serverBossGroup.shutdownGracefully();

        if (serverWorkerGroup.isShuttingDown()){
            logger.actorDebug("Worker group is shutting down for [{}]", this.getName());
        } else {
            logger.actorError("Worker group for [{}] failed to shut down.", this.getName());
        }

        if (serverBossGroup.isShuttingDown()){
            logger.actorDebug("Boss group is shutting down for {}", this.name);
        } else {
            logger.actorError("Boss group for [{}] failed to shut down.", this.getName());
        }
    }

    //todo: better label
    /**
     * Client close.
     */
    public void clientClose() {
        clientGroup.shutdownGracefully();

        if (clientGroup.isShuttingDown()){
            logger.actorDebug("Client group is shutting down for {}", this.name);
        } else {
            logger.actorError("Client group for [{}] failed to shut down.", this.getName());
        }
    }

    /**
     * Actor exits the stage.
     */
    public void exit() {
        this.serverClose();
        this.clientClose();
        logger.actorDebug("Exit actor [{}].", this.getName());

        synchronized (leavingSync){
            leavingSync.notify();
        }
    }

    /**
     * Speaks to the user.
     *
     * @param line the line
     */
    void speaks(String line) {
        System.out.printf("> %s%s%s\n",
                color, line, ANSI_RESET);
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

    //todo: label
    public void displayDmOut(Actor sender, String line) {
        String toPrint = String.format("%s%s%s > %s%s%s",
                sender.getColor(), sender.getName(), ANSI_RESET,
                sender.getColor(), line, ANSI_RESET);
        System.out.printf("%-" + formatWidth + "s\n", toPrint);
    }

    //todo: label
    public void displayDmIn(Contact sender, String line) {
        String toPrint = String.format("%s%s%s < %s%s%s",
                sender.getColor(), line, ANSI_RESET,
                sender.getColor(), sender.getName(), ANSI_RESET);
        System.out.printf("%" + formatWidth + "s\n", toPrint);
    }

    //todo: label
    public void cueNext(String targetName) {
        Contact target = findContact(targetName);

        if (target != null) {
            target.cueNext(new Contact(this));
            logger.actorInfo("[{}] sent [{}] to the next cue.", this.getName(), targetName);
        } else {
            logger.actorError("[{}] tried to send [{}] to the next cue, but couldn't find them.", this.getName(), targetName);
            throw new NullPointerException("There isn't any actor by the name " + targetName);
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

    //todo: label
    class ScriptReader implements Runnable {
        //todo: label
        Playscript script;
        int currentEp;
        int currentAct;
        //todo: label
        JSONObject initial;

        //todo: label
        Actor owner;

        //todo: label
        ScriptReader(Actor owner, int ep, int act) throws IOException {
            currentEp = ep;
            currentAct = act;
            script = new Playscript(currentEp, currentAct);
            this.owner = owner;

            initial = script.getInitialCue();
        }

        //todo: label
        @Override
        public void run() {
            try {
                goToCue(initial.getString("scene"), initial.getString("cue"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //todo: label
        //todo: remove this suppression when you're done filling out switch statements
        @SuppressWarnings("DuplicateBranchesInSwitch")
        public void goToCue(String sceneName, String cueName) throws InterruptedException {
            logger.actorInfo("[{}] going to cue {}/{}.", owner.getName(), sceneName, cueName);

            JSONObject cue = script.getDirection(sceneName, cueName);

            JSONObject presencesJson = cue.getJSONObject("actors");
            Playscript.Presence tempPre;
            Playscript.Presence myPresence = null;
            ArrayList<String> onstage = new ArrayList<>();

            for(String key:presencesJson.keySet()){
                tempPre = presencesJson.getEnum(Playscript.Presence.class, key);

                if(key.equals(name)){
                    myPresence = tempPre;
                } else {
                    if (tempPre != Playscript.Presence.offstage){
                        onstage.add(key);
                    }
                }
            }

            if(myPresence == null){
                myPresence = Playscript.Presence.offstage;
            }

            Playscript.DirectionType type = cue.getEnum(Playscript.DirectionType.class, "type");

            switch (Objects.requireNonNull(myPresence)) {
                //todo: explain case
                case offstage -> {
                    logger.actorInfo("[{}] has been instructed to exit the stage.", owner.getName());
                    owner.exit();
                    return;
                }
                //todo: explain case
                case idle -> {
                    //return;
                }
                //todo: write and explain case; is this necessary?
                case leading -> {
                }
                //todo: write and explain case; is this necessary?
                case responding -> {
                }
                //todo: write and explain case; is this necessary?
                case listening -> {
                }
            }

            synchronized (waitingForFriendsSync){
                ArrayList<String> notFound;
                notFound = waitForFriends(onstage);
                int pingOthers = 0;

                while (notFound.size() > 0){
                    pingOthers++;
                    if (pingOthers > 3){
                        for(int target:ports){
                            owner.clientOpen(owner, target);
                        }
                    }

                    //fixme waiting synchronization
                    speaks("i am waiting");
                    waitingForFriendsSync.wait(6000);

                    notFound = waitForFriends(onstage);
                }
            }

            JSONObject cuesTo;

            switch (type) {
                //todo: explain case
                case monologue -> {
                    if (myPresence == Playscript.Presence.leading) {
                        monologue(onstage, cue.getJSONObject("text"));
                        synchronized (scriptSync) {
                            scriptSync.notify();
                        }
                    } else {
                        synchronized (scriptSync) {
                            logger.actorDebug("[{}] is waiting for a monologue to finish.",
                                    owner.getName());
                            scriptSync.wait();
                            logger.actorDebug("The monologue that [{}] was waiting on has finished.",
                                    owner.getName());
                        }
                    }

                    cuesTo = cue.getJSONObject("cuesTo");
                    goToCue(cuesTo.getString("scene"), cuesTo.getString("cue"));
                }
                //todo: explain case
                case conversation -> {
                    converse(onstage, cue.getJSONObject("text"));
                    cuesTo = cue.getJSONObject("cuesTo");
                    goToCue(cuesTo.getString("scene"), cuesTo.getString("cue"));
                }
                //todo: write and explain case
                case enter -> {
                }
                //todo: write and explain case
                case exit -> {
                }
                //todo: write and explain case
                case readReg -> {
                }
                //todo: write and explain case
                case writeReg -> {
                }
                //todo: write and explain case
                case readFile -> {
                }
                //todo: write and explain case
                case writeFile -> {
                }
            }
        }

        //todo: label
        public void monologue(ArrayList<String> onstage, JSONObject text) throws InterruptedException {
            int lineNum = 1;
            JSONObject line = text.getJSONObject(String.valueOf(lineNum));
            boolean speaking = true;

            while (speaking){
                Thread.sleep(line.getInt("delay"));
                owner.speaks(line.getString("text"));

                lineNum++;
                try {
                    line = text.getJSONObject(String.valueOf(lineNum));
                } catch (JSONException e){
                    speaking = false;
                }
            }

            for (String member:onstage) {
                cueNext(member);
            }
        }

        //todo: label
        public void converse(ArrayList<String> onstage, JSONObject text) throws InterruptedException {
            logger.actorInfo("[{}] entering conversation.",
                    owner.getName());

            for (String member:onstage) {
                if(findContact(member) == null){
                    logger.actorError("[{}] needs to converse with [{}], but they're not onstage.",
                            owner.getName(), member);
                    return;
                } else {
                    logger.actorInfo("[{}] will be conversing with [{}].",
                            owner.getName(), member);
                }
            }

            int lineNum = 1;
            JSONObject line = text.getJSONObject(String.valueOf(lineNum));
            boolean conversing = true;

            while(conversing){
                if (line.getString("from").equals(owner.name)){
                    Thread.sleep(line.getInt("delay"));
                    displayDmOut(owner, line.getString("text"));
                    for (String member:onstage) {
                        dm(line.getString("text"), member);
                    }
                } else {
                    synchronized (scriptSync) {
                        scriptSync.wait();
                    }
                }

                lineNum++;
                try {
                    line = text.getJSONObject(String.valueOf(lineNum));
                } catch (JSONException e){
                    conversing = false;
                }
            }
        }

        /**
         * Checks whether everyone is on stage that needs to onstage.
         * @param waitingFor    The list of people that need to be onstage.
         * @return              The list of people who were not found.
         */
        public ArrayList<String> waitForFriends(ArrayList<String> waitingFor){
            ArrayList<String> notFound = new ArrayList<>();

            for (String contact:waitingFor) {
                if(findContact(contact) == null){
                    logger.actorError("[{}] is expecting [{}] to be onstage, but they're not.",
                            owner.getName(), contact);
                    notFound.add(contact);
                } else {
                    logger.actorInfo("[{}] has found [{}] onstage.",
                            owner.getName(), contact);
                }
            }

            return notFound;
        }
    }

    /**
     * Client handler class for incoming responses from other actors' servers.
     */
    class ClientHandler extends ChannelInboundHandlerAdapter {
        /**
         * The Actor who owns the client who owns this handler.
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

        //todo: label
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

        //todo: label
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Message receivedResponse = (Message) msg;
            Contact source = receivedResponse.getSource();

            switch (receivedResponse.getMessageType()) {
                //todo: explain case
                case dm -> {
                }
                //todo: explain case
                case rollcall -> {
                    logger.actorInfo("[{}] received a roll-call response from [{}], who is on port {}.",
                            owner.getName(), source.getName(), source.getPort());
                    if (findContact(source.getName()) == null) {
                        synchronized (waitingForFriendsSync){
                            ensemble.add(new Contact(source, ctx));
                            logger.actorDebug("[{}] added [{}] to their contact list.",
                                    owner.getName(), source.getName());
                            waitingForFriendsSync.notify();
                        }
                        if (connecting.getCount() != 0) {
                            connecting.countDown();
                        }
                    } else {
                        logger.actorError("[{}] did NOT add [{}] to their contact list.");
                    }
                }
                //todo: explain case
                case confirmation ->
                    logger.actorInfo("[{}] received a confirmation from [{}] about {}.",
                            owner.getName(), source.getName(), receivedResponse.getData());
            }
        }
    }

    /**
     * Server handler class for incoming request messages from other actors' clients.
     */
    class ServerHandler extends ChannelInboundHandlerAdapter {
        /**
         * The Actor who owns the server who owns this handler.
         */
        Actor owner;

        /**
         * Instantiates a new Server handler.
         *
         * @param owner the owner
         */
        ServerHandler(Actor owner) {
            this.owner = owner;
            logger.actorDebug("[{}]'s server handler has been instantiated.", owner.getName());
        }

        //todo: label
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Message received = (Message) msg;
            Contact source = received.getSource();

            Message response = new Message();
            response.setSource(new Contact(owner));

            switch (received.getMessageType()) {
                //todo: explain case
                case dm -> {
                    logger.actorInfo("[{}] received the direct-message \"{}\" from [{}].",
                            owner.getName(), received.getData(), source.getName());
                    displayDmIn(source, (String) received.getData());
                    response.setMessageType(Message.MessageType.confirmation);
                    response.setData("message");
                    synchronized (scriptSync) {
                        scriptSync.notify();
                    }
                }
                //todo: explain case
                case rollcall -> {
                    logger.actorInfo("[{}] received a roll-call request from [{}], who is on port {}.",
                            owner.getName(), source.getName(), source.getPort());
                    if (findContact(source.getName()) == null) {
                        clientOpen(owner, source.getPort());
                    } else {
                        logger.actorError("[{}] already has [{}] in their contact list.",
                                owner.getName(), source.getName());
                    }
                    response.setMessageType(Message.MessageType.rollcall);
                    response.setData(null);
                }
                //todo: explain case
                case confirmation -> {
                }
                //todo: explain case
                case nextCue -> {
                    //todo: this is a mess
                    /*String[] cue = (String[])receivedResponse.getData();
                    logger.actorInfo("[{}] received cue {}/{} from [{}].",
                            owner.getName(), cue[0], cue[1], source.getName());
                    owner.goToCue(cue[0], cue[1]);*/
                    synchronized (scriptSync) {
                        scriptSync.notify();
                    }
                    response.setMessageType(Message.MessageType.confirmation);
                    response.setData("nextCue");
                }
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

        //todo: label
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

        //todo: label
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