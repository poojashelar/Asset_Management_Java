package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequestDetails;
import com.db.awmd.challenge.exception.AccountDoesNotExistsException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;

import com.db.awmd.challenge.service.NotificationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }

  @Test
  public void getAccount() throws Exception{
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    account.setBalance(new BigDecimal(1000));

    this.accountsService.createAccount(account);

    Account fetchedAccount = this.accountsService.getAccount(uniqueId);
    assertThat(account.getAccountId()).isEqualTo(uniqueId);
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }


  @Test
  public void getAccountDoesNotExist(){
    String uniqueId = "Id-" + System.currentTimeMillis();
    this.accountsService.getAccount(uniqueId);

  }

  @Test
  public void transferAmount() throws Exception{
    String fromAccountId = "Id-fromAccount";
    String toAccountId = "Id-toAccount";

    this.accountsService.createAccount(new Account(fromAccountId, new BigDecimal(1000)));
    this.accountsService.createAccount(new Account(toAccountId, new BigDecimal(1000)));

    this.accountsService.transferMoney(new TransferRequestDetails(fromAccountId, toAccountId, new BigDecimal(100)));

    Account fromAccount = this.accountsService.getAccount(fromAccountId);
    assertThat(fromAccount.getBalance()).isEqualByComparingTo("900");

    Account toAccount = this.accountsService.getAccount(toAccountId);
    assertThat(toAccount.getBalance()).isEqualByComparingTo("1100");

  }


  @Test
  public void transferAmountAmountMoreThanBalance() throws Exception{
    String fromAccountId = "Id-fromAccount";
    String toAccountId = "Id-toAccount";

    this.accountsService.createAccount(new Account(fromAccountId, new BigDecimal(1000)));
    this.accountsService.createAccount(new Account(toAccountId, new BigDecimal(1000)));

    try {
      this.accountsService.transferMoney(new TransferRequestDetails(fromAccountId, toAccountId, new BigDecimal(10000)));
    }catch (InsufficientBalanceException insufficientBalanceException){
      assertThat(insufficientBalanceException.getMessage()).isEqualTo("Insufficient account balance in accountId:"+fromAccountId+"to perform this transaction");
    }

  }


  @Test
  public void transferAmountNegativeAmount() throws Exception{
    String fromAccountId = "Id-fromAccount";
    String toAccountId = "Id-toAccount";

    this.accountsService.createAccount(new Account(fromAccountId, new BigDecimal(1000)));
    this.accountsService.createAccount(new Account(toAccountId, new BigDecimal(1000)));

    try {
      this.accountsService.transferMoney(new TransferRequestDetails(fromAccountId, toAccountId, new BigDecimal(-1)));
    }catch (InsufficientBalanceException insufficientBalanceException){
      assertThat(insufficientBalanceException.getMessage()).isEqualTo("Insufficient account balance in accountId:"+fromAccountId+"to perform this transaction");
    }

  }


  @Test
  public void transferAmountCheckNotification() throws Exception{
    String fromAccountId = "Id-fromAccount";
    String toAccountId = "Id-toAccount";
    BigDecimal amountToTransfer = new BigDecimal(100);

    Account fromAccount = new Account(fromAccountId, new BigDecimal(1000));
    Account toAccount = new Account(toAccountId, new BigDecimal(1000));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    NotificationService notificationService = Mockito.mock(NotificationService.class);

    this.accountsService.setNotificationService(notificationService);

    this.accountsService.transferMoney(new TransferRequestDetails(fromAccountId, toAccountId, amountToTransfer));
    fromAccount.setBalance(new BigDecimal(900));
    toAccount.setBalance(new BigDecimal(1100));

    verify(notificationService, times(1)).notifyAboutTransfer(fromAccount,"Amount Debited: " + amountToTransfer + ". You have successfully transferred amount: " + amountToTransfer + " to AccountID: "+ toAccount.getAccountId());
    verify(notificationService, times(1)).notifyAboutTransfer(toAccount,"Amount Credited: " + amountToTransfer + ". You have received amount: " + amountToTransfer + " from AccountID: " + fromAccount.getAccountId());
  }


}
