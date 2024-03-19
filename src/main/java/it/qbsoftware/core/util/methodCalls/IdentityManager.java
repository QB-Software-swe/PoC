package it.qbsoftware.core.util.methodCalls;

import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import it.qbsoftware.persistence.IdentityDao;
import it.qbsoftware.persistence.IdentityImp;
import it.qbsoftware.persistence.MongoConnection;
import rs.ltt.jmap.common.Response;
import rs.ltt.jmap.common.entity.Identity;
import rs.ltt.jmap.common.method.MethodResponse;
import rs.ltt.jmap.common.method.call.identity.GetIdentityMethodCall;
import rs.ltt.jmap.common.method.response.identity.GetIdentityMethodResponse;

public class IdentityManager {
    MongoConnection db;

    @Inject
    public IdentityManager(MongoConnection db) {
        this.db = db;
    }

    public MethodResponse[] get(
            GetIdentityMethodCall call,
            ListMultimap<String, Response.Invocation> previousResponses) {
        IdentityDao identity = new IdentityImp(db);
        return new MethodResponse[] {
            GetIdentityMethodResponse.builder()
                    .list(new Identity[] {identity.read(call.getAccountId()).get()})
                    .build()
        };
    }
}
