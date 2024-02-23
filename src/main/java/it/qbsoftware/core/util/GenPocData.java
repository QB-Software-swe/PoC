package it.qbsoftware.core.util;

import it.qbsoftware.persistence.EmailDao;
import it.qbsoftware.persistence.EmailImp;
import it.qbsoftware.persistence.IdentityImp;
import it.qbsoftware.persistence.MailboxInfoDao;
import it.qbsoftware.persistence.MailboxInfoImp;
import it.qbsoftware.persistence.MongoConnectionSingleton;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.EmailBodyValue;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.entity.Role;

public class GenPocData {
    static final Logger logger = LoggerFactory.getLogger(GenPocData.class);

    public static void generate() {
        logger.info("Cleanup database");
        MongoConnectionSingleton.INSTANCE.getConnection().getDatabase().drop();
        logger.info("Generate data");
        generateAccount();
        generateMail();
        generateIdentity();
        generateMailboxInfo();
    }

    static void generateAccount() {
        Document doc = new Document();
        doc.put("_id", "0");
        doc.put("state", "0");
        MongoConnectionSingleton.INSTANCE
                .getConnection()
                .getDatabase()
                .getCollection("Account")
                .insertOne(doc);
    }

    static void generateIdentity() {
        new IdentityImp()
                .write(
                        Identity.builder()
                                .id("0")
                                .email("team@qbsoftware.it")
                                .name("QB Software")
                                .build());
        new IdentityImp()
                .write(
                        Identity.builder()
                                .id("1")
                                .email("example@examplemail.it")
                                .name("Identity example")
                                .build());
    }

    static void generateMailboxInfo() {
        MailboxInfo[] mailboxes =
                new MailboxInfo[] {
                    new MailboxInfo("0", "Inbox", Role.INBOX),
                    new MailboxInfo("1", "Important", Role.IMPORTANT),
                    new MailboxInfo("2", "Sent", Role.SENT),
                    new MailboxInfo("3", "Archived", Role.ARCHIVE)
                };

        MailboxInfoDao mailboxInfoDao = new MailboxInfoImp();
        for (MailboxInfo mailboxInfo : mailboxes) {
            mailboxInfoDao.write(mailboxInfo);
        }
    }

    static void generateMail() {

        HashMap<String, Boolean> mx = new HashMap<String, Boolean>();
        mx.put("0", true);
        mx.put("1", true);
        mx.put("2", true);

        Email[] emails =
                new Email[] {
                    Email.builder()
                            .id("0")
                            .threadId("0")
                            .sentAt(Instant.now().atOffset(ZoneOffset.ofHours(1)))
                            .receivedAt(Instant.now())
                            .to(
                                    EmailAddress.builder()
                                            .email("team@qbsoftware.org")
                                            .name("QB Software")
                                            .build())
                            .mailboxId("0", true)
                            .from(
                                    EmailAddress.builder()
                                            .email("example@mail.org")
                                            .name("Sender's name")
                                            .build())
                            .subject("Email object example")
                            .preview("Short preview of an e-mail")
                            .bodyStructure(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .bodyValue(
                                    "0",
                                    EmailBodyValue.builder()
                                            .value(
                                                    "Hello, this is an example of a content (body)"
                                                            + " of an e-mail.")
                                            .build())
                            .textBody(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .keyword("$seen", true)
                            .build(),
                    Email.builder()
                            .id("1")
                            .threadId("1")
                            .sentAt(Instant.now().atOffset(ZoneOffset.ofHours(1)))
                            .receivedAt(Instant.now())
                            .mailboxIds(mx)
                            .from(
                                    EmailAddress.builder()
                                            .email("team@qbsoftware.org")
                                            .name("QB Software Team")
                                            .build())
                            .to(
                                    EmailAddress.builder()
                                            .email("profX@uniX.org")
                                            .name("Prof X")
                                            .build())
                            .subject("Application for RTB revision")
                            .preview("RTB application")
                            .bodyStructure(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .bodyValue(
                                    "0",
                                    EmailBodyValue.builder()
                                            .value(
                                                    "Dear prof. X,\n"
                                                        + "with the present, the QB Software Team,"
                                                        + " would like to book for the RTB"
                                                        + " review\n\n"
                                                        + "Best regards, QB Software.")
                                            .build())
                            .textBody(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .keyword("$seen", false)
                            .build(),
                    Email.builder()
                            .id("2")
                            .threadId("1")
                            .sentAt(Instant.now().atOffset(ZoneOffset.ofHours(1)))
                            .receivedAt(Instant.now())
                            .mailboxId("0", true)
                            .from(
                                    EmailAddress.builder()
                                            .email("profX@uniX.org")
                                            .name("Prof X")
                                            .build())
                            .to(
                                    EmailAddress.builder()
                                            .email("team@qbsoftware.org")
                                            .name("QB Software Team")
                                            .build())
                            .subject("Application for RTB review")
                            .preview("RTB application")
                            .bodyStructure(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .bodyValue(
                                    "0",
                                    EmailBodyValue.builder()
                                            .value(
                                                    "Dear QB Software team, \n "
                                                            + "the appointment times are: \n"
                                                            + "- x y;\n"
                                                            + "- z k;\n\n"
                                                            + "Best regards, prof. X.")
                                            .build())
                            .textBody(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .keyword("$seen", false)
                            .build()
                };

        EmailDao emailDao = new EmailImp();

        for (Email email : emails) {
            emailDao.write(email);
        }
    }
}
