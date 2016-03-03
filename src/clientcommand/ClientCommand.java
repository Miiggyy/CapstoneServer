package clientcommand;

import clientconnection.ClientConnection;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientCommand {

    userinterface.StandardIO myUI;
    private final AtomicReference<Long> currentTime = new AtomicReference<>(System.nanoTime());
    int boardCount = 0;

    public ClientCommand(userinterface.StandardIO myUI) {
        this.myUI = myUI;
    }
    
    /**
    * Process the message (byte) received from a client
    * @param myClientConnection ClientConnection where the message came from
    * @param theCommand Byte received from client    
    */
    public void execute(clientconnection.ClientConnection myClientConnection, byte theCommand){
        String remote, local;
        int check = 0;

        if (theCommand == (byte) -1) {  // disconnect
            remote = "\tRemote Socket Address = " + myClientConnection.getRemoteSocketAddress();
            local = "\tLocal Socket Address = " + myClientConnection.getLocalSocketAddress();
            myClientConnection.disconnectClient();
            myUI.display("Client has been disconnected: ");
            myUI.display(remote);
            myUI.display(local);
        } else if (theCommand != (byte) -128){  // append message
            myClientConnection.appendMsg(byteToString(theCommand));
        } else{// process message
            myUI.display("Message from Client (" + myClientConnection.getRemoteSocketAddress() + "):\n" +myClientConnection.getMsg());
            if(!myClientConnection.isLogin()){ // attempt login
                String[] login = myClientConnection.getMsg().split(" ");                                
                if(login.length == 2){
                    myClientConnection.setID(login[0]);
                    myClientConnection.setPW(login[1]);
                    if(myClientConnection.login() != 0)
                        myClientConnection.sendMessageToClient((byte)-127);                                       
                    else
                        myClientConnection.sendMessageToClient((byte)126);                                                                                      
                    myClientConnection.closeDBConnection();
                }                    
            }
            else{ // process message
                String[] SQL = myClientConnection.getMsg().toLowerCase().trim().split("\\s+");
                ResultSet select = null;
                String msg;                
                int id;                
                try{
                    myClientConnection.login();
                    check = 0;
                    switch (Integer.valueOf(SQL[0])){
                        case 1:                           
                            String time = getTime("MMM dd yyyy HH:mm:ss");
                            sendMsg(myClientConnection, time);
                            myClientConnection.sendMessageToClient((byte)-128);
                            myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") has requested time: "+time);
                            break;
                        case 2:
                            if(inTable(myClientConnection.conn, Integer.parseInt(SQL[1])))
                                myClientConnection.sendMessageToClient((byte)'1');
                            else
                                myClientConnection.sendMessageToClient((byte)'0');
                            myClientConnection.sendMessageToClient((byte)-128);
                            break;
                        case 4:   // insert
                            if (!inTable(myClientConnection.conn, SQL[1])){
                                id = nextId();
                                if(SQL.length == 3){// email address and initial balance are provided
                                    check = insertCustomer(myClientConnection.conn, id, SQL[1] , Float.parseFloat(SQL[2]));
                                    while(check == 1062){// if a duplicate is found, generate a new id. repeat until successful
                                        id = nextId();
                                        check = insertCustomer(myClientConnection.conn, id, SQL[1] , Float.parseFloat(SQL[2]));
                                    }
                                }
                                else if(SQL.length == 2){// no initial balance, just email
                                    check = insertCustomer(myClientConnection.conn , id, SQL[1]);
                                    while(check == 1062){// if a duplicate is found, generate a new id. repeat until successful
                                        id = nextId();
                                        check = insertCustomer(myClientConnection.conn , id, SQL[1]);
                                    }
                                }
                                if(check == 0){// if there was no error
                                    myUI.display("Successfully inserted into database");                                    
                                    // send back the cid
                                    msg = String.format("%010d", id);
                                    myUI.display(msg);
                                    myClientConnection.sendMessageToClient((byte)130);
                                    sendMsg(myClientConnection, msg);
                                    myClientConnection.sendMessageToClient((byte)-128);
                                }
                                else
                                    myUI.display("Failed to insert into database. Error code: " + Integer.toString(check));
                            }
                            else {
                                myUI.display("Email address already in use.");
                            }
                            myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") added a database entry.");
                            break;

                        case 5:   // update balance
                            if(updateBalance(myClientConnection.conn, Integer.parseInt(SQL[1]), Float.parseFloat(SQL[2])) == 0)
                                myUI.display("Successfully updated database entry for cid: " + SQL[1]);
                            else
                                myUI.display("Failed to update database");
                            myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") updated a database entry's balance.");
                            break;

                        case 6:   // delete                            
                            try{
                                if(deleteCustomer(myClientConnection.conn, Integer.parseInt(SQL[1])) == 0)
                                    myUI.display("Successfully deleted database entry with CID: " + SQL[1]);
                                else
                                    myUI.display("Failed to delete database entry.");
                            }
                            catch (NumberFormatException e){
                                if(deleteCustomer(myClientConnection.conn, SQL[1]) == 0)
                                    myUI.display("Successfully deleted database entry with email: " + SQL[1]);
                                else
                                    myUI.display("Failed to delete database entry.");
                            }
                            break;

                        case 7:   // select
                            try{
                                if(SQL.length == 2){ // client provided cid or email
                                    select = getCustomer(myClientConnection.conn, Integer.parseInt(SQL[1]));
                                    myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") requested a database entry.");
                                }
                                else{ // no arguments so send everything
                                    select = getAllCustomers(myClientConnection.conn);
                                    myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") requested all database entries.");
                                }
                            }
                            catch (NumberFormatException e){ // client provided cid
                                select = getCustomer(myClientConnection.conn, SQL[1]);
                                myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") requested a database entry.");
                            }
                            if(select != null){
                                myClientConnection.sendMessageToClient((byte) 127);
                                msg = "<Data>";
                                while(select.next()){
                                    msg += "\n\t<Customer>";
                                    for(int i = 0; i < select.getMetaData().getColumnCount(); i++){
                                        msg += "\n\t\t<" + select.getMetaData().getColumnName(i+1) + ">" +  select.getString(i+1) + "</" + select.getMetaData().getColumnName(i+1) + ">";
                                    }
                                    msg += "\n\t</Customer>";
                                    sendMsg(myClientConnection, msg);
                                    msg = "";
                                }
                                msg += "\n</Data>";
                                sendMsg(myClientConnection, msg);
                                myClientConnection.sendMessageToClient((byte) -128);                                
                            }
                            break;

                        case 8: // update dev
                            if(SQL.length == 3){
                                if (updateDev(myClientConnection.conn, Integer.parseInt(SQL[1]), Integer.parseInt(SQL[2])) == 0)
                                    myUI.display("Successfully updated database entry for cid: " + SQL[1]);
                                else
                                    myUI.display("Failed to update database");
                            }
                            myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") updated a database entry's dev value.");
                            break;

                        case 9: // get balance
                            if(SQL.length == 2){
                                select = getBalance(myClientConnection.conn, Integer.parseInt(SQL[1]));
                                myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") requested balance for cid: " + Integer.parseInt(SQL[1]) + ".");
                                if(select.next())
                                    sendMsg(myClientConnection, select.getString(1));
                                else
                                    sendMsg(myClientConnection, "Could not find entry.");
                                myClientConnection.sendMessageToClient((byte) -128);
                            }
                            break;

                        case 10: // get timestamp
                            if(SQL.length == 2){
                                select = getTimestamp(myClientConnection.conn, Integer.parseInt(SQL[1]));
                                myUI.display("Client (" + myClientConnection.getRemoteSocketAddress() + ") requested expiry time for cid: " + Integer.parseInt(SQL[1]) + ".");
                                if(select.next())
                                    sendMsg(myClientConnection, select.getString(1));
                                else
                                    sendMsg(myClientConnection, "Could not find entry.");
                                myClientConnection.sendMessageToClient((byte) -128);
                            }
                            break;
                        default:
                            myUI.display("Invalid command from client. \n\tRemote Socket Address = " + myClientConnection.getRemoteSocketAddress());
                            break;                            
                    }

                } catch (SQLException e) {
                    myUI.display("SQLException" + Integer.toString(e.getErrorCode()));
                } catch (NumberFormatException e){
                    myUI.display("Failed to convert values. Check client message.");
                } catch (NullPointerException e){
                    myUI.display("Entry does not exist.");
                }finally{
                    // close resultset and sqlconnection
                    try{if(select != null) select.close();}catch (Exception e){};
                    myClientConnection.closeDBConnection();                    
                }
            }
            myClientConnection.clearMsg();
        }
    }

    private String byteToString(byte theByte){
        byte[] theByteArray = new byte[1];
        String theString = null;
        theByteArray[0] = theByte;
        try {
            theString = new String(theByteArray, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Cannot convert from UTF-8 to String; exiting program.");
            System.exit(1);
        }
        finally{
            return theString;
        }
    }

    /**
    * Get the system time in the specified format
    * @param format format of time to be used    
    * @return system time
    */
    private String getTime(String format){
        Calendar cal = Calendar.getInstance();        
        SimpleDateFormat sdf = new SimpleDateFormat(format);               
        return sdf.format(cal.getTime());                                
    }
    
    /**
     * Insert a new entry into customers database
     * @param conn  SQL Connection
     * @param cid       customer ID
     * @param balance   initial balance, default balance = 0
     * @param email   customer email
     * @return 0 if successful, returns sql error code if failed.
     */
    private int insertCustomer(Connection conn, int cid, String email, float balance ){
        int err;
        PreparedStatement pst = null;
        try{
            pst = conn.prepareStatement("insert into customers (CID, BALANCE, EMAIL, LASTUPDATED) values (?, ?, ?, '1970-01-01 10:10:10.0')");
            pst.setInt(1, cid);
            pst.setFloat(2, balance);
            pst.setString(3, email);
            pst.execute();
            err = 0;
        }
        catch (SQLException e){
            myUI.display(e.getLocalizedMessage());
            err = e.getErrorCode();
        }
        finally{
            try{if(pst != null) pst.close();}catch(Exception e){};
        }
        return err;
    }

    /**
     * Insert a new entry into customers database
     * @param conn  SQL Connection
     * @param cid     customer(card) ID
     * @param email   customer email
     * @return 0 if successful, returns sql error code if failed.
     */
    private int insertCustomer(Connection conn, int cid, String email){
        int err;
        PreparedStatement pst = null;
        try{
            pst = conn.prepareStatement("insert into customers (CID, EMAIL, LASTUPDATED) values (?, ?, '1970-01-01 10:10:10.0')");
            pst.setInt(1, cid);            
            pst.setString(2, email);
            pst.execute();
            err = 0;
        }
        catch (SQLException e){            
            err = e.getErrorCode();
        }
        finally{
            try{if(pst != null) pst.close();}catch(Exception e){};
        }
        return err;
    }

    /**
     * Delete an entry from the database
     * @param conn  SQL Connection
     * @param cid   customer(card) ID
     * @return 0 if successful, returns sql error code if failed.
     */
    private int deleteCustomer(Connection conn, int cid){
        int err;
        PreparedStatement pst = null;
        if(!inTable(conn, cid))
            err = -1;
        else{
            try{
                pst = conn.prepareStatement("delete from customers where CID=?");
                pst.setInt(1, cid);
                pst.execute();
                err = 0;
            }
            catch (SQLException e){
                err = e.getErrorCode();
            }
            finally{
                try{if(pst != null) pst.close();}catch(Exception e){};
            }
        }
        return err;
    }

    /**
     * Delete an entry from the database
     * @param conn  SQL Connection
     * @param email customer email address
     * @return 0 if successful, returns sql error code if failed.
     */
    private int deleteCustomer(Connection conn, String email){
        int err;
        PreparedStatement pst = null;
        if(!inTable(conn, email))
            err = -1;
        else{
            try{
                pst = conn.prepareStatement("delete from customers where Email=?");
                pst.setString(1, email);
                pst.execute();
                err = 0;
            }
            catch (SQLException e){
                err = e.getErrorCode();
            }
            finally{
                try{if(pst != null) pst.close();}catch(Exception e){};
            }
        }
        return err;
    }
    
    /**
     * Retrieves all entries from customers table
     * @param conn  SQL Connection     
     * @return ResultSet containing requested entries, null if no entries exist
     */
    private ResultSet getAllCustomers(Connection conn) throws SQLException{
        return getResultSet(conn, "select CID,Dev,Balance,Email,date_format(LastUpdated, '%b %e %Y %H:%i:%s') as LastUpdated from customers");
    }

    /**
     * Retrieves an entry from customers table
     * @param conn  SQL Connection     
     * @param cid   customer id of required entry
     * @return ResultSet containing requested entry, null if entry not found
     */
    private ResultSet getCustomer(Connection conn, int cid) throws SQLException{
        String command = "select CID,Dev,Balance,Email,date_format(LastUpdated, '%b %e %Y %H:%i:%s') as LastUpdated from customers where CID = " + cid;
        return getResultSet(conn, command);
    }

    /**
    * Retrieves an entry from customers table
    * @param conn  SQL Connection     
    * @param email email address of required entry
    * @return ResultSet containing requested entry, null if entry not found
    */
    private ResultSet getCustomer(Connection conn, String email) throws SQLException{
        String command = "select CID,Dev,Balance,Email,date_format(LastUpdated, '%b %e %Y %H:%i:%s') as LastUpdated from customers where Email = '" + email + "'";
        return getResultSet(conn, command);
    }

    /**
    * Retrieves the balance from an entry from customers table
    * @param conn  SQL Connection     
    * @param cid   customer id of required entry
    * @return ResultSet containing requested entry, null if entry not found
    */
    private ResultSet getBalance(Connection conn, int cid) throws SQLException{
        String command = "select Balance from customers where cid = " + cid;
        return getResultSet(conn, command);
    }

    /**
    * Retrieves the lastupdate from an entry from customers table
    * @param conn  SQL Connection     
    * @param cid   customer id of required entry
    * @return ResultSet containing requested entry, null if entry not found
    */
    private ResultSet getTimestamp(Connection conn, int cid) throws SQLException{
        String command = "select LastUpdated from customers where cid = " + cid;
        //String command = "select date_format(date_add((select LastUpdated from customers where cid = " + cid + "), interval '1:15' hour_minute), '%b %e %Y %H:%i:%s')";
        return getResultSet(conn, command);
    }

    /**
    * Executes an SQL select command
    * @param conn  SQL Connection     
    * @param sql SQL command to execute
    * @return ResultSet containing requested data, null if not found
    */
    private ResultSet getResultSet(Connection conn, String sql){
        Statement stm = null;
        try{
            stm = conn.createStatement(ResultSet.CONCUR_READ_ONLY, ResultSet.TYPE_FORWARD_ONLY);
            return stm.executeQuery(sql);
        }
        catch (SQLException e){
            try{if(stm != null) stm.close();}catch (Exception ex){};
            return null;
        }
    }

    /**
    * Send message to client
    * @param myClientConnection Connection with client to send message to
    * @param msg message to be sent    
    */
    private void sendMsg(ClientConnection myClientConnection, String msg){
        for(int i = 0; i < msg.length(); i++){
            myClientConnection.sendMessageToClient((byte)msg.charAt(i));
        }
    }

    /**
    * Update balance of an entry in customers table
    * @param conn SQL Connection
    * @param cid  customer ID of entry to be updated
    * @param balance amount to add or deduct from balance
    * @return 0 if successful, -1 if entry not in table otherwise returns sql error code if failed.
    */
    private int updateBalance(Connection conn, int cid, float balance){
        int err;
        PreparedStatement pst = null;
        if(!inTable(conn,cid))
            err = -1;
        else{
            try{                                
                if(Float.compare(balance, 0f) > 0){                    
                    pst = conn.prepareStatement("update customers set Balance=Balance+?, LastUpdated=LastUpdated where CID=?");
                    pst.setFloat(1, balance);
                    pst.setInt(2, cid);
                    pst.execute();                    
                }
                else if(Float.compare(balance, 0f) < 0){
                    pst = conn.prepareStatement("update customers set Balance=Balance+? where CID=?");
                    pst.setFloat(1, balance);
                    pst.setInt(2, cid);
                    pst.execute();
                }                                                    
                err = 0;
                
            }
            catch (SQLException e){
                err = e.getErrorCode();
            }
            finally{
                try{if(pst != null) pst.close();}catch (Exception e){};
            }            
        }
        return err;
    }

    private int updateCID(Connection conn, int cid, String email){
        try{
            PreparedStatement pst = conn.prepareStatement("update customers set CID=? where Email=?");
            pst.setInt(1, cid);
            pst.setString(2, email);
            pst.execute();
            return 0;
        }
        catch (SQLException e){
            return -1;
        }
    }
    
    /**
    * Update dev column of an entry in customers table
    * @param conn SQL Connection
    * @param cid  customer ID of entry to be updated
    * @param dev_bit new value of dev
    * @return 0 if successful, -1 if entry not in table otherwise returns sql error code if failed.
    */
    private int updateDev(Connection conn, int cid, int dev_bit){
        int err;
        PreparedStatement pst = null;
        if(!inTable(conn, cid))
            err = -1;
        else{
            boolean dev;
            if(dev_bit == 0)
                dev = false;
            else
                dev = true;
            try{
                pst = conn.prepareStatement("update customers set Dev=?, LastUpdated=LastUpdated where CID=?");
                pst.setBoolean(1, dev);
                pst.setInt(2, cid);
                pst.execute();
                err = 0;
            }
            catch (SQLException e){
                err = e.getErrorCode();
            }
            finally{
                try{if(pst != null) pst.close();}catch (Exception e){};
            }
        }
        return err;
    }
    
    /**
    * Check if an entry is in the customers table
    * @param conn SQL Connection    
    * @param email email address of entry to look for
    * @return true if found, false otherwise
    */
    private boolean inTable(Connection conn, String email){
        ResultSet set = null;
        boolean result = true;
        try{
            set = getCustomer(conn, email);
            if(!set.next())
                result = false;
            else
                result = true;
        }
        catch (SQLException e){
            result = true;
        }
        finally{
            try{if(set != null) set.close();}catch (Exception e){};
        }
        return result;
    }
    
    /**
    * Check if an entry is in the customers table
    * @param conn SQL Connection    
    * @param cid customer id of entry to look for
    * @return true if found, false otherwise
    */
    private boolean inTable(Connection conn, int cid){
        ResultSet set = null;
        boolean result = true;
        try{
            set = getCustomer(conn, cid);
            if(!set.next())
                result = false;
            else
                result = true;
        }
        catch (SQLException e){
            result = true;
        }
        finally{
            try{if(set != null) set.close();}catch (Exception e){};
        }
        return result;
    }
    
    /**
    * Generate a customer id
    * @return generated customer id
    */
    private int nextId() {
        return (int) (currentTime.accumulateAndGet(System.nanoTime(), (prev, next) -> next > prev ? next : prev + 1) % 2147483647);
    }
}
