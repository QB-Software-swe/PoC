package it.qbsoftware.core.util.methodCalls;

import com.google.common.base.Splitter;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import it.qbsoftware.core.util.MailboxInfo;
import it.qbsoftware.core.util.Updates;
import it.qbsoftware.persistence.MailboxInfoDao;
import it.qbsoftware.persistence.MailboxInfoImp;
import it.qbsoftware.persistence.MongoConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.entity.SetErrorType;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.QueryMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidResultReferenceMethodErrorResponse;
import rs.ltt.jmap.common.method.error.StateMismatchMethodErrorResponse;
import rs.ltt.jmap.common.method.response.mailbox.ChangesMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.QueryMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.SetMailboxMethodResponse;
import rs.ltt.jmap.mock.server.Changes;
import rs.ltt.jmap.mock.server.ResultReferenceResolver;
import rs.ltt.jmap.mock.server.Update;
import rs.ltt.jmap.mock.server.util.FuzzyRoleParser;

public class MailboxManager {
    final MongoConnection db;
    final Updates updates;

    @Inject
    public MailboxManager(MongoConnection db, Updates updates) {
        this.db = db;
        this.updates = updates;
    }

    public MethodResponse[] get(
            GetMailboxMethodCall call,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference = call.getIdsReference();
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
            final String[] idsParameter = call.getIds();
            ids = idsParameter == null ? null : Arrays.asList(idsParameter);
        }

        MailboxInfoDao mailboxInfo = new MailboxInfoImp(db);
        ArrayList<MailboxInfo> mailboxes = mailboxInfo.readAll();

        return new MethodResponse[] {
            GetMailboxMethodResponse.builder()
                    .list(
                            mailboxes.stream()
                                    .map(this::toMailbox)
                                    .filter(m -> ids == null || ids.contains(m.getId()))
                                    .toArray(Mailbox[]::new))
                    .state(db.getState())
                    .build()
        };
    }

    public MethodResponse[] changes(
            ChangesMailboxMethodCall call,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = call.getSinceState();
        if (since != null && since.equals(db.getState())) {
            return new MethodResponse[] {
                ChangesMailboxMethodResponse.builder()
                        .oldState(db.getState())
                        .newState(db.getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .updatedProperties(new String[0])
                        .build()
            };
        } else {
            final Update update = updates.getAccumulatedUpdateSince(since);
            if (update == null) {
                return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
            } else {
                final Changes changes = update.getChangesFor(Mailbox.class);
                return new MethodResponse[] {
                    ChangesMailboxMethodResponse.builder()
                            .oldState(since)
                            .newState(update.getNewVersion())
                            .updated(changes.updated)
                            .created(changes.created)
                            .destroyed(new String[0])
                            .hasMoreChanges(!update.getNewVersion().equals("0"))
                            .build()
                };
            }
        }
    }

    public MethodResponse[] query(
            final QueryMailboxMethodCall queryMailboxMethodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {

        return new MethodResponse[] {
            QueryMailboxMethodResponse.builder()
                    .accountId("0")
                    .canCalculateChanges(false)
                    .ids(new String[] {"3"})
                    .build()
        };
    }

    public MethodResponse[] set(
            final SetMailboxMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String ifInState = methodCall.getIfInState();
        final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder =
                SetMailboxMethodResponse.builder();
        final Map<String, Mailbox> create = methodCall.getCreate();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final String oldState = db.getState();
        if (ifInState != null) {
            if (!ifInState.equals(oldState)) {
                return new MethodResponse[] {new StateMismatchMethodErrorResponse()};
            }
        }

        if (create != null && create.size() > 0) {
            processCreateMailbox(create, responseBuilder);
        }
        if (update != null && update.size() > 0) {
            processUpdateMailbox(update, responseBuilder, previousResponses);
        }
        db.increaseState();
        final SetMailboxMethodResponse setMailboxResponse = responseBuilder.build();
        updates.getUpdates().put(oldState, Update.of(setMailboxResponse, db.getState()));
        return new MethodResponse[] {setMailboxResponse};
    }

    private Mailbox toMailbox(MailboxInfo mailboxInfo) {
        // TODO: implementare conteggio e-mail
        return Mailbox.builder()
                .id(mailboxInfo.getId())
                .name(mailboxInfo.getName())
                .role(mailboxInfo.getRole())
                .totalEmails(null)
                .totalThreads(null)
                .unreadEmails(null)
                .unreadThreads(null)
                .build();
    }

    private void processCreateMailbox(
            final Map<String, Mailbox> create,
            final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder) {
        for (Map.Entry<String, Mailbox> entry : create.entrySet()) {
            final String createId = entry.getKey();
            final Mailbox mailbox = entry.getValue();
            final String name = mailbox.getName();
            if (new MailboxInfoImp(db)
                    .readAll().stream()
                            .anyMatch(mailboxInfo -> mailboxInfo.getName().equals(name))) {
                responseBuilder.notCreated(
                        createId,
                        new SetError(SetErrorType.INVALID_PROPERTIES, "Esiste già " + name));
                continue;
            }
            final String id = UUID.randomUUID().toString();
            final MailboxInfo mailboxInfo = new MailboxInfo(id, name, mailbox.getRole());
            new MailboxInfoImp(db).write(mailboxInfo);
            responseBuilder.created(createId, toMailbox(mailboxInfo));
        }
    }

    private MailboxInfo patchMailbox(
            final String id,
            final Map<String, Object> patches,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final MailboxInfo currentMailbox =
                new MailboxInfoImp(db).read(id).get(); // FIXME: Questo è un Optional
        for (final Map.Entry<String, Object> patch : patches.entrySet()) {
            final String fullPath = patch.getKey();
            final Object modification = patch.getValue();
            final List<String> pathParts = Splitter.on('/').splitToList(fullPath);
            final String parameter = pathParts.get(0);
            if ("role".equals(parameter)) {
                final Role role = FuzzyRoleParser.parse((String) modification);
                return new MailboxInfo(currentMailbox.getId(), currentMailbox.getName(), role);
            } else {
                throw new IllegalArgumentException("No path " + fullPath);
            }
        }
        return currentMailbox;
    }

    private void processUpdateMailbox(
            Map<String, Map<String, Object>> update,
            SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder,
            ListMultimap<String, Response.Invocation> previousResponses) {
        for (final Map.Entry<String, Map<String, Object>> entry : update.entrySet()) {
            final String id = entry.getKey();
            try {
                final MailboxInfo modifiedMailbox =
                        patchMailbox(id, entry.getValue(), previousResponses);
                responseBuilder.updated(id, toMailbox(modifiedMailbox));
                new MailboxInfoImp(db).write(modifiedMailbox);
            } catch (final IllegalArgumentException e) {
                responseBuilder.notUpdated(
                        id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
            }
        }
    }
}
