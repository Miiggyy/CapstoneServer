
package usercommand;

import java.util.ArrayList;

public class UserCommand {
    userinterface.StandardIO myUI;
    server.Server myServer;

    public UserCommand(userinterface.StandardIO myUI, server.Server myServer) {
        this.myUI = myUI;
        this.myServer = myServer;
    }

    public void execute(String theCommand) {        
        String temp;
        ArrayList<String> clientIPAddress;
        
        switch (theCommand) {
            case "1": //QUIT
                myServer.stopServer();
                myUI.display("Quitting program by User command.");
                System.exit(-1);
                break;
            case "2": //LISTEN
                myUI.display("Server is now listening, ...");
                myServer.listen();
                break;
            case "3": //SET PORT
                myUI.display("Enter port number:");
                temp = myUI.getUserInput();
                myServer.setPort(Integer.parseInt(temp));
                break;
            case "4": //GET PORT
                myUI.display("The port number is " + myServer.getPort());
                break;
            case "5": //GET CLIENT IP ADDRESS
                clientIPAddress = myServer.getClientIPAddress();
                if(clientIPAddress != null){
                    for(int i = 0; i < clientIPAddress.size(); i++){
                        myUI.display(clientIPAddress.get(i));    
                    }             
                }
                break;
            case "6": //START SERVER SOCKET
                myServer.startServer();
                myUI.display("Server Socket has been created.");
                break;
            case "7": // Execute SQL                
                temp = myUI.getUserInput();
                myUI.display(myServer.executeSQL(temp));                
                break;
            case "/?": // display available commands
                myUI.display("1:\tQuit\n2:\tListen\n3:\tSet Port\n4:\tGet Port\n5:\tGet Client IP Addresses\n6:\tStart Server Socket\n7:\tExecute SQL\n");
                break;
            case "/x":
                myServer.disconnectAllClients();
                break;
            default:
                break;
        }
    }
}
