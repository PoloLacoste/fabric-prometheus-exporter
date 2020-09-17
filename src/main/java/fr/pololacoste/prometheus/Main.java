package fr.pololacoste.prometheus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.eclipse.jetty.server.Server;

import java.net.InetSocketAddress;

public class Main implements ModInitializer {

    private Server server;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
    }

    private void onServerStart(MinecraftServer minecraftServer) {
        String host = "localhost";
        int port = 9225;

        InetSocketAddress address = new InetSocketAddress(host, port);
        server = new Server(address);
        server.setHandler(new MetricsController(minecraftServer));

        try {
            server.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
