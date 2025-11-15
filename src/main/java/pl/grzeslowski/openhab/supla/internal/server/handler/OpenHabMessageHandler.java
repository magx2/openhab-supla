package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Objects.requireNonNull;

import io.netty.channel.socket.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.jsupla.server.api.MessageHandler;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTraitParser;

@RequiredArgsConstructor
public final class OpenHabMessageHandler implements MessageHandler {
    private static final AtomicLong ID = new AtomicLong();

    private final long id = ID.incrementAndGet();
    private final Logger log = LoggerFactory.getLogger(OpenHabMessageHandler.class.getName() + "#" + id);
    private final Object lock = new Object();
    private final AtomicReference<SuplaThing> currentThing = new AtomicReference<>();
    private final AtomicReference<Writer> writer = new AtomicReference<>();
    private final Set<String> discoveredThings = Collections.synchronizedSet(new HashSet<>());

    private final SuplaThingRegistry registry;
    private final ServerDiscoveryService serverDiscoveryService;
    private final SocketChannel socketChannel;

    @Override
    public void active(Writer writer) {
        log.debug("active");
        this.writer.set(writer);
    }

    @Override
    public void inactive() {
        log.debug("inactive");
        var local = currentThing.getAndSet(null);
        if (local != null) {
            local.inactive();
        }
        writer.set(null);
        discoveredThings.forEach(serverDiscoveryService::removeDevice);
        discoveredThings.clear();
    }

    @Override
    public void socketException(Throwable exception) {
        var thing = currentThing.getAndSet(null);
        if (thing == null) {
            log.warn(
                    "Got exception from socket without having handler attached. Breaking the socket connection",
                    exception);
        } else {
            thing.socketException(exception);
        }
        clear();
    }

    @Override
    public void handle(ToServerProto proto) {
        synchronized (lock) {
            synchronizedHandle(proto);
        }
    }

    /** Metod synchronized over lock */
    private void synchronizedHandle(ToServerProto proto) {
        // the current thing is set that means it already registered
        var thing = currentThing.get();
        if (thing != null) {
            thing.handle(proto);
            return;
        }

        // register process
        var register = RegisterDeviceTraitParser.parse(proto);
        if (register.isEmpty()) {
            log.debug("There is no Supla thing and the device did not send register message, but {}", proto);
            return;
        }
        var entity = register.get();
        var guid = entity.getGuid();
        var suplaThingOptional = registry.findSuplaThing(guid);
        if (suplaThingOptional.isEmpty()) {
            log.debug("There is no handler for device with GUID={}", guid);
            serverDiscoveryService.addDevice(entity);
            discoveredThings.add(entity.getGuid());
            return;
        }
        var suplaThing = suplaThingOptional.get();
        suplaThing.active(requireNonNull(writer.get(), "writer is null"));
        var registerResult = suplaThing.register(entity, this);
        if (!registerResult) {
            log.debug("Could not register device GUID={}", guid);
            return;
        }
        // correctly registered
        currentThing.set(suplaThing);
    }

    public void clear() {
        log.debug("clear");
        currentThing.set(null);
        var close = socketChannel.close();
        close.addListener(__ -> log.debug("Closed channel"));
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof OpenHabMessageHandler that)) return false;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
