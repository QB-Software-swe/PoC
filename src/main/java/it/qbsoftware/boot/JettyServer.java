package it.qbsoftware.boot;

import com.google.inject.Inject;
import it.qbsoftware.boot.handlers.ApiHandler;
import it.qbsoftware.boot.handlers.DownloadHandler;
import it.qbsoftware.boot.handlers.UploadHandler;
import it.qbsoftware.boot.handlers.WellKnownHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class JettyServer {

    private Server server;

    @Inject
    public JettyServer(
            WellKnownHandler wellKnownHandler,
            ApiHandler apiHandler,
            UploadHandler uploadHandler,
            DownloadHandler downloadHandler) {
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setName("JMAPServer");

        server = new Server(queuedThreadPool);

        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(9999);
        server.addConnector(serverConnector);

        // Server end points
        ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();
        contextHandlerCollection.addHandler(
                new ContextHandler(wellKnownHandler, WellKnownHandler.HANDLER_CONTEXT_PATH));
        contextHandlerCollection.addHandler(new ContextHandler(apiHandler));
        contextHandlerCollection.addHandler(new ContextHandler(uploadHandler));
        contextHandlerCollection.addHandler(new ContextHandler(downloadHandler));
        server.setHandler(contextHandlerCollection);
    }

    public void start() {
        try {
            server.start();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }
}
