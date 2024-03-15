package it.qbsoftware.boot.handlers;

import com.google.inject.Inject;
import it.qbsoftware.core.Jmap;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadHandler extends Handler.Abstract {
    final Logger logger = LoggerFactory.getLogger(DownloadHandler.class);
    public static final String HANDLER_ENDPOINT = "/download";
    public static final String HANDLER_CONTEXT_PATH = "/";
    private static Jmap jmap;

    @Inject
    public DownloadHandler(Jmap jmap) {
        DownloadHandler.jmap = jmap;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (!(request.getMethod().equals("POST")
                && Request.getPathInContext(request).equals(HANDLER_ENDPOINT))) {
            return false;
        }

        logger.info("Richiesta HTTP POST all'endpoint /download");
        response.setStatus(404); // Not implemented
        Content.Sink.write(response, true, "", callback);
        return true;
    }
}
