package driver;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.eclipse.californium.core.CoapClient;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.semiot.commons.namespaces.DUL;
import ru.semiot.commons.namespaces.NamespaceUtils;
import ru.semiot.commons.namespaces.SEMIOT;
import ru.semiot.platform.deviceproxyservice.api.drivers.ActuatingDeviceDriver;
import ru.semiot.platform.deviceproxyservice.api.drivers.CommandExecutionException;
import ru.semiot.platform.deviceproxyservice.api.drivers.CommandExecutionResult;
import ru.semiot.platform.deviceproxyservice.api.drivers.DeviceDriverManager;
import ru.semiot.platform.deviceproxyservice.api.drivers.DriverInformation;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class DeviceDriverImpld implements ActuatingDeviceDriver, ManagedService {
  
  private static final Logger logger = LoggerFactory.getLogger(
      DeviceDriverImpld.class);
  
  private boolean isConfigured = false;
  private static String groupId = "1";
  private static String coapUri = "coap://vs0.inf.ethz.ch/obs";
  private static final String DRIVER_NAME = "Plain Lamp Driver";
  private final DriverInformation info = new DriverInformation(
      Activator.DRIVER_PID,
      URI.create("https://raw.githubusercontent.com/semiotproject/semiot-platform/" +
          "master/device-proxy-service-drivers/mock-plain-lamp/" +
          "src/main/resources/ru/semiot/drivers/mocks/plainlamp/prototype.ttl#PlainLamp"));
  private Map<String, PlainLamp> devices = new HashMap<String, PlainLamp>();
  private static CoapClient coapClient;
  private volatile DeviceDriverManager manager;
  
  public static void main(String[] args) {
    coapClient = new CoapClient("coap://192.168.32.66:5683/light");
    
    byte state = coapClient.get().getPayload()[0];
    if(state != (byte)0) {
      coapClient.put(new byte[] {(byte)0}, 0);
    } else {
      coapClient.put(new byte[] {(byte) 255}, 0);
    }
    // manager.registerCommand(result);
  }
  
  public void start() {
    try {
      logger.info("Starting driver");
      manager.registerDriver(info);
      PlainLamp lamp = new PlainLamp("perccom" + groupId);
      devices.put(lamp.getId(), lamp);
      manager.registerDevice(info, new PlainLamp("perccom" + groupId));
      
      coapClient = new CoapClient(coapUri);
      /*coapClient.observe(new CoapHandler() {
        
        @Override
        public void onLoad(CoapResponse arg0) {
          logger.info(arg0.getResponseText());
        }
        
        @Override
        public void onError() {

        }
      });*/
    } catch (Throwable e) {
      logger.error(e.getMessage(), e);
    }
  }
  
  public void stop() {
    devices.clear();

    logger.info("{} stopped!", DRIVER_NAME);
  }
  
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    synchronized (this) {
      if (properties != null) {
        //if (!isConfigured) {
          logger.info("123");
          groupId = (String) properties.get("ru.semiot.drivers.mocks.plainlamp.groups.number");
          logger.info("2");
          coapUri = (String) properties.get("ru.semiot.drivers.mocks.plainlamp.coap.uri");
          logger.info("3");
          isConfigured = true;
        //}
      }
    }

  }

  @Override
  public String getDriverName() {
    return DRIVER_NAME;
  }

  @Override
  public CommandExecutionResult executeCommand(Model command)
      throws CommandExecutionException {
    logger.debug("executeCommand");
    //Algorithm:
    // 1. Check whether the command is supported
    // 1. Get the device id
    // 1. Run the command against the given device id
    // 1. Notify others about success or failure

    try {
      //ResourceFactory.createProperty("http://www.loa-cnr.it/ontologies/DUL.owl#involvesAgent")
      NodeIterator deviceIterator = command.listObjectsOfProperty(DUL.involvesAgent);
      NodeIterator operationIterator = command.listObjectsOfProperty(SEMIOT.targetOperation);

      if (deviceIterator.hasNext() && operationIterator.hasNext()) {
        Resource deviceUri = (Resource) deviceIterator.next();
        Resource operationUri = (Resource) operationIterator.next();

        String deviceId = NamespaceUtils.extractLocalName(deviceUri.getURI());

        if (devices.containsKey(deviceId)) {
          PlainLamp device = devices.get(deviceId);

          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
          }
          CommandExecutionResult result = new CommandExecutionResult(
              device, command, ZonedDateTime.now());
          
          // get value
          // put new value 0 | 255

          manager.registerCommand(result);
          
          return result;
        } else {
          throw CommandExecutionException.systemNotFound();
        }
      } else {
        throw CommandExecutionException.badCommand("Some information is missing!");
      }
    } catch (Throwable e) {
      logger.error(e.getMessage(), e);

      return null;
    }
  }
}
