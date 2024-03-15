package it.qbsoftware.core.util;

import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;

public class MailboxInfo implements IdentifiableMailboxWithRole {
    final String id;
    final String name;
    final Role role;

    public MailboxInfo(String id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Role getRole() {
        return role;
    }

    public String getName() {
        return name;
    }
}
