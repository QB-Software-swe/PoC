package it.qbsoftware.persistence;

import java.util.ArrayList;
import rs.ltt.jmap.common.entity.Email;

public interface EmailDao {
    ArrayList<Email> getAll();

    ArrayList<Email> readFromMailbox(String mailboxsId);

    void write(Email email);
}
