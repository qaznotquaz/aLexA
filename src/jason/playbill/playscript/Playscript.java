package jason.playbill.playscript;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * This class is a Java Object form of my playscript files, which are
 *      each .json files. These files and objects essentially encapsulate a
 *      single sub-act of an episode. Each playscript file should be named
 *      like so: [epXactY.qps]. For example, [ep1act1.qps].
 *
 * The Playscript class is responsible for containing the methods and
 *      functionality required by each script for their stage directions.
 *
 *
 *      this class may or may not be obsolete? we'll see.
 */
public class Playscript {
    final String relativePath = "C:\\Users\\JasonEaton\\IdeaProjects\\alex\\src\\jason\\playbill\\playscript";
    JSONObject header;
    JSONObject json;

    public Playscript(int episode, int act) throws IOException {
        String path = "scripts\\ep" + episode + "\\" + "ep" + episode + "act" + act + ".json";
        Stream<String> linesStream = Files.lines(Paths.get(relativePath, path));
        StringBuilder contentBuilder = new StringBuilder();
        linesStream.forEach(s -> contentBuilder.append(s).append("\n"));

        json = new JSONObject(contentBuilder.toString());

        header = json.getJSONObject("header");
        if (header.getInt("episode") != episode || header.getInt("act") != act){
            throw new IllegalStateException("Playscript at " + path + " has invalid header.");
        }
    }

    public JSONObject getInitialCue() {
        return header.getJSONObject("initial");
    }

    public JSONObject getDirection(String scene, String cue){
        return json.getJSONObject(scene).getJSONObject(cue);
    }

    public enum DirectionType {
        monologue,      // Actor speaking directly to the user.
        conversation,   // Actor speaking to one or more other Actors.
        enter,          // Actor is expected to be onstage.
        exit,           // Actor exits the stage.
        readReg,        // self explanatory
        writeReg,       // self explanatory
        readFile,       // self explanatory
        writeFile       // self explanatory
    }

    public enum Presence {
        idle,           // Actor is inactive and allowed to be onstage, but is not required.
        leading,        // Actor is active and the first one to act on this cue.
        responding,     // Actor is active, but is not the first to act.
        listening,      // Actor is inactive, but should be onstage.
        offstage        // Actor should be offstage.
    }
}
