package it.qbsoftware.boot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import it.qbsoftware.config.LogicModule;
import it.qbsoftware.config.PersistenceModule;
import it.qbsoftware.core.util.GenPocData;
import org.slf4j.LoggerFactory;

public class Boot {

    static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);
    }

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new LogicModule(), new PersistenceModule());
        injector.getInstance(GenPocData.class).generate();
        injector.getInstance(JettyServer.class).start();
    }
}
