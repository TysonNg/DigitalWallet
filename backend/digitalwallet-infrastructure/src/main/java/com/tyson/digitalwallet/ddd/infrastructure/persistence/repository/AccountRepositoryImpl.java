package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Account;
import com.tyson.digitalwallet.ddd.domain.repository.AccountRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.LedgerMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.AccountEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class AccountRepositoryImpl implements AccountRepository {

    private final SpringDataAccountRepository springDataAccountRepository;
    private final LedgerMapper ledgerMapper;

    public AccountRepositoryImpl(SpringDataAccountRepository springDataAccountRepository, LedgerMapper ledgerMapper) {
        this.springDataAccountRepository = springDataAccountRepository;
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return springDataAccountRepository.findById(id)
                .map(ledgerMapper::toDomain);
    }

    @Override
    public Optional<Account> findByCode(String code) {
        return springDataAccountRepository.findByCode(code)
                .map(ledgerMapper::toDomain);
    }

    @Override
    public Optional<Account> findByWalletId(UUID walletId) {
        return springDataAccountRepository.findByWallet_Id(walletId)
                .map(ledgerMapper::toDomain);
    }

    @Override
    public boolean existsByWalletId(UUID walletId) {
        return springDataAccountRepository.existsByWallet_Id(walletId);
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = ledgerMapper.toEntity(account);
        AccountEntity saved = springDataAccountRepository.save(entity);
        return ledgerMapper.toDomain(saved);
    }

    @Override
    public List<Account> findAll() {
        return springDataAccountRepository.findAll().stream()
                .map(ledgerMapper::toDomain)
                .collect(Collectors.toList());
    }
}
