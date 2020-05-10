import jason.playbill.ConsoleColors;
import jason.playbill.actor.Actor;

public class Key {

    public static void main(String[] args) throws InterruptedException {
        Actor Key = new Actor("Key", ConsoleColors.ANSI_BRIGHT_BLACK, 4002);

        Thread.sleep(5000);
        //Xander.dm("Hey sis","Lexa");

        Key.exit();
    }
}
