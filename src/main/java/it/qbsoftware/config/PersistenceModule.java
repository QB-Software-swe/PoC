package it.qbsoftware.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import it.qbsoftware.persistence.MongoConnection;

public class PersistenceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MongoConnection.class).in(Singleton.class);
        /*
        bind(EmailDao.class).to(EmailImp.class);
        bind(IdentityDao.class).to(IdentityImp.class);
        bind(MailboxInfoDao.class).to(MailboxInfoImp.class);
        */
    }
}
