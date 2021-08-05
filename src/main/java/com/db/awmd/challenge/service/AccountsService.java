package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.SyncAccount;
import com.db.awmd.challenge.domain.TransferRequestDetails;
import com.db.awmd.challenge.exception.AccountDoesNotExistsException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Value("${server.connection-timeout}")
  private String connectionTimeout;

  @Setter
  private NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferMoney(TransferRequestDetails transferRequestDetails) throws InsufficientBalanceException, AccountDoesNotExistsException {

    SyncAccount fromAccount = (SyncAccount) this.accountsRepository
            .getAccount(transferRequestDetails.getFromAccountId());
    SyncAccount toAccount = (SyncAccount) this.accountsRepository
            .getAccount(transferRequestDetails.getToAccountId());

    try {
      boolean isFromAccountLocked = fromAccount.getLock().tryLock(Long.valueOf(connectionTimeout), TimeUnit.MILLISECONDS);

      if(isFromAccountLocked){  //from account is locked
        boolean isToAccountLocked = toAccount.getLock().tryLock(Long.valueOf(connectionTimeout), TimeUnit.MILLISECONDS);

        if(isToAccountLocked){  //to account is also locked

          doTransaction(transferRequestDetails, fromAccount, toAccount);

          toAccount.getLock().unlock();
        }

        fromAccount.getLock().unlock();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Something went wrong. Server not able to process the request");
    }
  }

  private void doTransaction(TransferRequestDetails transferRequestDetails, SyncAccount fromAccount, SyncAccount toAccount) throws InsufficientBalanceException {
    BigDecimal amount = transferRequestDetails.getAmount();

    if ((fromAccount.getBalance().compareTo(amount)) >= 0) {
      fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
      toAccount.setBalance(toAccount.getBalance().add(amount));

      notificationService.notifyAboutTransfer(fromAccount, "Amount Debited: " + amount + ". You have successfully transferred amount: " + amount + " to AccountID: " + toAccount.getAccountId());
      notificationService.notifyAboutTransfer(toAccount, "Amount Credited: " + amount + ". You have received amount: " + amount + " from AccountID: " + fromAccount.getAccountId());
    } else {
      throw new InsufficientBalanceException("Insufficient account balance in accountId:" + fromAccount.getAccountId() + "to perform this transaction");
    }
  }
}

