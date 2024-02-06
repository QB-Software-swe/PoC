package it.qbsoftware.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.qbsoftware.core.utils.RequestResponse;
import java.util.Arrays;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;
import rs.ltt.jmap.gson.JmapAdapters;

public class Jmap {
  private static final Gson gson;

  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    JmapAdapters.register(gsonBuilder);
    gson = gsonBuilder.create();
  }

  // * TMP */
  JmapSession jmapSession = new JmapSession();

  // * --- */

  // -------------------------
  public RequestResponse requestSession(String authentication) {
    /*
     * TODO: implement authentication logic (check only if name exist)
     */

    return new RequestResponse(gson.toJson(jmapSession.sessionResources()), 200);
  }

  public RequestResponse request(String JsonRequestJmap) {
    Request request = null;

    // FIXME: find a better solution
    try {
      request = gson.fromJson(JsonRequestJmap, Request.class);
    } catch (Exception e) {
      request = null;
    }

    if (request != null) {
      return new RequestResponse(gson.toJson(computeResponse(request)), 200);
    } else {
      return new RequestResponse("", 500);
    }
  }

  // -------------------------

  @SuppressWarnings("null")
  private GenericResponse computeResponse(Request jmapRequest) {
    final Request.Invocation[] methodCalls = jmapRequest.getMethodCalls();

    // FIXME: something is missing here

    final ArrayListMultimap<String, rs.ltt.jmap.common.Response.Invocation> response =
        ArrayListMultimap.create();

    for (final Request.Invocation invocation : methodCalls) {
      final MethodCall methodCall = invocation.getMethodCall();
      final String id = invocation.getId();

      MethodResponse[] methodResponses =
          dispatch(methodCall, ImmutableListMultimap.copyOf(response));

      Arrays.stream(methodResponses)
          .sequential()
          .forEach(r -> response.put(id, new Response.Invocation(r, id)));
    }

    // FIXME: session must change for eache session state change
    return new Response(response.values().toArray(new Response.Invocation[0]), "0");
  }

  private MethodResponse[] dispatch(
      final MethodCall methodCall,
      final ListMultimap<String, Response.Invocation> previousResponses) {
    return switch (methodCall) {
      case EchoMethodCall echoCall -> {
        yield execute(echoCall, previousResponses);
      }

      default -> {
        yield new MethodResponse[] {new UnknownMethodMethodErrorResponse()};
      }
    };
  }

  private MethodResponse[] execute(
      EchoMethodCall methodCall, ListMultimap<String, Response.Invocation> previousResponses) {
    return new MethodResponse[] {
      EchoMethodResponse.builder().libraryName(methodCall.getLibraryName()).build()
    };
  }
}
