package jason.playbill.playscript;

import jason.playbill.actor.Actor;

public class Direction {
    int lineNumber;
    String from;
    String to;
    DirectionType type;
    String[] data;

    public Direction(String lineRaw){
        // [linenumber::from::to::type::data1||data2||data3...]
        String[] lineSplit = lineRaw.split("::");
        lineNumber = Integer.parseInt(lineSplit[0]);
        from = lineSplit[1];
        to = lineSplit[2];
        type = DirectionType.valueOf(lineSplit[3]);
        data = lineSplit[4].split("\\|\\|");

        /*switch (type) {
            case monologue:
                data = new String[2];
                //todo monologue direction
                break;
            case dialogue:
                //todo dialogue direction
                break;
            case enter:
                //todo enter direction
                break;
            case exit:
                //todo exit direction
                break;
            case readReg:
                //todo readreg direction
                break;
            case writeReg:
                //todo writereg direction
                break;
            case readFile:
                //todo readfile direction
                break;
            case writeFile:
                //todo writefile direction
                break;
        }*/
    }

    enum DirectionType{
        monologue,     // Actor speaking directly to the user.
        conversation,  // Actor speaking to one or more other Actors.
        enter,         // Actor is expected to be onstage.
        exit,          // Actor exits the stage.
        readReg,       // self explanatory
        writeReg,      // self explanatory
        readFile,      // self explanatory
        writeFile      // self explanatory
    }
}
