import jason.playbill.actor.Actor;

import java.util.Scanner;

import static jason.playbill.ConsoleColors.*;

public class Lexa {

    public static void main(String[] args) throws InterruptedException {
        //ProcessBuilder builder = new ProcessBuilder("notepad.exe");
        //Process process = builder.start();

        //Stream<ProcessHandle> nya = ProcessHandle.allProcesses();

        //nya.forEach(thing -> System.out.println(thing.info()));

        //System.out.println(isProcessRunning("Alexander"));

        /*try {
            ImplExample obj = new ImplExample();

            Hello stub = (Hello)UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello",stub);
            System.err.println("server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }*/

        /*Alexa me = new Alexa();
        me.says("Where's my brother?");

        Map<String, String> env = System.getenv();

        for (String key:env.keySet()){
            System.out.println(key + " " + env.get(key));
        }*/

        //int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        Actor Lexa = new Actor("Lexa", ANSI_CYAN, 4000);

        /*logger.error("this is a good thing");
        logger.debug("perhaps this too");
        Thread.sleep(5000);
        logger.trace("please");
        logger.info("unlikely");*/
        //Actor Addie = new Actor("Addie", ANSI_BG_PURPLE, 4003);

        //Scanner waiting = new Scanner(System.in);
        //waiting.nextLine();

        Lexa.exit();

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

    /*private static String getComputerName() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else return env.getOrDefault("HOSTNAME", "Unknown Computer");
    }

    private static boolean isProcessRunning(String processName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("tasklist.exe");
        Process process = processBuilder.start();
        String tasksList = toString(process.getInputStream());

        return tasksList.contains(processName);
    }

    private static String toString(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A");
        String string = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        return string;
    }*/
}
