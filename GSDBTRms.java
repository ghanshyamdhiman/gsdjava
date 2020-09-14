// BTDeviceSearch.java

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.*;

public class GSDBTRms extends MIDlet implements CommandListener, Runnable
{
    private GSDBTRmsClient client = null;
    private KeyCapture captureKeys = null;
    
    private Display display = null ;
    private Form mainForm = new Form("GSD BT Keyboard");
    
    private List knownDevicesList = new List("Known Devices", List.IMPLICIT);
    private List knownServicesList = new List("", List.IMPLICIT);
    
    private Command deviceSearchCommand = new Command("Search Devices", Command.SCREEN, 1);
    private Command deviceListCommand = new Command("List Device", Command.SCREEN, 2);
  
    private Command serviceSearchCommand = new Command("Search Services", Command.SCREEN, 1);
    private Command serviceListCommand = new Command("List Services", Command.SCREEN, 2);
    
    private Command backCommand = new Command("Back", Command.BACK, 1);
    private Command exitCommand = new Command("Exit", Command.EXIT, 2);
    private Command connectCommand = new Command("Connect", Command.ITEM, 3);
     
    final private static int MAIN_MENU = 00;
    final private static int DEVICE_MENU = 11;
    final private static int SERVICE_MENU = 22;
    final private static int CONNECT_MENU = 33;
    private int currentMenu = 0;
    
    private Thread serviceThread = null;
      
    public GSDBTRms() {
        display = Display.getDisplay(this);
        client = new GSDBTRmsClient(this);
    }
    public void alertMessage(String message) {
        Alert al = new Alert("Message", message, null, null);
	al.setTimeout(Alert.FOREVER);
	display.setCurrent(al);
    }
    public void showTheCanvas() {
	Ticker t = new Ticker("Press any key");
	captureKeys = new KeyCapture();
        captureKeys.addCommand(backCommand);
        captureKeys.addCommand(exitCommand);
        captureKeys.setCommandListener(this);
	captureKeys.setTicker(t);
	captureKeys.setClient(client);
	display.setCurrent(captureKeys);
   }
    public void startApp() {
        mainForm.addCommand(deviceSearchCommand);
        mainForm.addCommand(deviceListCommand);
        mainForm.addCommand(exitCommand);
	mainForm.setCommandListener(this);
        display.setCurrent(mainForm);
        knownDevicesList.addCommand(serviceSearchCommand);
        knownDevicesList.addCommand(serviceListCommand);
        knownDevicesList.addCommand(backCommand);
        knownDevicesList.setCommandListener(this);
        knownServicesList.addCommand(connectCommand);
        knownServicesList.addCommand(backCommand);
        knownServicesList.setCommandListener(this);
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
         notifyDestroyed();
    }
    public void commandAction(Command c, Displayable s) {
        if (c == deviceSearchCommand) {
            currentMenu = MAIN_MENU;
	    client.startDeviceSearch();
       } else if (c == deviceListCommand) {
            currentMenu = DEVICE_MENU;
            client.retrieveDevicesFromRms();
            int noOfDevices = client.knownDevices.size();
            if (noOfDevices > 0) {
                Enumeration enum1 = client.knownDevices.keys();
                while(enum1.hasMoreElements()) {
                    knownDevicesList.append((String)enum1.nextElement(), null);
                }
                display.setCurrent(knownDevicesList);
            } else {
                alertMessage("No Devices Listed Make A Fresh Service Search");
            }
        } else if (c == serviceSearchCommand) {
            int index = knownDevicesList.getSelectedIndex();
            String deviceAddress = (String)client.knownDevices.get(knownDevicesList.getString(index));
            client.startServiceSearch(deviceAddress);
        } else if (c == serviceListCommand) {
            currentMenu = SERVICE_MENU;
            client.retrieveServicesFromRms();
            int noOfServices = client.knownServices.size();
            if (noOfServices > 0) {
                Enumeration enum1 = client.knownServices.keys();
                while(enum1.hasMoreElements()) {
                    knownServicesList.append((String)enum1.nextElement(), null);
                }
                display.setCurrent(knownServicesList);
            } else {
                alertMessage("No Services Listed Make A Fresh Service Search");
            }
       } else if (c == connectCommand) {
            currentMenu = CONNECT_MENU;
            int index = knownServicesList.getSelectedIndex();
            String serviceName = knownServicesList.getString(index);
            serviceThread = new Thread(this, serviceName);
            serviceThread.start();
       } else if (c == backCommand) {
             switch(currentMenu) {
                case DEVICE_MENU:
                    currentMenu = MAIN_MENU;
                    display.setCurrent(mainForm);
                    break;
                case SERVICE_MENU:
                    currentMenu = DEVICE_MENU;
                    display.setCurrent(knownDevicesList);
                    break;
                case CONNECT_MENU:
                    currentMenu = SERVICE_MENU;
                    display.setCurrent(knownServicesList);
                    break;
            }
        } else if (c == List.SELECT_COMMAND) {
          List currentList = (List)display.getCurrent();
          int index = currentList.getSelectedIndex();
          alertMessage("Listed :" + currentList.getString(index));
        } else if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
	}
    }
    public void run() {
        client.connectToService(serviceThread.getName());
    }
}

class KeyCapture extends Canvas {
    KeyProcessing kprocess = null;
    int ydown = 55;
    int xup = 55;
    int theKey = 0;
    int theHashCode = 0;

    KeyCapture() { }
 
    public void setClient(GSDBTRmsClient client) {
	kprocess = new KeyProcessing(client);
    }
    protected void keyPressed(int keyCode) {
	theKey = keyCode;
	if (kprocess != null) {
            theKey = kprocess.HandleKey(keyCode);
	}
        repaint(0, 0, getWidth(), getHeight());
    }
    public void paint(Graphics g) {
	g.setColor(123,123,123);
        g.fillRect(xup - 15, ydown - 15, xup + 75, ydown + 75);
        g.setColor(0,0,0);
        g.drawString("Key Code :" + String.valueOf(theKey), xup , ydown, 0);
    }
}

