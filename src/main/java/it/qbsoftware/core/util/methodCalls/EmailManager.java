package it.qbsoftware.core.util.methodCalls;

import com.google.common.base.Splitter;
import com.google.common.collect.ListMultimap;
import it.qbsoftware.core.util.MailboxInfo;
import it.qbsoftware.core.util.Updates;
import it.qbsoftware.persistence.EmailDao;
import it.qbsoftware.persistence.EmailImp;
import it.qbsoftware.persistence.MailboxInfoImp;
import it.qbsoftware.persistence.MongoConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.Response.Invocation;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.EmailBodyValue;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.entity.SetErrorType;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.email.ChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.common.method.error.AnchorNotFoundMethodErrorResponse;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidResultReferenceMethodErrorResponse;
import rs.ltt.jmap.common.method.error.StateMismatchMethodErrorResponse;
import rs.ltt.jmap.common.method.response.email.ChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse;
import rs.ltt.jmap.mock.server.Changes;
import rs.ltt.jmap.mock.server.CreationIdResolver;
import rs.ltt.jmap.mock.server.ResultReferenceResolver;
import rs.ltt.jmap.mock.server.Update;

public class EmailManager {
    MongoConnection db;
    Updates updates;

    EmailManager(MongoConnection db, Updates updates) {
        this.db = db;
        this.updates = updates;
    }

    public MethodResponse[] queryChanges(
            QueryChangesEmailMethodCall queryChangesEmailMethodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = queryChangesEmailMethodCall.getSinceQueryState();
        if (since != null && since.equals(db.getState())) {
            return new MethodResponse[] {
                QueryChangesEmailMethodResponse.builder()
                        .oldQueryState(db.getState())
                        .newQueryState(db.getState())
                        .added(Collections.emptyList())
                        .removed(new String[0])
                        .build()
            };
        } else {
            return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
        }
    }

    public MethodResponse[] get(
            GetEmailMethodCall getEmailMethodCall,
            ListMultimap<String, Invocation> previousResponses) {
        final Request.Invocation.ResultReference idsReference =
                getEmailMethodCall.getIdsReference();
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
            ids = Arrays.asList(getEmailMethodCall.getIds());
        }
        EmailDao emailDao = new EmailImp(db);
        Map<String, Email> emails = new HashMap<String, Email>();
        for (Email email : emailDao.getAll()) {
            emails.put(email.getId(), email);
        }
        final String[] properties = getEmailMethodCall.getProperties();
        Stream<Email> emailStream = emails.values().stream();
        if (Arrays.equals(properties, Email.Properties.THREAD_ID)) {
            emailStream =
                    emailStream.map(
                            email ->
                                    Email.builder()
                                            .id(email.getId())
                                            .threadId(email.getThreadId())
                                            .build());
        } else if (Arrays.equals(properties, Email.Properties.MUTABLE)) {
            emailStream =
                    emailStream.map(
                            email ->
                                    Email.builder()
                                            .id(email.getId())
                                            .keywords(email.getKeywords())
                                            .mailboxIds(email.getMailboxIds())
                                            .build());
        }
        return new MethodResponse[] {
            GetEmailMethodResponse.builder()
                    .list(emailStream.toArray(Email[]::new))
                    .state(db.getState())
                    .build()
        };
    }

    public MethodResponse[] query(
            QueryEmailMethodCall queryEmailMethodCall,
            ListMultimap<String, Invocation> previousResponses) {
        final Filter<Email> filter = queryEmailMethodCall.getFilter();
        EmailDao emailDao = new EmailImp(db);
        Map<String, Email> emails = new HashMap<String, Email>();
        for (Email email : emailDao.getAll()) {
            emails.put(email.getId(), email);
        }
        Stream<Email> emailStream = emails.values().stream();
        emailStream = applyFilter(filter, emailStream);
        emailStream = emailStream.sorted(Comparator.comparing(Email::getReceivedAt).reversed());

        if (Boolean.TRUE.equals(queryEmailMethodCall.getCollapseThreads())) {
            emailStream = emailStream.filter(distinctByKey(Email::getThreadId));
        }

        final List<String> ids = emailStream.map(Email::getId).collect(Collectors.toList());
        final String anchor = queryEmailMethodCall.getAnchor();
        final int position;
        if (anchor != null) {
            final Long anchorOffset = queryEmailMethodCall.getAnchorOffset();
            final int anchorPosition = ids.indexOf(anchor);
            if (anchorPosition == -1) {
                return new MethodResponse[] {new AnchorNotFoundMethodErrorResponse()};
            }
            position = Math.toIntExact(anchorPosition + (anchorOffset == null ? 0 : anchorOffset));
        } else {
            position =
                    Math.toIntExact(
                            queryEmailMethodCall.getPosition() == null
                                    ? 0
                                    : queryEmailMethodCall.getPosition());
        }
        final int limit =
                Math.toIntExact(
                        queryEmailMethodCall.getLimit() == null
                                ? 40
                                : queryEmailMethodCall.getLimit());
        final int endPosition = Math.min(position + limit, ids.size());
        final String[] page = ids.subList(position, endPosition).toArray(new String[0]);
        final Long total =
                Boolean.TRUE.equals(queryEmailMethodCall.getCalculateTotal())
                        ? (long) ids.size()
                        : null;

        return new MethodResponse[] {
            QueryEmailMethodResponse.builder()
                    .canCalculateChanges(false) // Non implementato
                    .queryState(db.getState())
                    .total(total)
                    .ids(page)
                    .position((long) position)
                    .build()
        };
    }

    public MethodResponse[] changes(
            ChangesEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals("0")) {
            return new MethodResponse[] {
                ChangesEmailMethodResponse.builder()
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
                final Changes changes = update.getChangesFor(Email.class);
                return new MethodResponse[] {
                    ChangesEmailMethodResponse.builder()
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

    public MethodResponse[] set(
            SetEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        EmailDao emailDao = new EmailImp(db);
        Map<String, Email> emails = new HashMap<String, Email>();
        for (Email email : emailDao.getAll()) {
            emails.put(email.getId(), email);
        }
        final String ifInState = methodCall.getIfInState();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final Map<String, Email> create = methodCall.getCreate();
        final String[] destroy = methodCall.getDestroy();
        if (destroy != null && destroy.length > 0) {
            throw new IllegalStateException("");
        }
        final SetEmailMethodResponse.SetEmailMethodResponseBuilder responseBuilder =
                SetEmailMethodResponse.builder();
        final String oldState = db.getState();
        if (ifInState != null) {
            if (!ifInState.equals(oldState)) {
                return new MethodResponse[] {new StateMismatchMethodErrorResponse()};
            }
        }
        if (update != null) {
            final List<Email> modifiedEmails = new ArrayList<>();
            for (final Map.Entry<String, Map<String, Object>> entry : update.entrySet()) {
                final String id = entry.getKey();
                try {
                    final Email modifiedEmail = patchEmail(id, entry.getValue(), previousResponses);
                    modifiedEmails.add(modifiedEmail);
                    responseBuilder.updated(id, modifiedEmail);
                } catch (final IllegalArgumentException e) {
                    responseBuilder.notUpdated(
                            id, new SetError(SetErrorType.INVALID_PROPERTIES, e.getMessage()));
                }
            }
            for (final Email email : modifiedEmails) {
                emails.put(email.getId(), email);
            }
            db.increaseState();
            final String newState = db.getState();
            HashMap<String, MailboxInfo> mInfo = new HashMap<>();
            for (var x : new MailboxInfoImp(db).readAll()) {
                mInfo.put(x.getId(), x);
            }

            updates.getUpdates()
                    .put(oldState, Update.updated(modifiedEmails, mInfo.keySet(), newState));
        }
        if (create != null && create.size() > 0) {
            processCreateEmail(create, responseBuilder, previousResponses);
        }
        return new MethodResponse[] {responseBuilder.build()};
    }

    private Email patchEmail(
            final String id,
            final Map<String, Object> patches,
            ListMultimap<String, Response.Invocation> previousResponses) {
        EmailDao emailDao = new EmailImp(db);
        Map<String, Email> emails = new HashMap<String, Email>();
        for (Email email : emailDao.getAll()) {
            emails.put(email.getId(), email);
        }
        final Email.EmailBuilder emailBuilder = emails.get(id).toBuilder();
        for (final Map.Entry<String, Object> patch : patches.entrySet()) {
            final String fullPath = patch.getKey();
            final Object modification = patch.getValue();
            final List<String> pathParts = Splitter.on('/').splitToList(fullPath);
            final String parameter = pathParts.get(0);
            if (parameter.equals("keywords")) {
                if (pathParts.size() == 2 && modification instanceof Boolean) {
                    final String keyword = pathParts.get(1);
                    final Boolean value = (Boolean) modification;
                    emailBuilder.keyword(keyword, value);
                } else {
                    throw new IllegalArgumentException("");
                }
            } else if (parameter.equals("mailboxIds")) {
                if (pathParts.size() == 2 && modification instanceof Boolean) {
                    final String mailboxId = pathParts.get(1);
                    final Boolean value = (Boolean) modification;
                    emailBuilder.mailboxId(mailboxId, value);
                } else if (modification instanceof Map) {
                    final Map<String, Boolean> mailboxMap = (Map<String, Boolean>) modification;
                    emailBuilder.clearMailboxIds();
                    for (Map.Entry<String, Boolean> mailboxEntry : mailboxMap.entrySet()) {
                        final String mailboxId =
                                CreationIdResolver.resolveIfNecessary(
                                        mailboxEntry.getKey(), previousResponses);
                        emailBuilder.mailboxId(mailboxId, mailboxEntry.getValue());
                    }
                } else {
                    throw new IllegalArgumentException("No path " + fullPath);
                }
            } else {
                throw new IllegalArgumentException("No path " + fullPath);
            }
        }
        return emailBuilder.build();
    }

    private void processCreateEmail(
            Map<String, Email> create,
            SetEmailMethodResponse.SetEmailMethodResponseBuilder responseBuilder,
            ListMultimap<String, Response.Invocation> previousResponses) {
        for (final Map.Entry<String, Email> entry : create.entrySet()) {
            final String createId = entry.getKey();
            final String id = UUID.randomUUID().toString();
            final String threadId = UUID.randomUUID().toString();
            final Email userSuppliedEmail = entry.getValue();
            final Map<String, Boolean> mailboxMap = userSuppliedEmail.getMailboxIds();
            final Email.EmailBuilder emailBuilder =
                    userSuppliedEmail.toBuilder()
                            .id(id)
                            .threadId(threadId)
                            .receivedAt(Instant.now());
            emailBuilder.clearMailboxIds();
            for (Map.Entry<String, Boolean> mailboxEntry : mailboxMap.entrySet()) {
                final String mailboxId =
                        CreationIdResolver.resolveIfNecessary(
                                mailboxEntry.getKey(), previousResponses);
                emailBuilder.mailboxId(mailboxId, mailboxEntry.getValue());
            }
            final List<EmailBodyPart> attachments = userSuppliedEmail.getAttachments();
            emailBuilder.clearAttachments();
            if (attachments != null) {
                for (final EmailBodyPart attachment : attachments) {
                    final String partId = attachment.getPartId();
                    final EmailBodyValue value =
                            partId == null ? null : userSuppliedEmail.getBodyValues().get(partId);
                    if (value != null) {
                        final EmailBodyPart emailBodyPart = injectId(attachment);
                        emailBuilder.attachment(emailBodyPart);
                    } else {
                        emailBuilder.attachment(attachment);
                    }
                }
            }
            final Email email = emailBuilder.build();

            createEmail(email);
            responseBuilder.created(createId, email);
        }
    }

    private static EmailBodyPart injectId(final Attachment attachment) {
        return EmailBodyPart.builder()
                .blobId(UUID.randomUUID().toString())
                .charset(attachment.getCharset())
                .type(attachment.getType())
                .name(attachment.getName())
                .size(attachment.getSize())
                .build();
    }

    private void createEmail(final Email email) {
        EmailDao emailDao = new EmailImp(db);
        emailDao.write(email);
        db.increaseState();
    }

    private static Stream<Email> applyFilter(
            final Filter<Email> filter, Stream<Email> emailStream) {
        if (filter instanceof EmailFilterCondition) {
            final EmailFilterCondition emailFilterCondition = (EmailFilterCondition) filter;
            final String inMailbox = emailFilterCondition.getInMailbox();
            if (inMailbox != null) {
                emailStream =
                        emailStream.filter(email -> email.getMailboxIds().containsKey(inMailbox));
            }
            final String[] header = emailFilterCondition.getHeader();
            if (header != null
                    && header.length == 2
                    && header[0].equals("Autocrypt-Setup-Message")) {
                emailStream =
                        emailStream.filter(
                                email -> header[1].equals(email.getAutocryptSetupMessage()));
            }
        }
        return emailStream;
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
