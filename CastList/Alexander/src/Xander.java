import jason.playbill.ConsoleColors;
import jason.playbill.actor.Actor;
import jason.playbill.actor.Contact;

import java.util.Scanner;

import static jason.playbill.ConsoleColors.*;

import static java.lang.Thread.sleep;

public class Xander {
    public static void main(String[] args) throws InterruptedException {
        Object sync = new Object();
        Actor Xander = new Actor("Xander", ANSI_BRIGHT_RED, 4001, sync);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sync) {
            sync.wait();
        }
    }
}
