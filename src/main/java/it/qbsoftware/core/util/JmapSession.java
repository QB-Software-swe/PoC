package it.qbsoftware.core.util;

import com.google.common.collect.ImmutableMap;
import rs.ltt.jmap.common.SessionResource;
import rs.ltt.jmap.common.SessionResource.SessionResourceBuilder;
import rs.ltt.jmap.common.entity.Account;
import rs.ltt.jmap.common.entity.Capability;
import rs.ltt.jmap.common.entity.capability.CoreCapability;
import rs.ltt.jmap.common.entity.capability.MailAccountCapability;
import rs.ltt.jmap.common.entity.capability.MailCapability;

@SuppressWarnings("null")
public class JmapSession {
    static final String API_ENDPOINT = "/api";
    static final String UPLOAD_ENDPOINT = "/upload";
    static final String DOWNLOAD_ENDPOINT = "/download";

    static final long maxServerSizeUpload = 100 * 1024 * 1024L;
    static final long maxServerObjectInGet = 4096;

    long maxSizeAttachmentsPerEmail;
    String state;

    static ImmutableMap.Builder<Class<? extends Capability>, Capability> serverCapability;

    static {
        serverCapability = ImmutableMap.builder();
        serverCapability.put(
                CoreCapability.class,
                CoreCapability.builder()
                        .maxSizeUpload(maxServerSizeUpload)
                        .maxObjectsInGet(maxServerObjectInGet)
                        .build());
        serverCapability.put(MailCapability.class, MailCapability.builder().build());
    }

    public JmapSession() {
        maxSizeAttachmentsPerEmail = 0;
        state = "0";
    }

    public SessionResource generateSessionResources() {
        SessionResourceBuilder sessionResourceBuilder = SessionResource.builder();

        sessionResourceBuilder
                .apiUrl(API_ENDPOINT)
                .uploadUrl(UPLOAD_ENDPOINT)
                .downloadUrl(DOWNLOAD_ENDPOINT)
                .eventSourceUrl("/api")
                .state(state);

        // Example
        sessionResourceBuilder
                .username("QB Software")
                .account(
                        "0",
                        Account.builder()
                                .name("QB Software")
                                .accountCapabilities(
                                        ImmutableMap.of(
                                                MailAccountCapability.class,
                                                MailAccountCapability.builder()
                                                        .maxSizeAttachmentsPerEmail(
                                                                maxSizeAttachmentsPerEmail)
                                                        .build()))
                                .isPersonal(true)
                                .isReadOnly(false)
                                .build())
                .capabilities(serverCapability.build())
                .primaryAccounts(ImmutableMap.of(MailAccountCapability.class, "0"))
                .build();

        return sessionResourceBuilder.build();
    }
}
