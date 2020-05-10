package jason.playbill.playscript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * This class is a Java Object form of a .qps (Qaz's PlayScript) file.
 * Each .qps file, and consequently each Playscript object, encapsulates
 *      a single sub-act of an episode. Each .qps file should be named
 *      like so: [epXactY.qps]. For example, [ep1act1.qps].
 *
 * The Playscript class is responsible for containing the methods and
 *      functionality required by each script for their stage directions.
 *
 * The formatting of a .qps file will be as follows. Each line
 *      [linenumber::from::to::type::data1||data2||data3...], e.g.
 *      [1::Lexa::Xander::dialogue::What's up?||50], or
 *      [97::Key::User::readReg::Computer\HKEY_CURRENT_USER\Software\Valve\Steam||AutoLoginUser]
 *
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
