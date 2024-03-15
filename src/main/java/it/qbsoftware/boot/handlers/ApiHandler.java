package it.qbsoftware.boot.handlers;

import com.google.inject.Inject;
import it.qbsoftware.core.Jmap;
import it.qbsoftware.core.util.RequestResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiHandler extends Handler.Abstract {
    final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    public static final String HANDLER_ENDPOINT = "/api";
    public static final String HANDLER_CONTEXT_PATH = "/";
    private static Jmap jmap;

    @Inject
    public ApiHandler(Jmap jmap) {
        ApiHandler.jmap = jmap;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (!(request.getMethod().equals("POST")
                && Request.getPathInContext(request).equals(HANDLER_ENDPOINT))) {
            return false;
        }

        logger.info("Richiesta HTTP POST all'endpoint /api");
        String requestPayload = Content.Source.asString(request);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=UTF-8");

        RequestResponse jmapResponse = jmap.request(requestPayload);
        response.setStatus(jmapResponse.responseCode());

        Content.Sink.write(response, true, jmapResponse.payload(), callback);
        return true;
    }
}
