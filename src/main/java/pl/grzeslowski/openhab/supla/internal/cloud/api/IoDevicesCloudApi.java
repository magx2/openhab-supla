package pl.grzeslowski.openhab.supla.internal.cloud.api;

import io.swagger.client.model.Device;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface IoDevicesCloudApi {
    Device getIoDevice(int id, List<String> include) throws Exception;

    List<Device> getIoDevices(List<String> include) throws Exception;
}
