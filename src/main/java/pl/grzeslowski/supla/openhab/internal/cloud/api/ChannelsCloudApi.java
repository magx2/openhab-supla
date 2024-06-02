package pl.grzeslowski.supla.openhab.internal.cloud.api;

import io.swagger.client.ApiException;
import io.swagger.client.model.ChannelExecuteActionRequest;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface ChannelsCloudApi {
    void executeAction(ChannelExecuteActionRequest body, Integer id) throws ApiException;

    io.swagger.client.model.Channel getChannel(int id, List<String> include) throws Exception;

    List<io.swagger.client.model.Channel> getChannels(List<String> include) throws Exception;
}
