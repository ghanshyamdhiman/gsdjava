// BTDeviceSearch.java

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.*;

public class GSDJavaKeyboard extends MIDlet implements CommandListener, Runnable {
    private GSDJavaKeyboardClient client ;
    private GSDKeyCapture captureKeys;
    
    public List knownDevicesList = new List("Known Devices", List.IMPLICIT);
    public List knownServicesList = new List("", List.IMPLICIT);
    
    private Command searchDevices = new Command("Search Devices", Command.SCREEN, 1);
    private Command listDevices = new Command("List Devices", Command.SCREEN, 2);
    private Command searchServices = new Command("Search Services", Command.SCREEN, 1);
    private Command listServices = new Command("List Services", Command.OK, 2);
    private Command backCommand = new Command("Back", Command.BACK, 2);
    private Command exitCommand = new Command("Exit", Command.EXIT, 3);
    private Command connect = new Command("Connect", Command.ITEM, 1);
     
    final private static int MAIN_MENU = 00;
    final private static int DEVICE_MENU = 11;
    final private static int SERVICE_MENU = 22;
    final private static int CONNECT_MENU = 33;
    private int currentMenu = 0;
    
    private Form mainForm = new Form("KSET BT Keyboard");
    public Display display ;
    public Thread serviceThread = null;
   
      
    public GSDJavaKeyboard() {
        display = Display.getDisplay(this);
        client = new GSDJavaKeyboardClient(this);
    }
    public void alertMessage(String message) {
        Alert al = new Alert("Message", message, null, null);
	al.setTimeout(Alert.FOREVER);
	display.setCurrent(al);
    }
    public void showTheCanvas() {
	Ticker t = new Ticker("Press any key");
	captureKeys = new GSDKeyCapture();
        captureKeys.addCommand(backCommand);
        captureKeys.addCommand(exitCommand);
        captureKeys.setCommandListener(this);
	captureKeys.setTicker(t);
	captureKeys.setClient(client);
	display.setCurrent(captureKeys);
   }
    public void startApp() {
        mainForm.addCommand(searchDevices);
        mainForm.addCommand(listDevices);
        mainForm.addCommand(exitCommand);
	mainForm.setCommandListener(this);
        display.setCurrent(mainForm);
        knownDevicesList.addCommand(searchServices);
        knownDevicesList.addCommand(listServices);
        knownDevicesList.addCommand(backCommand);
        knownDevicesList.setCommandListener(this);
        knownServicesList.addCommand(connect);
        knownServicesList.addCommand(backCommand);
        knownServicesList.setCommandListener(this);
    }
    
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
         notifyDestroyed();
    }
    public void commandAction(Command c, Displayable s) {
        if (c == searchDevices) {
	   client.searchRemoteDevices();
        } else if(c == listDevices) {
           currentMenu = DEVICE_MENU;
           client.listRemoteDevices();
           if (knownDevicesList.size() > 0) {
               display.setCurrent(knownDevicesList);
           } else {
               alertMessage("No Devices Listed Make A Fresh Device Search");
           }
        } else if (c == searchServices) {
            int index = knownDevicesList.getSelectedIndex();
            String remoteDeviceAddress = knownDevicesList.getString(index);
            client.searchRemoteServices(remoteDeviceAddress);
        } else if (c == listServices) {
            currentMenu = SERVICE_MENU;
            client.listServices();
            if (knownServicesList.size() > 0) {
            display.setCurrent(knownServicesList);
            } else {
                alertMessage("No Services Listed Make A Fresh Service Search");
            }
        } else if (c == connect) {
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
                     try {
                        serviceThread.join();
                    } catch (InterruptedException ine) {}
                    display.setCurrent(knownServicesList);
                    break;
            }
        } else if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
	}
    }
    public void run() {
        client.connectToService(serviceThread.getName());
    }
}

class GSDKeyCapture extends Canvas {
    GSDKeyProcessing kprocess = null;
    
    private int ydown = 55;
    private int xup = 55;
    private int theKey = 0;
    private int theHashCode = 0;

    GSDKeyCapture() {
    }
 
    public void setClient(GSDJavaKeyboardClient client) {
     	kprocess = new GSDKeyProcessing(client);
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

