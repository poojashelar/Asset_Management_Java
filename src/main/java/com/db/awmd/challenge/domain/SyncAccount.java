package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

public class SyncAccount extends Account {

    @JsonIgnore
    @Getter
    ReentrantLock lock = new ReentrantLock(true);

    public SyncAccount(String accountId) {
        super(accountId);
    }

    public SyncAccount(String accountId, BigDecimal balance) {
        super(accountId, balance);
    }
}
