package jason.playbill.playscript;

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
    ArrayList<Direction> directions;

    Playscript(int episode, int act) throws IOException {
        String path = "ep" + episode + "\\" + "ep" + episode + "act" + act + ".qps";
        Stream<String> linesStream = Files.lines(Paths.get(relativePath, path));
        directions = new ArrayList<>();
        linesStream.forEach(line -> directions.add(new Direction(line)));
    }
}
