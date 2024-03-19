package it.qbsoftware.core.util.methodCalls;

import com.google.common.collect.ListMultimap;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;

public class EchoManager {
    public MethodResponse[] echo(
            EchoMethodCall call, ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[] {
            EchoMethodResponse.builder().libraryName(call.getLibraryName()).build()
        };
    }
}
