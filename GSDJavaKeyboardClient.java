import javax.bluetooth.*;
import javax.microedition.io.*;
import java.io.*;
import java.util.*;

public class GSDJavaKeyboardClient implements DiscoveryListener 
{
    private static final UUID KEYBOARD_SERVER_UUID = new UUID("14dda1651f4f4a2da48c3b612ec107e5", false);
 
    private LocalDevice	localDevice = null ;
    private DiscoveryAgent discoveryAgent = null ;
    
    private boolean inquiring = false ;
    private int serviceSearchID = 0;
     
    public Hashtable knownDevices = new Hashtable();
    public Hashtable knownServices = new Hashtable();
  
    private OutputStream dos = null;
    private GSDJavaKeyboard mainMidlet = null ;
      
    public GSDJavaKeyboardClient(GSDJavaKeyboard mainMidlet){
	this.mainMidlet = mainMidlet;
              
        if (localDevice == null) {
            try {
                localDevice = LocalDevice.getLocalDevice ( ) ;
            } catch ( BluetoothStateException bse ){
                mainMidlet.alertMessage("Bluetooth is not on");
            }
        }
    }
   
    private void storeDevice(RemoteDevice remoteDevice) {
        String deviceName = null;
        if (knownDevices.contains(remoteDevice)) {
            ;
        } else {
           try {
                deviceName = remoteDevice.getFriendlyName(false);
           } catch (IOException ioe) {
                deviceName = remoteDevice.getBluetoothAddress();
           }
           if (deviceName.equals("")) {
                deviceName = remoteDevice.getBluetoothAddress();
           }
           knownDevices.put(deviceName, remoteDevice);
           boolean nameListed = false;
           for(int i = 0; i < mainMidlet.knownDevicesList.size(); i++)
           {
               if (mainMidlet.knownDevicesList.getString(i).equals(deviceName)) {
                   nameListed = true;
                   break;
               }
           }
           if(!nameListed) {
            mainMidlet.knownDevicesList.append(deviceName, null);
           }
        }
    }
    public void listRemoteDevices() {
        if (discoveryAgent == null) {
            discoveryAgent = localDevice.getDiscoveryAgent();
        }
        if (knownDevices == null){
            knownDevices = new Hashtable();
        } else {
            knownDevices.clear();
        }
        mainMidlet.knownDevicesList.deleteAll();
        RemoteDevice[] remoteDevices = discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);
        if (remoteDevices != null) {
            synchronized(knownDevices) {
                for (int i = remoteDevices.length-1; i >= 0; i--) {
                    storeDevice(remoteDevices[i]);
                }
            } 
        }
        remoteDevices = null;
        remoteDevices = discoveryAgent.retrieveDevices(DiscoveryAgent.CACHED);
        if (remoteDevices != null) {
            synchronized(knownDevices) {
                for (int i = remoteDevices.length-1; i >= 0; i--) {
                    storeDevice(remoteDevices[i]);
                }
            }
        }
        if (knownDevices.isEmpty()) {
            mainMidlet.alertMessage("No Preknown of Cached Device make a fresh Device Search");
        }
    }
    public void searchRemoteDevices() {	
        if (discoveryAgent == null) {
            discoveryAgent = localDevice.getDiscoveryAgent();
        }
        knownDevices.clear();
        mainMidlet.knownDevicesList.deleteAll();
        
        try {
            inquiring = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
        } catch(BluetoothStateException bse) {
            mainMidlet.alertMessage("Bluetooth error while starting inquiry");
            return;
        }
        if (!inquiring) {
            mainMidlet.alertMessage("Error starting inquiry");
        }
    }
    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
        storeDevice(remoteDevice);
    }
    public void inquiryCompleted(int discType) {
        inquiring = false;
        String msg = " \n Inquiry : " ;
        switch ( discType ) {
            case DiscoveryListener.INQUIRY_COMPLETED :
                if (knownDevices.size() == 0) {
                    msg += "Completed : No devices found!";
                } else {
                    getFriendlyNames();
                    msg += " Completed : No of devices found : " + String.valueOf(knownDevices.size());
                }
                break;
           case INQUIRY_ERROR :
                if(knownDevices.size() > 0) {
                    msg += " Error : But No of devices found : " + String.valueOf(knownDevices.size());
                    getFriendlyNames();
                } else {
                    msg +=  " Error : Error occured during inquiry.";
                }
                break;
           case INQUIRY_TERMINATED :
                msg += " Terminated : Search Terminated ";
                if(knownDevices.size() > 0) {
                    getFriendlyNames();
                }
                break;
        }
        mainMidlet.alertMessage(msg);
    }
    private void getFriendlyNames() {
        String friendlyName = null;
        RemoteDevice remoteDevice = null;
        Enumeration enum1 = knownDevices.keys();
        
        while(enum1.hasMoreElements()) {
            remoteDevice = (RemoteDevice)knownDevices.get((String)enum1.nextElement());
            try {
                friendlyName = remoteDevice.getFriendlyName(false);
            } catch (IOException ioe) {
               continue;
            }
            if (!friendlyName.equals("")) {
                mainMidlet.knownDevicesList.append(friendlyName, null);
            }
        }
    }
    public void sendKey(char keyCode) {
	try {
            dos.write ( keyCode ) ;
            dos.flush ( ) ;
	} catch ( Exception e )	{
            System.out.println ( " Could not write to out stream " ) ;
	}
    }
    public void servicesDiscovered(int transID, ServiceRecord[] serviceRecords) {      
       for (int i = 0;i < serviceRecords.length ; i++) {
            storeServices(serviceRecords[i]);
       }
    }
    public void listServices() {
        String serviceName = null;
        Enumeration enum1 = knownServices.keys();
        
        while(enum1.hasMoreElements()) {
            serviceName = (String)enum1.nextElement();
            boolean nameListed = false;
            for(int i = 0; i < mainMidlet.knownServicesList.size(); i++)
            {
               if (mainMidlet.knownServicesList.getString(i).equals(serviceName)) {
                   nameListed = true;
                   break;
               }
           }
           if(!nameListed) {
            mainMidlet.knownServicesList.append(serviceName, null);
           }
       }
    }
    private void storeServices(ServiceRecord serviceRecord) {
        DataElement nameElement = null;
        String serviceName = null;
        
        if (knownServices.contains(serviceRecord)) {
            ;
        } else {
             nameElement = (DataElement)serviceRecord.getAttributeValue(0x100);
             if (nameElement != null && nameElement.getDataType() == DataElement.STRING ){
                 serviceName = (String)nameElement.getValue();
             } else {
                 serviceName = "new service";
             }
             knownServices.put(serviceName, serviceRecord);
            boolean nameListed = false;
            for(int i = 0; i < mainMidlet.knownServicesList.size(); i++)
            {
               if (mainMidlet.knownServicesList.getString(i).equals(serviceName)) {
                   nameListed = true;
                   break;
               }
           }
           if(!nameListed) {
            mainMidlet.knownServicesList.append(serviceName, null);
          }
       }
    }
    public void serviceSearchCompleted(int transID, int respCode) {
        serviceSearchID = 0;
        String msg = "\n Service Search : ";
        switch(respCode) {
            case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
                msg += "Completed";
                break;
            case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
                msg += "Device not reachable";
                break;
            case DiscoveryListener.SERVICE_SEARCH_ERROR:
                msg += "Error";
                break;
            case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
                msg += "No services found";
                break;
            case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
                msg += "Search terminated";
                break;
        }
        mainMidlet.alertMessage(msg);
    }
    public void searchRemoteServices(String remoteDeviceAddress) {
        RemoteDevice remoteDevice = (RemoteDevice)knownDevices.get(remoteDeviceAddress);
        
        if (knownServices == null) {
            knownServices = new Hashtable();
        } else {
            knownServices.clear();
        }
        mainMidlet.knownServicesList.deleteAll();
        
        int attrs[] = {0x100,0x101,0x102};
        UUID[] uuidSet = {KEYBOARD_SERVER_UUID};
        
        try {
            discoveryAgent.searchServices(attrs, uuidSet, remoteDevice, this);
        } catch (BluetoothStateException bse) {
            mainMidlet.alertMessage("Error starting service search");
            return;
        }
    }
    public void connectToService(String serviceName) {
        ServiceRecord serviceRecord = (ServiceRecord)knownServices.get(serviceName);
        StreamConnection conn = null;
        String url = null;
        try {
            url = serviceRecord.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            conn = (StreamConnection) Connector.open(url);
        } catch (IOException e) {
            System.err.println("Note: can't connect to: " + url);
        }
        try {
            dos = conn.openOutputStream();
            mainMidlet.showTheCanvas();
        } catch (IOException e) {
           System.err.println("Can't write to server for: " + url);
        }
    }
}
    

    
