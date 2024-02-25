package it.qbsoftware.boot.handlers;

import it.qbsoftware.core.util.JmapSingleton;
import it.qbsoftware.core.util.RequestResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WellKnownHandler extends Handler.Abstract {
    final Logger logger = LoggerFactory.getLogger(WellKnownHandler.class);
    public static final String HANDLER_CONTEXT_PATH = "/.well-known";
    public static final String HANDLER_ENDPOINT = "/jmap";

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (!(request.getMethod().equals("GET")
                && Request.getPathInContext(request).equals(HANDLER_ENDPOINT))) {
            return false;
        }

        logger.info("HTTP GET request to the endpoint /.well-known/jmap");
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=UTF-8");

        RequestResponse jmapResponse =
                JmapSingleton.INSTANCE
                        .getJmap()
                        .requestSession(request.getHeaders().get(HttpHeader.AUTHORIZATION));
        response.setStatus(jmapResponse.responseCode());

        Content.Sink.write(response, true, jmapResponse.payload(), callback);
        return true;
    }
}
