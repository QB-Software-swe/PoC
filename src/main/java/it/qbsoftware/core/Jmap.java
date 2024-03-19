package it.qbsoftware.core;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import it.qbsoftware.core.util.JmapSession;
import it.qbsoftware.core.util.MailboxInfo;
import it.qbsoftware.core.util.RequestResponse;
import it.qbsoftware.persistence.EmailDao;
import it.qbsoftware.persistence.EmailImp;
import it.qbsoftware.persistence.IdentityDao;
import it.qbsoftware.persistence.IdentityImp;
import it.qbsoftware.persistence.MailboxInfoDao;
import it.qbsoftware.persistence.MailboxInfoImp;
import it.qbsoftware.persistence.MongoConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.GenericResponse;
import rs.ltt.jmap.common.Request;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.Response.Invocation;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.EmailBodyValue;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.entity.Mailbox;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.SetError;
import rs.ltt.jmap.common.entity.SetErrorType;
import rs.ltt.jmap.common.entity.Thread;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.filter.Filter;
import rs.ltt.jmap.common.method.MethodCall;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.core.EchoMethodCall;
import rs.ltt.jmap.common.method.call.email.ChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.GetEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryChangesEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.QueryEmailMethodCall;
import rs.ltt.jmap.common.method.call.email.SetEmailMethodCall;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.ChangesMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.GetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.QueryMailboxMethodCall;
import rs.ltt.jmap.common.method.call.mailbox.SetMailboxMethodCall;
import rs.ltt.jmap.common.method.call.thread.ChangesThreadMethodCall;
import rs.ltt.jmap.common.method.call.thread.GetThreadMethodCall;
import rs.ltt.jmap.common.method.error.AnchorNotFoundMethodErrorResponse;
import rs.ltt.jmap.common.method.error.CannotCalculateChangesMethodErrorResponse;
import rs.ltt.jmap.common.method.error.InvalidResultReferenceMethodErrorResponse;
import rs.ltt.jmap.common.method.error.StateMismatchMethodErrorResponse;
import rs.ltt.jmap.common.method.error.UnknownMethodMethodErrorResponse;
import rs.ltt.jmap.common.method.response.core.EchoMethodResponse;
import rs.ltt.jmap.common.method.response.email.ChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.GetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryChangesEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.QueryEmailMethodResponse;
import rs.ltt.jmap.common.method.response.email.SetEmailMethodResponse;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.ChangesMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.GetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.QueryMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.mailbox.SetMailboxMethodResponse;
import rs.ltt.jmap.common.method.response.thread.ChangesThreadMethodResponse;
import rs.ltt.jmap.common.method.response.thread.GetThreadMethodResponse;
import rs.ltt.jmap.gson.JmapAdapters;
import rs.ltt.jmap.mock.server.Changes;
import rs.ltt.jmap.mock.server.CreationIdResolver;
import rs.ltt.jmap.mock.server.ResultReferenceResolver;
import rs.ltt.jmap.mock.server.Update;
import rs.ltt.jmap.mock.server.util.FuzzyRoleParser;

public class Jmap {
    final Logger logger = LoggerFactory.getLogger(Jmap.class);
    static final Gson GSON;
    final LinkedHashMap<String, Update> updates;
    static MongoConnection db;

    @Inject
    public Jmap(MongoConnection db, LinkedHashMap<String, Update> updates) {
        Jmap.db = db;
        Jmap.updates = updates;
    }

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        JmapAdapters.register(gsonBuilder);
        GSON = gsonBuilder.create();
    }

    public RequestResponse requestSession(String authentication) {
        logger.info("Richiesta di autenticazione autorizzata");
        return new RequestResponse(GSON.toJson(new JmapSession().generateSessionResources()), 200);
    }

    public RequestResponse request(String jsonRequestJmap) {
        Request request = null;

        try {
            request = GSON.fromJson(jsonRequestJmap, Request.class);
            logger.info("Richiesta in JSON:\n" + jsonRequestJmap);
        } catch (Exception e) {
            logger.error("Richiesta in JSON non valida:\n" + jsonRequestJmap);
            request = null;
        }

        if (request != null) {
            String responseJson = GSON.toJson(computeResponse(request));
            logger.info("Risposta: " + responseJson);
            return new RequestResponse(responseJson, 200);
        } else {
            logger.error("Risposta:\n error 500");
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
                logger.info("Eseguo method call Echo");
                yield execute(echoCall, previousResponses);
            }

            case GetIdentityMethodCall getIdentityMethodCall -> {
                logger.info("Eseguo method call Identity/Get");
                yield execute(getIdentityMethodCall, previousResponses);
            }

            case GetMailboxMethodCall getMailboxMethodCall -> {
                logger.info("Eseguo method call Mailbox/Get");
                yield execute(getMailboxMethodCall, previousResponses);
            }

            case ChangesMailboxMethodCall changesMailboxMethodCall -> {
                logger.info("Eseguo method call Mailbox/Changes");
                yield execute(changesMailboxMethodCall, previousResponses);
            }

            case QueryChangesEmailMethodCall queryChangesEmailMethodCall -> {
                logger.info("Eseguo method call Email/QueryChanges");
                yield execute(queryChangesEmailMethodCall, previousResponses);
            }

            case QueryMailboxMethodCall queryMailboxMethodCall -> {
                logger.info("Eseguo method call Mailbox/Query");
                yield execute(queryMailboxMethodCall, previousResponses);
            }

            case GetThreadMethodCall getThreadMethodCall -> {
                logger.info("Eseguo method call Thread/Get");
                yield execute(getThreadMethodCall, previousResponses);
            }

            case GetEmailMethodCall getEmailMethodCall -> {
                logger.info("Eseguo method call Email/Get");
                yield execute(getEmailMethodCall, previousResponses);
            }

            case ChangesEmailMethodCall changesEmailMethodCall -> {
                logger.info("Eseguo method call Email/Changes");
                yield execute(changesEmailMethodCall, previousResponses);
            }

            case QueryEmailMethodCall queryEmailMethodCall -> {
                logger.info("Eseguo method call Email/Query");
                yield execute(queryEmailMethodCall, previousResponses);
            }

            case ChangesThreadMethodCall changesThreadMethodCall -> {
                logger.info("Eseguo method call Thread/Changes");
                yield execute(changesThreadMethodCall, previousResponses);
            }

            case SetEmailMethodCall setEmailMethodCall -> {
                logger.info("Eseguo method call Email/Set");
                yield execute(setEmailMethodCall, previousResponses);
            }

            case SetMailboxMethodCall setMailboxMethodCall -> {
                logger.info("Eseguo method call Mailbox/Set");
                yield execute(setMailboxMethodCall, previousResponses);
            }

            default -> {
                logger.info(
                        "Il metodo '"
                                + methodCall.getClass()
                                + "' non è stato riconosciuto/implementato dal server");
                yield new MethodResponse[] {new UnknownMethodMethodErrorResponse()};
            }
        };
    }

    private MethodResponse[] execute(
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

    private MethodResponse[] execute(
            final SetMailboxMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String ifInState = methodCall.getIfInState();
        final SetMailboxMethodResponse.SetMailboxMethodResponseBuilder responseBuilder =
                SetMailboxMethodResponse.builder();
        final Map<String, Mailbox> create = methodCall.getCreate();
        final Map<String, Map<String, Object>> update = methodCall.getUpdate();
        final String oldState = getState();
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
        increaseState();
        final SetMailboxMethodResponse setMailboxResponse = responseBuilder.build();
        updates.put(oldState, Update.of(setMailboxResponse, getState()));
        return new MethodResponse[] {setMailboxResponse};
    }

    private MethodResponse[] execute(
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
        final String oldState = getState();
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
            increaseState();
            final String newState = getState();
            HashMap<String, MailboxInfo> mInfo = new HashMap<>();
            for (var x : new MailboxInfoImp(db).readAll()) {
                mInfo.put(x.getId(), x);
            }

            updates.put(oldState, Update.updated(modifiedEmails, mInfo.keySet(), newState));
        }
        if (create != null && create.size() > 0) {
            processCreateEmail(create, responseBuilder, previousResponses);
        }
        return new MethodResponse[] {responseBuilder.build()};
    }

    private MethodResponse[] execute(
            ChangesEmailMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals("0")) {
            return new MethodResponse[] {
                ChangesEmailMethodResponse.builder()
                        .oldState(getState())
                        .newState(getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .build()
            };
        } else {
            final Update update = getAccumulatedUpdateSince(since);
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
                            .hasMoreChanges(!update.getNewVersion().equals(getState()))
                            .build()
                };
            }
        }
    }

    private MethodResponse[] execute(
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
            GetThreadMethodResponse.builder().list(threads).state(getState()).build()
        };
    }

    private MethodResponse[] execute(
            ChangesThreadMethodCall changesThreadMethodCall,
            ListMultimap<String, Invocation> previousResponses) {
        final String since = changesThreadMethodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                ChangesThreadMethodResponse.builder()
                        .oldState(getState())
                        .newState(getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .build()
            };
        } else {
            final Update update = getAccumulatedUpdateSince(since);
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
                            .hasMoreChanges(!update.getNewVersion().equals(getState()))
                            .build()
                };
            }
        }
    }

    private MethodResponse[] execute(
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
                    .queryState(getState())
                    .total(total)
                    .ids(page)
                    .position((long) position)
                    .build()
        };
    }

    private MethodResponse[] execute(
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
                    .state(getState())
                    .build()
        };
    }

    private MethodResponse[] execute(
            EchoMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        return new MethodResponse[] {
            EchoMethodResponse.builder().libraryName(methodCall.getLibraryName()).build()
        };
    }

    private MethodResponse[] execute(
            GetIdentityMethodCall getIdentityMethodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {

        IdentityDao identityDao = new IdentityImp(db);

        return new MethodResponse[] {
            GetIdentityMethodResponse.builder()
                    .list(
                            new Identity[] {
                                identityDao.read(getIdentityMethodCall.getAccountId()).get()
                            })
                    .build()
        };
    }

    private MethodResponse[] execute(
            GetMailboxMethodCall getMailboxMethodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {

        final Request.Invocation.ResultReference idsReference =
                getMailboxMethodCall.getIdsReference();
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
            final String[] idsParameter = getMailboxMethodCall.getIds();
            ids = idsParameter == null ? null : Arrays.asList(idsParameter);
        }

        MailboxInfoDao mailboxInfoDao = new MailboxInfoImp(db);
        ArrayList<MailboxInfo> mailboxs = mailboxInfoDao.readAll();

        return new MethodResponse[] {
            GetMailboxMethodResponse.builder()
                    .list(
                            mailboxs.stream()
                                    .map(this::toMailbox)
                                    .filter(m -> ids == null || ids.contains(m.getId()))
                                    .toArray(Mailbox[]::new))
                    .state(getState())
                    .build()
        };
    }

    private MethodResponse[] execute(
            QueryChangesEmailMethodCall queryChangesEmailMethodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = queryChangesEmailMethodCall.getSinceQueryState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                QueryChangesEmailMethodResponse.builder()
                        .oldQueryState(getState())
                        .newQueryState(getState())
                        .added(Collections.emptyList())
                        .removed(new String[0])
                        .build()
            };
        } else {
            return new MethodResponse[] {new CannotCalculateChangesMethodErrorResponse()};
        }
    }

    private MethodResponse[] execute(
            ChangesMailboxMethodCall methodCall,
            ListMultimap<String, Response.Invocation> previousResponses) {
        final String since = methodCall.getSinceState();
        if (since != null && since.equals(getState())) {
            return new MethodResponse[] {
                ChangesMailboxMethodResponse.builder()
                        .oldState(getState())
                        .newState(getState())
                        .updated(new String[0])
                        .created(new String[0])
                        .destroyed(new String[0])
                        .updatedProperties(new String[0])
                        .build()
            };
        } else {
            final Update update = getAccumulatedUpdateSince(since);
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

    private Update getAccumulatedUpdateSince(final String oldVersion) {
        final ArrayList<Update> updates = new ArrayList<>();
        for (Map.Entry<String, Update> updateEntry : this.updates.entrySet()) {
            if (updateEntry.getKey().equals(oldVersion) || updates.size() > 0) {
                updates.add(updateEntry.getValue());
            }
        }
        if (updates.isEmpty()) {
            return null;
        }
        return Update.merge(updates);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
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

    // Session
    public String getState() {
        return db.getDatabase()
                .getCollection("Account")
                .find(Filters.eq("_id", "0"))
                .first()
                .getString("state");
        /*return MongoConnectionSingleton.INSTANCE
        .getConnection()
        .getDatabase()
        .getCollection("Account")
        .find(Filters.eq("_id", "0"))
        .first()
        .getString("state");*/
    }

    public void increaseState() {
        db.getDatabase()
                .getCollection("Account")
                .updateOne(
                        Filters.eq("_id", "0"),
                        Updates.set("state", String.valueOf(Integer.valueOf(getState()) + 1)));
        /*MongoConnectionSingleton.INSTANCE
        .getConnection()
        .getDatabase()
        .getCollection("Account")
        .updateOne(
                Filters.eq("_id", "0"),
                Updates.set("state", String.valueOf(Integer.valueOf(getState()) + 1)));*/
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

    private void createEmail(final Email email) {
        EmailDao emailDao = new EmailImp(db);
        emailDao.write(email);
        increaseState();
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
