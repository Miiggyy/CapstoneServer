package abstractserver;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.Server;

public abstract class AbstractServer implements Runnable{

    InputStream input;
    OutputStream output;
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    protected ArrayList<clientconnection.ClientConnection> clients = new ArrayList<>();
    protected clientcommand.ClientCommand myClientCommand;
    byte msg = ' ', clientCommand = ' ';
    int portNumber = 5555, backlog = 500;
    boolean doListen = false;

    public AbstractServer(int portNumber, int backlog, clientcommand.ClientCommand myClientCommand) {
        this.portNumber = portNumber;
        this.backlog = backlog;
        this.myClientCommand = myClientCommand;
    }

    public void startServer() {
        if (serverSocket != null) {
            stopServer();
        } else {
            try {                
                serverSocket = new ServerSocket(portNumber, backlog);                
            } catch (IOException e) {
                System.err.println("Cannot create ServerSocket, exiting program.");
                System.exit(1);
            } finally {
            }
        }

    }

    public void stopServer() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Cannot close ServerSocket, exiting program.");
                System.exit(1);
            } finally {
            }

        }
    }

    public void disconnectClient() { //Needs work: should disconnect a particular client.
        try {
            clientSocket.close();
            input = null;
            output = null;
        } catch (IOException e) {
            System.err.println("cannot close streams, or cannot close client socket; exiting program.");
            System.exit(1);
        } finally {
        }
    }
    
    public void disconnectAllClients(){
        for(int i = 0; i < clients.size(); i++){
            clients.get(i).disconnectClient();
        }
    }

    public void removeDisconnected(){
        for(int i = 0; i < clients.size(); i++){
            if(clients.get(i) == null || !clients.get(i).isRunning())
                clients.remove(i);
        }
    }

    protected synchronized void setDoListen(boolean doListen){
        this.doListen = doListen;
    }
    
    public void listen() {
        try {
            setDoListen(true);
            serverSocket.setSoTimeout(500);
            Thread myListenerThread = new Thread(this);
            myListenerThread.start();     
        } catch (SocketException ex) {
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void stopListening() {
        setDoListen(false);
    }

    @Override
    public void run(){
        while (true) {
            if (doListen == true) {
                try {
                    clientSocket = serverSocket.accept();
                    clientconnection.ClientConnection myCC = new clientconnection.ClientConnection(clientSocket, myClientCommand, (Server) this);
                    clients.add(myCC);
                    Thread myCCthread = new Thread(myCC);
                    myCCthread.start();
                    System.out.println("Client connected:\n\tRemote Socket Address = "+clientSocket.getRemoteSocketAddress() + "\n\tLocal Socket Address = " +clientSocket.getLocalSocketAddress());
                } catch (IOException e) {
                    //check doListen.
                } catch (SQLException ex) {
                    Logger.getLogger(AbstractServer.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                }
            }
        }
    }

    public abstract void handleMessageFromClient(clientconnection.ClientConnection theCC, byte theCommand);
        
    public byte getMessageFromClient() {
        try {
            msg = (byte) input.read();
            System.out.println(msg);
        } catch (IOException e) {
            System.err.println("cannot read from socket; exiting program.");
            System.exit(1);
        } finally {
            return msg;
        }
    }

    public void sendMessageToClient(byte msg) {
        try {
            output.write(msg);
            output.flush();
        } catch (IOException e) {
            System.err.println("cannot send to socket; exiting program.");
            System.exit(1);
        } finally {
        }
    }
        
    public ArrayList<String> getClientIPAddress() {
        ArrayList<String> list = new ArrayList<>();
        for(int i = 0; i < clients.size(); i++){
            list.add(clients.get(i).getRemoteSocketAddress());
        }
        if(list.size() == 0)
            list.add("No clients connected.");
        return list;
    }
    
    public void setPort(int portNumber) {
        this.portNumber = portNumber;
    }

    public int getPort() {
        return this.portNumber;
    }
        
    public void sendToAllClients(byte msg){
        for(int i = 0; i < clients.size();i++){
            if(clients.get(i) != null || clients.get(i).isRunning())
                clients.get(i).sendMessageToClient(msg);
        }
    }
    
    public void serverStarted(){
        
    }
    
    public void serverStopped(){
        
    }
    
    public void listeningException(){
        
    }
    
    public void clientConnected(){
        
    }
    
    public void clientDisconnected(){
        
    }
    
    public void clientException(){
        
    }
}