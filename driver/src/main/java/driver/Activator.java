package driver;


import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import ru.semiot.platform.deviceproxyservice.api.drivers.ActuatingDeviceDriver;
import ru.semiot.platform.deviceproxyservice.api.drivers.DeviceDriver;
import ru.semiot.platform.deviceproxyservice.api.drivers.DeviceDriverManager;

import java.util.Properties;

/**
 *
 * @author Daniil Garayzuev <garayzuev@gmail.com>
 */
public class Activator extends DependencyActivatorBase {
  public static final String DRIVER_PID = "ru.semiot.drivers.mocks.plainlamp";

  @Override
  public void init(BundleContext bundleContext,
      DependencyManager manager)
      throws Exception {
    Properties properties = new Properties();
    properties.setProperty(Constants.SERVICE_PID, DRIVER_PID);

    manager.add(createComponent()
        .setInterface(new String[]{
                ActuatingDeviceDriver.class.getName(),
                DeviceDriver.class.getName(),
                ManagedService.class.getName()},
            properties)
        .setImplementation(DeviceDriverImpld.class)
        .add(createServiceDependency()
            .setService(DeviceDriverManager.class)
            .setRequired(true))
        .add(createConfigurationDependency().setPid(DRIVER_PID)));
  }

}
