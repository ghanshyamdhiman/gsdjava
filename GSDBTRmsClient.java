import javax.bluetooth.*;
import javax.microedition.io.*;
import java.io.*;
import java.util.*;
import javax.microedition.rms.*;


public class GSDBTRmsClient implements DiscoveryListener 
{
    private static final UUID KEYBOARD_SERVER_UUID = new UUID("14dda1651f4f4a2da48c3b612ec107e5", false);
 
    private LocalDevice	localDevice = null ;
    private DiscoveryAgent discoveryAgent = null ;
    
    private boolean inquiring = false ;
    private int serviceSearchID = 0;
     
    private Vector devicesFound = new Vector();
    private Vector servicesFound = new Vector();
    public Hashtable knownObjects = new Hashtable();
    public Hashtable knownDevices = new Hashtable();
    public Hashtable knownServices = new Hashtable();
  
    private OutputStream dos = null;
    private GSDBTRms mainMidlet = null;
    
    private RecordStore deviceRecordStore = null;
    private RecordStore serviceRecordStore = null;
    
    private static final String DEVICE_DB = "DevicesStore";
    private static final String SERVICE_DB = "ServicesStore";
          
    public GSDBTRmsClient(GSDBTRms mainMidlet) {
	this.mainMidlet = mainMidlet;
              
        if (localDevice == null) {
            try {
                localDevice = LocalDevice.getLocalDevice();
            } catch ( BluetoothStateException bse ){
                mainMidlet.alertMessage("Bluetooth is not on");
            }
        }
    }
    private void storeDevicesToRms(RemoteDevice remoteDevice) {
        String deviceAddress = remoteDevice.getBluetoothAddress();
        knownObjects.put(deviceAddress, remoteDevice);
        String deviceName = null;
        try {
                deviceName = remoteDevice.getFriendlyName(false);
        } catch (IOException ioe) {
               deviceName = "name not found";
        }
        
        try {
            deviceRecordStore = RecordStore.openRecordStore(DEVICE_DB, true);
        } catch (RecordStoreException rse) {
            System.out.println("Rms Open Error" + rse);
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            byte record[];
            dos.writeUTF(deviceName);
            dos.writeUTF(deviceAddress);
            dos.flush();
            record = baos.toByteArray();
            deviceRecordStore.addRecord(record, 0, record.length);
            baos.reset();
            baos.close();
            dos.close();
        } catch (Exception e) {
            System.out.println("Record Adding Error" + e);
        }
         try {
            deviceRecordStore.closeRecordStore();
        } catch (RecordStoreException rse) {
            System.out.println("RMS Close Error" + rse);
        }
    }
     public void retrieveDevicesFromRms() {
        knownDevices.clear();
     
        try {
            deviceRecordStore = RecordStore.openRecordStore(DEVICE_DB, false);
        } catch (RecordStoreException rse) {
            System.out.println("Rms Open Error" + rse);
        }
        
        try {
            byte record[] = new byte[255];
            ByteArrayInputStream bais = new ByteArrayInputStream(record);
            DataInputStream dis = new DataInputStream(bais);
            String deviceName = null;
            String deviceAddress = null;
            for(int i = 1; i <= deviceRecordStore.getNumRecords(); i ++) {
                deviceRecordStore.getRecord(i, record, 0);
                deviceName = dis.readUTF();
                deviceAddress = dis.readUTF();
                knownDevices.put(deviceName, deviceAddress);
                bais.reset();
            }
            bais.close();
            dis.close();
        } catch (Exception e) {
            System.out.println("Data Retrieve Error" + e);
        }
       
        try {
            deviceRecordStore.closeRecordStore();
        } catch (RecordStoreException rse) {
            System.out.println("RMS Close Error" + rse);
        }
    }
    public void startDeviceSearch() {	
        if (discoveryAgent == null) {
            discoveryAgent = localDevice.getDiscoveryAgent();
        }
        try {
            devicesFound.removeAllElements();
            knownObjects.clear();
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
        devicesFound.addElement(remoteDevice);
      
    }
    public void inquiryCompleted(int discType) {
        inquiring = false;
        String msg = " \n Inquiry : " ;
        
        switch ( discType ) {
            case DiscoveryListener.INQUIRY_COMPLETED :
                if ( devicesFound.size() == 0) {
                    msg += "Completed : No devices found!";
                } else {
                    msg += " Completed : No of devices found : " + String.valueOf(devicesFound.size());
                    for(int i = 0; i < devicesFound.size(); i++) {
                        storeDevicesToRms((RemoteDevice)devicesFound.elementAt(i));
                    }
                }
                break;
           case INQUIRY_ERROR :
                if(devicesFound.size() > 0) {
                    msg += " Error : But No of devices found : " + String.valueOf(devicesFound.size());
                    for(int i = 0; i < devicesFound.size(); i++) {
                        storeDevicesToRms((RemoteDevice)devicesFound.elementAt(i));
                    }
                } else {
                    msg +=  " Error : Error occured during inquiry.";
                }
                break;
           case INQUIRY_TERMINATED :
                msg += "Terminated : Search Terminated";
                break;
        }
        mainMidlet.alertMessage ( msg ) ;
    }
    
    public void sendKey(char keyCode) {
	try {
            dos.write(keyCode) ;
            dos.flush();
	} catch (Exception e) {
            System.out.println("Could not write to out stream");
	}
    }
    public void servicesDiscovered(int transID, ServiceRecord[] serviceRecords) { 
       servicesFound.addElement(serviceRecords);
    }
    private void storeServicesToRms(ServiceRecord[] serviceRecord) {
        try {
            serviceRecordStore = RecordStore.openRecordStore(SERVICE_DB, true);
        } catch (RecordStoreException rse) {
            System.out.println("Rms Open Error" + rse);
        }
                
        DataElement nameElement = null;
        String serviceName = null;
        String serviceUrl = null;
        
        try {
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos);
               
                for( int i = 0; i < serviceRecord.length; i++) {
                    nameElement = (DataElement)serviceRecord[i].getAttributeValue(0x100);
                     if (nameElement != null && nameElement.getDataType() == DataElement.STRING ){
                         serviceName = (String)nameElement.getValue();
                         serviceUrl = (String)serviceRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                     } else {
                         serviceName = "new service";
                         serviceUrl = (String)serviceRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                     }
                      byte record[];
                      dos.writeUTF(serviceName);
                      dos.writeUTF(serviceUrl);
                      dos.flush();
                      record = baos.toByteArray();
                      serviceRecordStore.addRecord(record, 0, record.length);
                      baos.reset();
                }
                baos.close();
                dos.close();
         } catch (Exception e) {
                System.out.println("Record Adding Error" + e);
         }
        
         try {
            serviceRecordStore.closeRecordStore();
         } catch (RecordStoreException rse) {
            System.out.println("RMS Close Error" + rse);
         }
    }
    public void retrieveServicesFromRms() {
        knownServices.clear();
     
        try {
            serviceRecordStore = RecordStore.openRecordStore(SERVICE_DB, false);
        } catch (RecordStoreException rse) {
            System.out.println("Rms Open Error" + rse);
        }
        
        try {
            byte record[] = new byte[255];
            ByteArrayInputStream bais = new ByteArrayInputStream(record);
            DataInputStream dis = new DataInputStream(bais);
            String serviceName = null;
            String serviceUrl = null;
            for(int i = 1; i <= serviceRecordStore.getNumRecords(); i ++) {
                serviceRecordStore.getRecord(i, record, 0);
                serviceName = dis.readUTF();
                serviceUrl = dis.readUTF();
                knownServices.put(serviceName, serviceUrl);
                bais.reset();
            }
            bais.close();
            dis.close();
        } catch (Exception e) {
            System.out.println("Data Retrieve Error" + e);
        }
       
        try {
            serviceRecordStore.closeRecordStore();
        } catch (RecordStoreException rse) {
            System.out.println("RMS Close Error" + rse);
        }
    }
    public void serviceSearchCompleted(int transID, int respCode) {
        serviceSearchID = 0;
        int noOfServices = servicesFound.size();
               
        String msg = "\n Service Search : ";
        switch(respCode) {
            case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
                if ( noOfServices == 0) {
                    msg += "Completed : No Services Found!";
                } else {
                    msg += " Completed : No Of Services Found : " + String.valueOf(noOfServices);
                    for(int i = 0; i < noOfServices; i++) {
                        storeServicesToRms((ServiceRecord[])servicesFound.elementAt(i));
                    }
                }
                break;
           case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
                msg += "Device not reachable";
                break;
            case DiscoveryListener.SERVICE_SEARCH_ERROR:
                msg += "Error";
                break;
            case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
                msg += "No Records Found";
                break;
            case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
                msg += "Terminated";
                break;
        }
        mainMidlet.alertMessage(msg);
    }
    public void startServiceSearch(String deviceAddress) {
        
        RemoteDevice remoteDevice = (RemoteDevice)knownObjects.get(deviceAddress);
            
        int attrs[] = {0x100,0x101,0x102};
        UUID[] uuidSet = {KEYBOARD_SERVER_UUID};
        
        try {
            servicesFound.removeAllElements();
            discoveryAgent.searchServices(attrs, uuidSet, remoteDevice, this);
        } catch (BluetoothStateException bse) {
            mainMidlet.alertMessage("Error starting service search");
            return;
        }
    }
    public void connectToService(String serviceName) {
        String url = (String)knownServices.get(serviceName);
        StreamConnection conn = null;
        try {
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
    

    
