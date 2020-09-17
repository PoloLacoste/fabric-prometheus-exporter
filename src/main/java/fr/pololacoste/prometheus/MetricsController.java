package fr.pololacoste.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsController extends AbstractHandler {

    private MinecraftServer server;

    private final Gauge players = Gauge.build().name("mc_players").help("Online players").labelNames("name").create().register();
    private final Gauge loadedChunks = Gauge.build().name("mc_loaded_chunks_total").help("Chunks loaded per world").labelNames("world").create().register();
    private final Gauge playersOnline = Gauge.build().name("mc_players_online_total").help("Players currently online per world").labelNames("world").create().register();
    private final Gauge entities = Gauge.build().name("mc_entities_total").help("Entities loaded per world").labelNames("world").create().register();
    private final Gauge livingEntities = Gauge.build().name("mc_living_entities_total").help("Living entities loaded per world").labelNames("world").create().register();
    private final Gauge memory = Gauge.build().name("mc_jvm_memory").help("JVM memory usage").labelNames("type").create().register();

    private final Gauge offlinePlayers = Gauge.build().name("mc_offline_players").help("Offline players length").labelNames("name").create().register();

    public MetricsController(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (!target.equals("/metrics")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        memory.labels("max").set(Runtime.getRuntime().maxMemory());
        memory.labels("used").set(Runtime.getRuntime().totalMemory());

        int id = 0;

        for (ServerWorld world : server.getWorlds()) {

            String name = getWorldName(id);
            id++;

            loadedChunks.labels(name).set(world.getChunkManager().getLoadedChunkCount());
            playersOnline.labels(name).set(world.getPlayers().size());

            AtomicInteger entityCount = new AtomicInteger();
            AtomicInteger livingEntityCount = new AtomicInteger();

            world.iterateEntities().forEach(entity -> {
                if(entity.isAlive()) {
                    livingEntityCount.getAndIncrement();
                }
                entityCount.getAndIncrement();
            });

            entities.labels(name).set(entityCount.get());
            livingEntities.labels(name).set(livingEntityCount.get());
        }

        AtomicInteger offline = new AtomicInteger();

        server.getPlayerManager().getPlayerList().forEach(player -> {
            if(player.isDisconnected()) {
                offline.getAndIncrement();
            }
            else {
                players.labels(player.getName().asString()).set(player.pingMilliseconds);
            }
        });

        offlinePlayers.labels("offline").set(offline.get());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(TextFormat.CONTENT_TYPE_004);

        TextFormat.write004(response.getWriter(), CollectorRegistry.defaultRegistry.metricFamilySamples());

        baseRequest.setHandled(true);
    }

    private String getWorldName(int id)
    {
        switch (id)
        {
            case 0: return "Overworld";
            case 1: return "Nether";
            case 2: return "End";
        }

        return Integer.toString(id);
    }
}
