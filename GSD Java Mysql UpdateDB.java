/*
 * GSDUpdateDB.java
 *
 * Created on February 15, 2006, 3:50 PM
 */

import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.util.*;

/**
 *
 * @author ghanshyam
 * @version
 */
public class GSDUpdateDB extends HttpServlet {
    Connection con = null;
    PreparedStatement pstat = null;
          
        
    public void createTable(String tblName) {
        StringBuffer statement = new StringBuffer("CREATE TABLE ");
        statement.append(tblName);
        statement.append("(VARCHAR username, VARCHAR userpass)");
        String sqlStatement = statement.toString();
        
        try {
            Statement stat = con.createStatement();
            stat.execute(sqlStatement);
            stat.close();
            con.close();
        } catch (Exception e) {
            System.out.println("DB exception " + e);
        }
    }
    public void insertRecord(String userName, String userPass) {
        StringBuffer statement = new StringBuffer("INSERT INTO TABLE usertbl VALUES(?,?)");
        String sqlStatement = statement.toString();
        
        try {
            pstat = con.prepareStatement(sqlStatement);
            pstat.setString(1, userName);
            pstat.setString(2, userPass);
            pstat.executeUpdate();
            pstat.close();
            con.close();
        } catch (Exception e) {
            System.out.println("Error inserting record" + e);
        }
        
    }
    public void openDB(String dbName, String dbUser, String dbPassword) {
        StringBuffer dbURL = new StringBuffer("jdbc:mysql://localhost:3036/");
        dbURL.append(dbName);
        dbURL.append("?user=");
        dbURL.append(dbUser);
        dbURL.append("password=");
        dbURL.append(dbPassword);
        String connectionURL = dbURL.toString();
        
        String driverClass = "com.mysql.jdbc.Driver";
              
        try {
            Class.forName(driverClass).newInstance();
            con = DriverManager.getConnection(connectionURL, "root", "ddlj");
        } catch (Exception e) {
            System.out.println("Error Connecting to DataBase" + e);
        }
    }  
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        //openDB("gsddb", "root", "ddlj");
        //createTable("usertbl");
      
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
        
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        
        ServletInputStream servIn = null;
        ServletOutputStream servOut = null;
        
        String inputData = null;
        StringTokenizer tokenData = null;
        String userDetails[] = null;
        String userName = null;
        String userPass = null;
        String message = null;
        
        try {
            servIn = request.getInputStream();
            while(servIn.available() < 0 );
       
            byte data[] = new byte[123];
            StringBuffer buffer = new StringBuffer();
            servIn.read(data);
            for(int i = 0; i < data.length; i++) {
                if(data[i] != 0) {
                    buffer.append((char)data[i]);
                }
            }
            inputData = buffer.toString();
            tokenData = new StringTokenizer(inputData, ":");
            int tokens = tokenData.countTokens();
            userDetails = new String[tokens];
            int i = 0;
            while(tokenData.hasMoreTokens()) {
                userDetails[i] = tokenData.nextToken();
                i++;
            }
            userName = userDetails[0];
            userPass = userDetails[1];
            System.out.println("User Name : " + userName);
            System.out.println("User Pass : " + userPass);
            servIn.close();
        } catch (Exception e) {
            System.out.println("Data error" + e);
        }
        message = authenticateUser(userName, userPass);
        System.out.println("Dear user : " + message );
        
        try {
            servOut = response.getOutputStream();
            servOut.write(message.getBytes());
            servOut.flush();
            servOut.close();
        } catch (Exception e) {
            System.out.println("OutStream error" + e);
        }
    }
    
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
    public String authenticateUser(String userName, String userPass) {
              
        String connectionURL = "jdbc:mysql://localhost:3306/gsddb?user=root;password=ddlj";
	Connection connection = null;
	PreparedStatement statement = null;
	ResultSet rs = null;
	
        String passFound = null;
        String message = null;
        boolean found = false;

	try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(connectionURL, "root", "ddlj");
            statement = connection.prepareStatement("SELECT pass FROM gsdtbl WHERE name = ?");
            statement.setString(1, userName);
            rs = statement.executeQuery();
            while (rs.next()) {
                passFound = rs.getString(1);
                if(passFound.equals(userPass)) {
                    found = true;
                    break;
                }
            }
            if(found) {
                message = "Welcome";
            } else {
                message = "Wrong Password";
            }
	} catch(Exception sqlExp){
            System.out.println("SQL Exception " + sqlExp);
	}
	finally {
            try {
		statement.close();
            } catch (Exception ioe) {
		System.out.println ( "Statement Close Exception" + ioe );
            }
            try {
		rs.close();
            } catch (Exception ioe) {
		System.out.println ( "Result set Close Exception" + ioe );
            }
	}
        return message;
    }
}
