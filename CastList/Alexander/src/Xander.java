import jason.playbill.actor.Actor;

import static jason.playbill.ConsoleColors.*;

public class Xander {
    public static void main(String[] args) throws InterruptedException {
        Object sync = new Object();
        Actor Xander = new Actor("Xander", ANSI_RED, 4001, sync);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sync) {
            sync.wait();
        }
    }
}
