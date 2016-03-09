# server 
This is the server software for the University of Manitoba ECE Group 16 Capstone Project. This software was developed using Netbeans IDE 8.0.2. This server listens to port 5555 for client connections and assumes that there is a MySQL server on the same machine running on port 3306. 

If the MySQL server is on a different machine, open the project on netbeans and uncomment line 29 in ClientConnection.java then change the value of CONN_STRING to the ip address of the MySQL server. Do the same for line 14 in Server.java. Then recompile using Netbeans.

To run the server, either open a command terminal, navigate to the dist folder and run

    java -jar Server.jar
or open and run the project using Netbeans IDE.

Clients are required to provide login credentials upon successful connection to the server. The clients must send the login credentials in the following format:

    username password

followed by -128 in byte form.

# Client to Server command formats
get time: 	

    1

check entry:	

    2 [cid]

insert entry:	

    4 [email] [balance]
	4 [email]
		
update balance:	

    5 [cid] [balance]
delete entry:	

    6 [email]

select entry:	

    7
    7 [cid]
  	7 [email]
		
		
update dev:	

    8 [cid] [dev]

get balance:	

    9 [cid]

get timestamp:	

    10 [cid]


# XML Format

    <Data>
    	<Customer>
    		<CID>1</CID>
    		<Dev>0</Dev>
    		<Balance>8</Balance>
    		<Email>test</Email>
    		<LastUpdated>1970-01-01 10:10:10.0</LastUpdated>
    	</Customer>
    	...
    	<Customer>
        ...
    	</Customer>
    </Data>

# MySQL Database setup
Copy and paste the following code into the mysql command line client to create the database, tables, triggers and user accounts used in the project.

    CREATE DATABASE projectdb;
    
    CREATE TABLE projectdb.customers(CID int NOT NULL,Dev boolean DEFAULT false,Balance float DEFAULT 0,Email varchar(50) NOT NULL UNIQUE,LastUpdated timestamp,PRIMARY KEY (CID));
    	
    CREATE TABLE projectdb.customers_history LIKE projectdb.customers;
    
    ALTER TABLE projectdb.customers_history MODIFY COLUMN CID int NOT NULL, DROP INDEX Email,DROP PRIMARY KEY, CHANGE COLUMN LastUpdated Updated timestamp, ENGINE = MyISAM, ADD Action VARCHAR(8) DEFAULT 'INSERT' FIRST, ADD Rev INT(6) NOT NULL AUTO_INCREMENT AFTER Action, ADD BalanceChanges FLOAT AFTER Rev,ADD PRIMARY KEY (CID,Rev);
    
    DROP TRIGGER IF EXISTS projectdb.customers__ai;
    DROP TRIGGER IF EXISTS projectdb.customers__au;
    DROP TRIGGER IF EXISTS projectdb.customers__bd;
    
    CREATE TRIGGER projectdb.customers__ai AFTER INSERT ON projectdb.customers FOR EACH ROW INSERT INTO projectdb.customers_history SELECT 'INSERT', NULL,  d.Balance, d.CID, d.DEV, d.BALANCE, d.EMAIL, NOW() FROM projectdb.customers AS d WHERE d.CID = NEW.CID;
    CREATE TRIGGER projectdb.customers__au AFTER UPDATE ON projectdb.customers FOR EACH ROW INSERT INTO projectdb.customers_history SELECT 'UPDATE', NULL, NEW.Balance - OLD.Balance, d.CID, d.DEV, d.BALANCE, d.EMAIL, NOW() FROM projectdb.customers AS d WHERE d.CID = NEW.CID;
    CREATE TRIGGER projectdb.customers__bd BEFORE DELETE ON projectdb.customers FOR EACH ROW INSERT INTO projectdb.customers_history SELECT 'DELETE', NULL, 0, d.CID, d.DEV, d.BALANCE, d.EMAIL, NOW() FROM projectdb.customers AS d WHERE d.CID = OLD.CID;
    
    CREATE USER 'scanner'@'%' IDENTIFIED BY 'Capstone2015Scanner';
    GRANT SELECT,UPDATE ON projectdb.* TO 'scanner'@'%';
    CREATE USER 'phone'@'%' IDENTIFIED BY 'Capstone2015Phone';
    GRANT SELECT,UPDATE ON projectdb.* TO 'phone'@'%';
    CREATE USER 'kiosk'@'%' IDENTIFIED BY 'Capstone2015Kiosk';
    GRANT SELECT,UPDATE,INSERT,DELETE ON projectdb.* TO 'kiosk'@'%';
    CREATE USER 'web'@'%' IDENTIFIED BY 'Capstone2015Web';
    GRANT SELECT,UPDATE ON projectdb.* TO 'web'@'%';
    CREATE USER 'server'@'%' IDENTIFIED BY 'Capstone2015Server';
    GRANT SELECT,UPDATE,INSERT,DELETE ON projectdb.* TO 'server'@'%';
