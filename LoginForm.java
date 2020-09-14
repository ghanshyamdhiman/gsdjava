/*
 * LoginForm.java
 *
 * Created on February 15, 2006, 2:11 PM
 */

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

/**
 *
 * @author  ghanshyam
 * @version
 */
public class LoginForm extends MIDlet implements CommandListener {
    
    private Command submitCommand = new Command("Submit", Command.OK, 1);
    private Command clearCommand = new Command("Clear", Command.CANCEL, 2);
    private Command sendCommand = new Command("Send", Command.OK, 3);
    private Command backCommand = new Command("Back", Command.BACK, 5);
    public List servicesList = new List("Services", List.IMPLICIT);
            
           
    private GSDLoginForm loginForm = null;
    public GSDEmailForm emailForm = null;
    
    public Display display = Display.getDisplay(this);
    
    public void startApp() {
        loginForm = new GSDLoginForm("User Details", this);
        loginForm.addCommand(submitCommand);
        loginForm.addCommand(clearCommand);
        loginForm.setCommandListener(this);
        display.setCurrent(loginForm);
        servicesList.setCommandListener(this);
        emailForm = new GSDEmailForm("Send Mail", this);
        emailForm.addCommand(sendCommand);
        emailForm.addCommand(backCommand);
        emailForm.setCommandListener(this);
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
    }
    
    public void commandAction(Command c, Displayable d) {
        if(c == submitCommand) {
            loginForm.processLogin();
        } else if (c == clearCommand) {
            loginForm.clearContents();
        } else if (c == List.SELECT_COMMAND) {
            List currentList = (List)display.getCurrent();
            int currentListIndex = currentList.getSelectedIndex();
            
        } else if (c == sendCommand) {
            emailForm.sendMail();
        } else if (c == backCommand) {
            emailForm.clearContents();
            display.setCurrent(loginForm);
        }
    }
}

class GSDLoginForm extends Form implements Runnable {
    
    private TextField userName = new TextField("User Name :", "", 8, TextField.ANY);
    private TextField userPass = new TextField("Password :", "", 8, TextField.PASSWORD);
    private StringItem status = new StringItem("Message", "");
    
    private HttpConnection serverConnection = null;
    
    private DataOutputStream dos = null;
    private DataInputStream dis = null;
    
    private LoginForm mainMidlet = null;
    
    GSDLoginForm(String title, MIDlet mainMidlet) {
        super(title);
        this.mainMidlet = (LoginForm)mainMidlet;
        append(userName);
        append(userPass);
        append(status);
    }
    public String getUserName() {
        return userName.getString();
    }
    public String getUserPass() {
        return userPass.getString();
    }
    public void processLogin() {
        new Thread(this).start();
    }
    public void clearContents() {
        userName.setChars(null, 0, 0);
        userPass.setChars(null, 0, 0);
        status.setText(null);
    }
    public String sendToServer(String userName, String userPass) {
       String serverResponse = null;
        try {
            serverConnection = (HttpConnection)Connector.open("http://192.168.100.227:8080/GSDUpdateDB");
            serverConnection.setRequestMethod(HttpConnection.POST);
            dos = serverConnection.openDataOutputStream();
            dos.write(userName.getBytes());
            dos.write(":".getBytes());
            dos.write(userPass.getBytes());
            dos.flush();
            
            if(serverConnection.getResponseCode() != HttpConnection.HTTP_OK) {
                append("Http Error");
            }
            dos.close();
         } catch (IOException ioe) {
             append("IO error");
         }
        try {
            dis = serverConnection.openDataInputStream();
            while(dis.available() < 0 );
            byte data[] = new byte[123];
            dis.read(data);
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i < data.length; i++) {
                if(data[i] != 0) {
                    buffer.append((char)data[i]);
                }
            }
            serverResponse = buffer.toString();
            dis.close();
            serverConnection.close();
        } catch (Exception e) {
            append("Data Reading error" + e);
        }
        if(serverResponse == null) {
            serverResponse = "NO response";
        } 
        return serverResponse;
    }
    public void run() {
        String serverResponse = null;
        serverResponse = sendToServer(getUserName(), getUserPass());
        if(serverResponse.equals("Welcome")) {
           mainMidlet.display.setCurrent(mainMidlet.emailForm);
       }
    }
}
class GSDEmailForm extends Form implements Runnable {
    private TextField fmAdd = new TextField("From :", "", 35, TextField.EMAILADDR);
    private TextField toAdd = new TextField("To :", "", 35, TextField.EMAILADDR);
    private TextField ccAdd = new TextField("Cc :", "", 35, TextField.EMAILADDR);
    private TextField subject = new TextField("Subject :", "", 15, TextField.ANY);
    private TextField textMsg = new TextField("Message :", "", 125, TextField.ANY);
    
    private HttpConnection serverConnection = null;
    private GSDMailAttributes gsdmail = null;
    
    private DataOutputStream dos = null;
    private DataInputStream dis = null;
    
    private LoginForm mainMidlet = null;
    
    GSDEmailForm(String title, MIDlet mainMidlet) {
        super(title);
        this.mainMidlet = (LoginForm)mainMidlet;
        append(fmAdd);
        append(toAdd);
        append(ccAdd);
        append(subject);
        append(textMsg);
    }
    public void clearContents() {
        fmAdd.setChars(null, 0, 0);
        toAdd.setChars(null, 0, 0);
        ccAdd.setChars(null, 0, 0);
        subject.setChars(null, 0, 0);
        textMsg.setChars(null, 0, 0);
    }
    public void sendMail() {
        new Thread(this).start();
    }
    public String ConnectToServer() {
        gsdmail = new GSDMailAttributes();
        gsdmail.fromAddressee = fmAdd.getString().trim();
        gsdmail.toAddressee = toAdd.getString().trim();
        gsdmail.ccAddressee = ccAdd.getString().trim();
        gsdmail.emailSubject = subject.getString().trim();
        gsdmail.emailContent = textMsg.getString().trim();
        
        String serverResponse = null;
        
        try {
            serverConnection = (HttpConnection)Connector.open("http://192.168.100.227:8080/TestOne/GSDSendMail");
            serverConnection.setRequestMethod(HttpConnection.POST);
            dos = serverConnection.openDataOutputStream();
            dos.write(gsdmail.fromAddressee.getBytes());
            dos.write(":".getBytes());
            dos.write(gsdmail.toAddressee.getBytes());
            dos.write(":".getBytes());
            dos.write(gsdmail.ccAddressee.getBytes());
            dos.write(":".getBytes());
            dos.write(gsdmail.emailSubject.getBytes());
            dos.write(":".getBytes());
            dos.write(gsdmail.emailContent.getBytes());
            
            dos.flush();
            
            if(serverConnection.getResponseCode() != HttpConnection.HTTP_OK) {
                append("Http Error");
            }
            dos.close();
         } catch (IOException ioe) {
             append("IO error");
         }
        try {
            dis = serverConnection.openDataInputStream();
            while(dis.available() < 0 );
            byte data[] = new byte[512];
            dis.read(data);
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i < data.length; i++) {
                if(data[i] != 0) {
                    buffer.append((char)data[i]);
                }
            }
            serverResponse = buffer.toString();
            dis.close();
            serverConnection.close();
        } catch (Exception e) {
            append("Data Reading error" + e);
        }
        if(serverResponse == null) {
            serverResponse = "NO response";
        } 
        return serverResponse;
    }
    public void run() {
        String serverResponse = null;
        serverResponse = ConnectToServer();
        append(new StringItem("From Server", serverResponse, StringItem.BUTTON));
    }
}
class GSDMailAttributes
{
    public String fromAddressee;
    public String toAddressee;
    public String ccAddressee;
    public String emailSubject;
    public String emailContent;
}
