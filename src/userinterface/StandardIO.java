package userinterface;

import java.io.*;

public class StandardIO implements Runnable {

    BufferedReader console = null;
    usercommand.UserCommand myCommand;

    public StandardIO() {
        console = new BufferedReader(new InputStreamReader(System.in));
        if (console == null) {
            System.err.println("No Standard Input device, exiting program.");
            System.exit(1);
        }
    }
    
    public void setCommand(usercommand.UserCommand myCommand) {
        this.myCommand = myCommand;
    }

    public String getUserInput() {
        String userInput = "no input";

        try {
            userInput = console.readLine();
            return userInput;
        } catch (IOException e) {
            System.err.println("Error reading from Standard Input device, exiting program.");
            System.exit(1);
        }
        return userInput;
    }

    public void display(String theResult) {
        System.out.println(theResult);
    }

    @Override
    public void run() {
        while (true) {
            String userInput = getUserInput();
            myCommand.execute(userInput);
        }
    }
}
