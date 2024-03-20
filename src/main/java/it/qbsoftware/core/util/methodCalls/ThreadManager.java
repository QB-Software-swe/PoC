package it.qbsoftware.core.util.methodCalls;

import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import it.qbsoftware.core.util.Updates;
import it.qbsoftware.persistence.EmailDao;
import it.qbsoftware.persistence.EmailImp;
import it.qbsoftware.persistence.MongoConnection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.Response.Invocation;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidResultReferenceMethodErrorResponse;
import rs.ltt.jmap.common.method.response.thread.ChangesThreadMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.mock.server.Changes;
import rs.ltt.jmap.mock.server.ResultReferenceResolver;
import rs.ltt.jmap.mock.server.Update;

public class ThreadManager {
    MongoConnection db;
    Updates updates;

    @Inject
    public ThreadManager(MongoConnection db, Updates updates) {
        this.db = db;
        this.updates = updates;
    }

    public MethodResponse[] get(
            GetThreadMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = methodCall.getIdsReference();
        final List<String> ids;
        if (idsReference != null) {
            try {
                ids =
                        Arrays.asList(
                                ResultReferenceResolver.resolve(idsReference, previousResponses));
            } catch (final IllegalArgumentException e) {
                return new MethodResponse[] {new InvalidResultReferenceMethodErrorResponse()};
            }
        } else {
            ids = Arrays.asList(methodCall.getIds());
        }
        Map<String, Email> emails = new HashMap<String, Email>();
        EmailDao emailDao = new EmailImp(db);
        for (Email email : emailDao.getAll()) {
            emails.put(email.getId(), email);
        }
        final Thread[] threads =
                ids.stream()
                        .map(
                                threadId ->
                                        Thread.builder()
                                                .id(threadId)
                                                .emailIds(
                                                        emails.values().stream()
                                                                .filter(
                                                                        email ->
                                                                                email.getThreadId()
                                                                                        .equals(
                                                                                                threadId))
                                                                .sorted(
                                                                        Comparator.comparing(
                                                                                Email
                                                                                        ::getReceivedAt))
                                                                .map(Email::getId)
                                                                .collect(Collectors.toList()))
                                                .build())
                        .toArray(Thread[]::new);
        return new MethodResponse[] {
            GetThreadMethodResponse.builder().list(threads).state(db.getState()).build()
        };
    }

    public MethodResponse[] changes(
            ChangesThreadMethodCall changesThreadMethodCall,
            ListMultimap<String, Invocation> previousResponses) {
        final String since = changesThreadMethodCall.getSinceState();
        if (since != null && since.equals(db.getState())) {
            return new MethodResponse[] {
                ChangesThreadMethodResponse.builder()
                        .oldState(db.getState())
                        .newState(db.getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .build()
            };
        } else {
            final Update update = updates.getAccumulatedUpdateSince(since);
            if (update == null) {
                return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Thread.class);
                return new MethodResponse[] {
                    ChangesThreadMethodResponse.builder()
                            .oldState(since)
                            .newState(update.getNewVersion())
                            .updated(changes == null ? new String[0] : changes.updated)
                            .created(changes == null ? new String[0] : changes.created)
                            .destroyed(new String[0])
                            .hasMoreChanges(!update.getNewVersion().equals(db.getState()))
                            .build()
                };
            }
        }
    }
}
