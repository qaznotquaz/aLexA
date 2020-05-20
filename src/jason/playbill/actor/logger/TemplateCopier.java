package jason.playbill.actor.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

//todo: label
public class TemplateCopier {
    final public static String[][] names = {{"Alexandra", "AlexandraSyaaniLaskea"},
            {"Alexander", "AlexanderPurppuranpunainenLaskea"},
            {"CallMeKey", "CallMeKey"},
            {"Fate", "Fate"}};

    //todo: label
    public static void main(String[] args) throws IOException {
        String templatePath = "C:\\Users\\JasonEaton\\IdeaProjects\\alex\\src\\jason\\playbill\\actor\\logger\\template.xml";
        //File template = new File();
        String targetpath;
        File targetFile;

        StringBuilder contentBuilder = new StringBuilder();
        String templateText;

        Stream<String> stream = Files.lines( Paths.get(templatePath), StandardCharsets.UTF_8);
        stream.forEach(s -> contentBuilder.append(s).append("\n"));
        templateText = contentBuilder.toString();

        for (String[] name : names) {
            targetpath = "C:\\Users\\JasonEaton\\IdeaProjects\\alex\\CastList\\" + name[0] + "\\src\\log4j2.xml";
            targetFile = new File(targetpath);
            String newText = templateText.replace("ACTORJOURNALSIGN", name[1]);

            FileOutputStream targetWriter = new FileOutputStream(targetFile);

            targetWriter.write(newText.getBytes());
        }
    }
}