package it.qbsoftware.boot.handlers;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadHandler extends Handler.Abstract {
    final Logger logger = LoggerFactory.getLogger(UploadHandler.class);
    public static final String HANDLER_ENDPOINT = "/upload";
    public static final String HANDLER_CONTEXT_PATH = "/";

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (!(request.getMethod().equals("POST")
                && Request.getPathInContext(request).equals(HANDLER_ENDPOINT))) {
            return false;
        }

        logger.info("HTTP POST request to the endpoint /upload");
        response.setStatus(404); // Not implemented
        Content.Sink.write(response, true, "", callback);
        return true;
    }
}
