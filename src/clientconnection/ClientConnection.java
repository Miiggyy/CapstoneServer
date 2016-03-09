package clientconnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ClientConnection implements Runnable {

    InputStream input;
    OutputStream output;
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    userinterface.StandardIO myUI;
    clientcommand.ClientCommand myClientCommand;
    server.Server myServer;
    private boolean stopThisThread = false, login = false;
    byte msg;
    String message = "";
    private String USERNAME = null;
    private String PASSWORD = null;
    private static final String CONN_STRING = "jdbc:mysql://localhost:3306/projectdb";
    public Connection conn = null;

    public ClientConnection(Socket clientSocket, clientcommand.ClientCommand myClientCommand, server.Server myServer) throws SQLException {
        this.clientSocket = clientSocket;
        this.myClientCommand = myClientCommand;
        this.myServer = myServer;
        try {
            input = clientSocket.getInputStream();
            output = clientSocket.getOutputStream();
        } catch (IOException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Cannot create IO streams; exiting program.");
            System.exit(1);
        }
    }

    @Override
    public void run() {
        byte msg;
        while (!stopThisThread) {
            msg = getMessageFromClient();
            myServer.handleMessageFromClient(this, msg);
        }
    }

    public byte getMessageFromClient() {
        try {
            msg = (byte) input.read();
            return msg;
        } catch (Exception e) {
            stopThisThread = true;
            return -1;
        }
    }

    public void sendMessageToClient(byte msg) {
        try {
            output.write(msg);
            output.flush();
        } catch (IOException e) {
            stopThisThread = true;
        } finally {
        }
    }

    public void clientQuit() {
        myUI.display("Client has quit: "+clientSocket.getRemoteSocketAddress());
        disconnectClient();
    }

    public void disconnectClient() {
        try {
            stopThisThread = true;
            clientSocket.close();
            clientSocket = null;
            input = null;
            output = null;
            myServer.removeDisconnected();
        } catch (IOException e) {
            System.err.println("cannot close client socket; exiting program.");
            System.exit(1);
        } finally {
        }
    }

    public void appendMsg(String msg){
        message += msg;
    }

    public String getMsg(){
        return message;
    }

    public void clearMsg(){
        message = "";
    }

    public String getRemoteSocketAddress(){
        String msg = "";
        if(clientSocket != null)
            msg = String.valueOf(clientSocket.getRemoteSocketAddress());
        return msg;
    }

    public String getLocalSocketAddress(){
        String msg = "";
        if(clientSocket != null)
            msg = String.valueOf(clientSocket.getLocalSocketAddress());
        return msg;
    }

    public boolean isLogin(){
        return login;
    }

    /**
    * Attempt to log into database using credentials set with setID and setPW
    * @return 0 on success, -1 on failure
    */
    public int login(){
        if (USERNAME.toLowerCase().compareTo("root") != 0){    
            try{
                this.conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
            }
            catch (SQLException e){
                System.err.println(e);
                login = false;
                return -1;
            }
            login = true;
            return 0;
        }
        else
            return -1;
    }
    
    public void closeDBConnection(){
        try{if(this.conn != null) this.conn.close();} catch (Exception e) {}; 
        this.conn = null;
    }
    
    public void setID(String id){
        this.USERNAME = id;
    }
    
    public void setPW(String pw){
        this.PASSWORD = pw;
    }
    
    public boolean isRunning(){
        return !stopThisThread;
    }    
}
