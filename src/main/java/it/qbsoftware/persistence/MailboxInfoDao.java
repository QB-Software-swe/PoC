package it.qbsoftware.persistence;

import it.qbsoftware.core.util.MailboxInfo;
import java.util.ArrayList;
import java.util.Optional;

public interface MailboxInfoDao {
    public ArrayList<MailboxInfo> readAll();

    Optional<MailboxInfo> read(String id);

    void write(MailboxInfo mailboxInfo);
}
