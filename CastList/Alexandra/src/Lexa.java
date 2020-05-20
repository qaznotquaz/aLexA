import jason.playbill.actor.Actor;

import static jason.playbill.ConsoleColors.*;

public class Lexa {
    public static void main(String[] args) throws InterruptedException {
        /*try {todo: get registry access working. move it to actor or playscript, not sure which yet
            ImplExample obj = new ImplExample();

            Hello stub = (Hello)UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello",stub);
            System.err.println("server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }*/

        /*Map<String, String> env = System.getenv();

        for (String key:env.keySet()){
            System.out.println(key + " " + env.get(key));
        }*/

        Object sync = new Object();
        Actor Lexa = new Actor("Lexa", ANSI_CYAN, 4000, sync);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (sync) {
            sync.wait();
        }

        //todo: move startup animation to actor
        /*System.out.print('/');
        Thread.sleep(250);
        System.out.print('\b');
        System.out.print('-');
        Thread.sleep(250);
        System.out.print('\b');
        System.out.print('\\');
        Thread.sleep(250);
        System.out.print('\b');*/
    }

    //todo: move environment access to either actor or playscript, not sure which yet
    /*private static String getComputerName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else return env.getOrDefault("HOSTNAME", "Unknown Computer");
    }

    todo: move process access to either actor or playscript, not sure which yet
    private static boolean isProcessRunning(String processName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
        Process process = processBuilder.start();
        String tasksList = toString(process.getInputStream());

        return tasksList.contains(processName);
    }

    todo: why do i have this?
    private static String toString(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A");
        String string = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        return string;
    }*/
}
