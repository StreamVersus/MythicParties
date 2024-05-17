package ru.streamversus.mythicparties.entrypoints;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

//TODO: MOVE ALL CONTENT FROM BUKKIT ENTRY
@Plugin(id = "mythicparties", name = "MythicParties", version = "${version}")
public class MythicPartiesVelocity {
    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public MythicPartiesVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        logger.info("test");
    }
}
