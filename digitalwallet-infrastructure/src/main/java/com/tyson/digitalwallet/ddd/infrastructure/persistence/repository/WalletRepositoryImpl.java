package com.tyson.digitalwallet.ddd.infrastructure.persistence.repository;

import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;

import com.tyson.digitalwallet.ddd.infrastructure.persistence.mapper.WalletMapper;
import com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity.WalletEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class WalletRepositoryImpl implements WalletRepository {
    private final SpringDataWalletRepository springDataWalletRepository;
    private final WalletMapper walletMapper;

    public WalletRepositoryImpl(SpringDataWalletRepository springDataWalletRepository, WalletMapper walletMapper) {
        this.springDataWalletRepository = springDataWalletRepository;
        this.walletMapper = walletMapper;
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return springDataWalletRepository.existsByUserId(userId);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return springDataWalletRepository.findById(id)
                .map(walletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return springDataWalletRepository.findByUserId(userId)
                .map(walletMapper::toDomain);
    }

    @Override
    public Wallet saveWallet(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        WalletEntity saved = springDataWalletRepository.save(entity);
        return walletMapper.toDomain(saved);
    }
}
