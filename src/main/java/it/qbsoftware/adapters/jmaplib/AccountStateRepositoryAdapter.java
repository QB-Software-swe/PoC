package it.qbsoftware.adapters.jmaplib;

import it.qbsoftware.business.domain.AccountState;
import it.qbsoftware.business.ports.out.domain.AccountStateRepository;

public class AccountStateRepositoryAdapter implements AccountStateRepository{
    AccountStateRepository accountStateRepository;

    @Override
    public AccountState retrive(String accountId) {
        return accountStateRepository.retrive(accountId);
    }

}
