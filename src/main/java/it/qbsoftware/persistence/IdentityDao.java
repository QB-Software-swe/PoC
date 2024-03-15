package it.qbsoftware.persistence;

import java.util.Optional;
import rs.ltt.jmap.common.entity.Identity;

public interface IdentityDao {
    Optional<Identity> read(String id);

    void write(Identity identity);
}
