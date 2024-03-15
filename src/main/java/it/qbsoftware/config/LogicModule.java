package it.qbsoftware.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import it.qbsoftware.boot.JettyServer;
import it.qbsoftware.core.Jmap;

public class LogicModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Jmap.class).in(Singleton.class);
        bind(JettyServer.class).in(Singleton.class);
    }
}
