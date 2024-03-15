package it.qbsoftware.core.util;

import com.google.inject.Inject;
import it.qbsoftware.persistence.EmailDao;
import it.qbsoftware.persistence.EmailImp;
import it.qbsoftware.persistence.IdentityImp;
import it.qbsoftware.persistence.MailboxInfoDao;
import it.qbsoftware.persistence.MailboxInfoImp;
import it.qbsoftware.persistence.MongoConnection;
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
    private static MongoConnection db;

    @Inject
    public GenPocData(MongoConnection db) {
        GenPocData.db = db;
    }

    public void generate() {
        logger.info("Cleanup database");
        db.getDatabase().drop();
        logger.info("Generate data");
        generateAccount();
        generateMail();
        generateIdentity();
        generateMailboxInfo();
    }

    private void generateAccount() {
        Document doc = new Document();
        doc.put("_id", "0");
        doc.put("state", "0");
        db.getDatabase().getCollection("Account").insertOne(doc);
    }

    private void generateIdentity() {
        new IdentityImp(db)
                .write(
                        Identity.builder()
                                .id("0")
                                .email("team@qbsoftware.it")
                                .name("QB Software")
                                .build());
        new IdentityImp(db)
                .write(
                        Identity.builder()
                                .id("1")
                                .email("example@examplemail.it")
                                .name("Identità d'esempio")
                                .build());
    }

    private void generateMailboxInfo() {
        MailboxInfo[] mailboxes =
                new MailboxInfo[] {
                    new MailboxInfo("0", "Inbox", Role.INBOX),
                    new MailboxInfo("1", "Importanti", Role.IMPORTANT),
                    new MailboxInfo("2", "Inviate", Role.SENT),
                    new MailboxInfo("3", "Archiviate", Role.ARCHIVE)
                };

        MailboxInfoDao mailboxInfoDao = new MailboxInfoImp(db);
        for (MailboxInfo mailboxInfo : mailboxes) {
            mailboxInfoDao.write(mailboxInfo);
        }
    }

    private void generateMail() {

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
                                            .name("Nome del mittente")
                                            .build())
                            .subject("Esempio oggetto dell'e-mail")
                            .preview("Una breve preview dell'e-mail")
                            .bodyStructure(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .bodyValue(
                                    "0",
                                    EmailBodyValue.builder()
                                            .value(
                                                    "Ciao, questo è un esempio di contenuto (body)"
                                                            + " di un e-mail.")
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
                            .subject("Candidatura per la revisione RTB")
                            .preview("Candidatura RTB")
                            .bodyStructure(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .bodyValue(
                                    "0",
                                    EmailBodyValue.builder()
                                            .value(
                                                    "Gentile prof. X,\n"
                                                        + "con la presente il gruppo QB Software"
                                                        + " intende prenotarsi per la revisione"
                                                        + " RTB.\n\n"
                                                        + "Distinti saluti, QB Software.")
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
                            .subject("Candidatura per la revisione RTB")
                            .preview("Candidatura RTB")
                            .bodyStructure(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .bodyValue(
                                    "0",
                                    EmailBodyValue.builder()
                                            .value(
                                                    "Gentile gruppo QB Software, \n "
                                                            + "gli orari sono: \n"
                                                            + "- x y;\n"
                                                            + "- z k;\n\n"
                                                            + " Saluti, prof. X.")
                                            .build())
                            .textBody(
                                    EmailBodyPart.builder().partId("0").type("text/plain").build())
                            .keyword("$seen", false)
                            .build()
                };

        EmailDao emailDao = new EmailImp(db);

        for (Email email : emails) {
            emailDao.write(email);
        }
    }
}
