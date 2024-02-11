package it.qbsoftware.core;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.qbsoftware.core.util.JmapSession;
import it.qbsoftware.core.util.MailboxInfo;
import it.qbsoftware.core.util.RequestResponse;
import it.qbsoftware.persistence.EmailDao;
import it.qbsoftware.persistence.EmailImp;
import it.qbsoftware.persistence.IdentityDao;
import it.qbsoftware.persistence.IdentityImp;
import it.qbsoftware.persistence.MailboxInfoDao;
import it.qbsoftware.persistence.MailboxInfoImp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.joda.time.DateTime;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.entity.Keyword;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidResultReferenceMethodErrorResponse;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.gson.JmapAdapters;
import rs.ltt.jmap.mock.server.ResultReferenceResolver;

public class Jmap {
  static final Gson GSON;

  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    JmapAdapters.register(gsonBuilder);
    GSON = gsonBuilder.create();
  }

  public RequestResponse requestSession(String authentication) {
    JmapSession jmapSession = new JmapSession();
    return new RequestResponse(GSON.toJson(jmapSession.sessionResources()), 200);
  }

  public RequestResponse request(String JsonRequestJmap) {
    System.out.println(
        "\n---------\n" + DateTime.now().toString() + " - Richiesta:\n" + JsonRequestJmap);
    Request request = null;

    try {
      request = GSON.fromJson(JsonRequestJmap, Request.class);
    } catch (Exception e) {
      System.out.println("\n\n" + DateTime.now().toString() + " - Risposta ERRORE!!!");
      request = null;
    }

    if (request != null) {
      String responseJson = GSON.toJson(computeResponse(request));
      System.out.println("\n\n" + DateTime.now().toString() + " - Risposta:\n" + responseJson);
      return new RequestResponse(responseJson, 200);
    } else {
      System.out.println("\n\n" + DateTime.now().toString() + " - Risposta ERRORE500!!!");
      return new RequestResponse("", 500);
    }
  }

  @SuppressWarnings("null")
  private GenericResponse computeResponse(Request jmapRequest) {
    final Request.Invocation[] methodCalls = jmapRequest.getMethodCalls();

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

    return new Response(response.values().toArray(new Response.Invocation[0]), "0");
  }

  private MethodResponse[] dispatch(
      final MethodCall methodCall,
      final ListMultimap<String, Response.Invocation> previousResponses) {
    return switch (methodCall) {
      case EchoMethodCall echoCall -> {
        yield execute(echoCall, previousResponses);
      }

      case GetIdentityMethodCall getIdentityMethodCall -> {
        yield execute(getIdentityMethodCall, previousResponses);
      }

      case GetMailboxMethodCall getMailboxMethodCall -> {
        yield execute(getMailboxMethodCall, previousResponses);
      }

      case QueryChangesEmailMethodCall queryChangesEmailMethodCall -> {
        yield execute(queryChangesEmailMethodCall, previousResponses);
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

  private MethodResponse[] execute(
      GetIdentityMethodCall getIdentityMethodCall,
      ListMultimap<String, Response.Invocation> previousResponses) {

    IdentityDao identityDao = new IdentityImp();

    return new MethodResponse[] {
      GetIdentityMethodResponse.builder()
          .list(
              new Identity[] {identityDao.getIdentity(getIdentityMethodCall.getAccountId()).get()})
          .build()
    };
  }

  private MethodResponse[] execute(
      GetMailboxMethodCall getMailboxMethodCall,
      ListMultimap<String, Response.Invocation> previousResponses) {

    final Request.Invocation.ResultReference idsReference = getMailboxMethodCall.getIdsReference();
    final List<String> ids;

    if (idsReference != null) {
      try {
        ids = Arrays.asList(ResultReferenceResolver.resolve(idsReference, previousResponses));
      } catch (final IllegalArgumentException e) {
        return new MethodResponse[] {new InvalidResultReferenceMethodErrorResponse()};
      }
    } else {
      final String[] idsParameter = getMailboxMethodCall.getIds();
      ids = idsParameter == null ? null : Arrays.asList(idsParameter);
    }

    MailboxInfoDao mailboxInfoDao = new MailboxInfoImp();
    ArrayList<MailboxInfo> mailboxs = mailboxInfoDao.getMailboxsInfo();

    return new MethodResponse[] {
      GetMailboxMethodResponse.builder()
          .list(
              mailboxs.stream()
                  .map(this::toMailbox)
                  .filter(m -> ids == null || ids.contains(m.getId()))
                  .toArray(Mailbox[]::new))
          .state("0") // FIXME: Get STATE
          .build()
    };
  }

  private MethodResponse[] execute(
      QueryChangesEmailMethodCall queryChangesEmailMethodCall,
      ListMultimap<String, Response.Invocation> previousResponses) {
    final String since = queryChangesEmailMethodCall.getSinceQueryState();
    if (since != null && since.equals("0")) {
      return new MethodResponse[] {
        QueryChangesEmailMethodResponse.builder()
            .oldQueryState("0")
            .newQueryState("0")
            .added(Collections.emptyList())
            .removed(new String[0])
            .build()
      };
    } else {
      return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
    }
  }

  private Mailbox toMailbox(MailboxInfo mailboxInfo) {
    EmailDao emailDao = new EmailImp();
    ArrayList<Email> emailsInMailbox = emailDao.getEmailsInMailboxs(mailboxInfo.getId());

    // TODO: implementare conteggio e-mail
    return Mailbox.builder()
        .id(mailboxInfo.getId())
        .name(mailboxInfo.getName())
        .role(mailboxInfo.getRole())
        .totalEmails(emailsInMailbox.stream().count())
        .totalThreads(0L)
        .unreadEmails(
            emailsInMailbox.stream()
                .filter(e -> e.getKeywords() != null)
                .filter(e -> !e.getKeywords().containsKey(Keyword.SEEN))
                .count())
        .unreadThreads(0L)
        .build();
  }
}
