import jason.playbill.actor.Actor;

import static jason.playbill.ConsoleColors.*;
import static jason.playbill.ConsoleColors.ANSI_BRIGHT_BLACK;

public class Fate {

    public static void main(String[] args) throws InterruptedException {
        Object sync = new Object();
        Actor Fate = new Actor("Fate", ANSI_BRIGHT_YELLOW, 4003, sync);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sync) {
            sync.wait();
        }
    }
}
