package servertest;

public class ServerTest {

    public static void main(String[] args) {
        userinterface.StandardIO myUI = new userinterface.StandardIO();
        clientcommand.ClientCommand myClientCommand = new clientcommand.ClientCommand(myUI);        
        server.Server myServer = new server.Server(5555, 1, myClientCommand);
        usercommand.UserCommand myCommand = new usercommand.UserCommand(myUI, myServer);
        myUI.setCommand(myCommand);
        Thread myUIthread = new Thread(myUI);
        myUIthread.start();     
        myUI.display("1:\tQuit\n2:\tListen\n3:\tSet Port\n4:\tGet Port\n5:\tGet Client IP Addresses\n6:\tStart Server Socket\n7:\tExecute SQL\n");        
    }
}
