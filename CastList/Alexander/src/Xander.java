import jason.playbill.actor.Actor;
import jason.playbill.actor.Contact;

import java.util.Scanner;

import static jason.playbill.ConsoleColors.*;

import static java.lang.Thread.sleep;

public class Xander {
    public static void main(String[] args) throws InterruptedException {
        Actor Xander = new Actor("Xander", ANSI_BRIGHT_RED, 4001);
        Xander.dm("Hey sis","Lexa");

        Xander.exit();
    }
}
