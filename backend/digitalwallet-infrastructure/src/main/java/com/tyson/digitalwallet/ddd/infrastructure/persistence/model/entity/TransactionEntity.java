package com.tyson.digitalwallet.ddd.infrastructure.persistence.model.entity;

import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    public TransactionEntity(){};

    private UUID id;

    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal amountBefore;
    private BigDecimal amountAfter;
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private WalletEntity receiverWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id", nullable = false)
    private WalletEntity senderWallet;
}
