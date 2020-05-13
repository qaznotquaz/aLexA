import static jason.playbill.ConsoleColors.*;
import jason.playbill.actor.Actor;

public class CallMeKey {

    public static void main(String[] args) throws InterruptedException {
        Object sync = new Object();
        Actor CallMeKey = new Actor("CallMeKey", ANSI_BRIGHT_BLACK, 4002, sync);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sync) {
            sync.wait();
        }
    }
}
