package it.qbsoftware.core.util;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

import it.qbsoftware.core.util.methodCalls.EchoManager;
import it.qbsoftware.core.util.methodCalls.EmailManager;
import it.qbsoftware.core.util.methodCalls.IdentityManager;
import it.qbsoftware.core.util.methodCalls.MailboxManager;
import it.qbsoftware.core.util.methodCalls.SubmissionManager;
import it.qbsoftware.core.util.methodCalls.ThreadManager;
import it.qbsoftware.persistence.MongoConnection;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.call.email.ChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.submission.GetEmailSubmissionMethodCall;
import rs.ltt.jmap.common.method.call.submission.SetEmailSubmissionMethodCall;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.gson.JmapAdapters;

public class Dispatcher {
    final Logger logger = LoggerFactory.getLogger(Dispatcher.class);
    static final Gson GSON;
    static MongoConnection db;
    static EchoManager echoM;
    static EmailManager emailM;
    static IdentityManager identityM;
    static MailboxManager mailboxM;
    static SubmissionManager submissionM;
    static ThreadManager threadM;

    static {
        GsonBuilder builder = new GsonBuilder();
        JmapAdapters.register(builder);
        GSON = builder.create();
    }

    @Inject
    public Dispatcher(MongoConnection db, EchoManager echoManager, EmailManager emailManager, IdentityManager identityManager, MailboxManager mailboxManager, SubmissionManager submissionManager, ThreadManager threadManager) {
        Dispatcher.db = db;
        echoM = echoManager;
        emailM = emailManager;
        identityM = identityManager;
        mailboxM = mailboxManager;
        submissionM = submissionManager;
        threadM = threadManager;
    }

    @SuppressWarnings("null")
    public GenericResponse computeResponse(Request jmapRequest) {
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

    private MethodResponse[] dispatch(final MethodCall methodCall, final ListMultimap<String, Response.Invocation> previousResponses) {
        return switch(methodCall) {
            case EchoMethodCall call -> {
                yield echoM.echo(call, previousResponses);
            }
            case GetIdentityMethodCall call -> {
                yield identityM.get(call, previousResponses);
            }
            case GetEmailMethodCall call -> {
                yield emailM.get(call, previousResponses);
            }
            case ChangesEmailMethodCall call -> {
                yield emailM.changes(call, previousResponses);
            }
            case QueryEmailMethodCall call -> {
                yield emailM.query(call, previousResponses);
            }
            case SetEmailMethodCall call -> {
                yield emailM.set(call, previousResponses);
            }
            case SetEmailSubmissionMethodCall call -> {
                yield submissionM.set(call, previousResponses);
            }
            case GetEmailSubmissionMethodCall call -> {
                yield submissionM.get(call, previousResponses);
            }
            case GetMailboxMethodCall call -> {
                yield mailboxM.get(call, previousResponses);
            }
            case ChangesMailboxMethodCall call -> {
                yield mailboxM.changes(call, previousResponses);
            }
            case SetMailboxMethodCall call -> {
                yield mailboxM.set(call, previousResponses);
            }
            case GetThreadMethodCall call -> {
                yield threadM.get(call, previousResponses);
            }
            case ChangesThreadMethodCall call -> {
                yield threadM.changes(call, previousResponses);
            }
            default -> {
                yield new MethodResponse[] {new UnknownMethodMethodErrorResponse()};
            }
        }
    }
    
}
