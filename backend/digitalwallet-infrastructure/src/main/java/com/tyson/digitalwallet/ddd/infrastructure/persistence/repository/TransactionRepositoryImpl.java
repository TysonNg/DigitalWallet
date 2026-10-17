package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.repository.TransactionRepository;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.TransactionMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.TransactionEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    private final SpringDataTransactionRepository springDataTransactionRepository;
    private final TransactionMapper transactionMapper;

    public TransactionRepositoryImpl(
            SpringDataTransactionRepository springDataTransactionRepository,
            TransactionMapper transactionMapper
    ) {
        this.springDataTransactionRepository = springDataTransactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = transactionMapper.toEntity(transaction);
        TransactionEntity saved = springDataTransactionRepository.save(entity);
        return transactionMapper.toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return springDataTransactionRepository.findById(id)
                .map(transactionMapper::toDomain);
    }

    @Override
    public List<Transaction> findByWalletId(UUID walletId) {
        return springDataTransactionRepository.findByWalletId(walletId).stream()
                .map(transactionMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return springDataTransactionRepository.count();
    }

    @Override
    public long countByStatus(TransactionStatus status) {
        return springDataTransactionRepository.countByStatus(status);
    }

    @Override
    public BigDecimal getTotalVolumeByStatus(TransactionStatus status) {
        return springDataTransactionRepository.sumTotalVolumeByStatus(status);
    }
}

