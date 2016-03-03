package server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends abstractserver.AbstractServer{
    private static String USERNAME = "server";
    private static String PASSWORD = "Capstone2015Server";    
    //private static final String CONN_STRING = "jdbc:mysql://142.160.58.242:3306/projectdb";
    private static final String CONN_STRING = "jdbc:mysql://localhost:3306/projectdb";
    public Connection conn = null;
    
    public Server(int portNumber, int backlog, clientcommand.ClientCommand myClientCommand) {
        super(portNumber, backlog, myClientCommand);
    }
    
    @Override
    public void handleMessageFromClient(clientconnection.ClientConnection myClientConnection, byte theCommand){
        myClientCommand.execute(myClientConnection, theCommand);        
    }
    
    public void login() throws SQLException{
        this.conn = DriverManager.getConnection(CONN_STRING, USERNAME, PASSWORD);
    }
    
    public String executeSQL(String command){       
        String result = "";
        PreparedStatement pst = null;
        ResultSet set = null;
        try{
            login();                    
            try{
                if(command.split("\\s+")[0].compareToIgnoreCase("select") == 0){                
                    set = getResultSet(this.conn, command);                
                    result += "/t";
                    for(int i = 0; i < set.getMetaData().getColumnCount(); i++){
                        if(set.getMetaData().getColumnName(i+1).compareTo("Email") == 0)
                            result += set.getMetaData().getColumnName(i+1) + "\t\t\t||\t";
                        else
                            result += set.getMetaData().getColumnName(i+1) + "\t||\t";

                    }
                    while(set.next()){
                        result += "\n";                
                        for(int i = 0; i < set.getMetaData().getColumnCount(); i++){
                            result += set.getString(i+1) + "\t||\t";                        
                        }
                    }                
                }
                else{
                    pst = conn.prepareStatement(command);
                    pst.execute();
                    result = "Successfully executed command";
                }            
            }
            catch (SQLException e){
                result = "SQLException. Check syntax";
            }
        }
        catch(SQLException e){
            result =  "Failed to connect to database";
        }                 
        finally{
            // close resultsets, statements and connections
            try { if (set != null) set.close(); } catch (Exception e) {};
            try { if (pst != null) pst.close(); } catch (Exception e) {};
            try { if (conn != null) conn.close(); } catch (Exception e) {};
        }
        return result;
    }
        
    private ResultSet getResultSet(Connection conn, String sql){
        try{
            Statement stm = conn.createStatement(ResultSet.CONCUR_READ_ONLY, ResultSet.TYPE_FORWARD_ONLY);
            return stm.executeQuery(sql);
        }
        catch (SQLException e){
            return null;
        }
    }
    
}
