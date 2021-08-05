package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequestDetails;
import com.db.awmd.challenge.exception.AccountDoesNotExistsException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.TransactionFailedException;
import com.db.awmd.challenge.service.AccountsService;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

//  @PostMapping(path ="/{fromAccountId}/transfer")
//  public ResponseEntity<Object> transferMoney(@PathVariable String fromAccountId, @RequestBody TransferMoneyDetails transferMoneyDetails){
//  //  log.info("Transferring amount: "+ transferMoneyDetails.amount + " from Account Id: " + fromAccountId + "To Account Id :" + transferMoneyDetails.getAccountId());
//    try {
//      this.accountsService.transferMoney(fromAccountId, transferMoneyDetails.toAccount, transferMoneyDetails.amount);
//    } catch (TransactionFailedException tfe){
//      return new ResponseEntity<>(tfe.getMessage(), HttpStatus.BAD_REQUEST);
//    }
//    return new ResponseEntity<>(HttpStatus.OK);
//  }

  @PutMapping(path = "/transfer")
  public ResponseEntity<Object> amountTransfer(@RequestBody @Valid TransferRequestDetails transferRequestDetails){

    log.info("Transferring amount: {} from account id {} to account id {} ",
            transferRequestDetails.getAmount(),
            transferRequestDetails.getFromAccountId(),
            transferRequestDetails.getToAccountId());

    try {
      this.accountsService.transferMoney(transferRequestDetails);
    }
    catch (AccountDoesNotExistsException accountDoesNotExistsException){
      return new ResponseEntity<>(accountDoesNotExistsException.getMessage(), HttpStatus.NOT_FOUND);
    }
    catch (InsufficientBalanceException insufficientBalanceException) {
      return new ResponseEntity<>(insufficientBalanceException.getMessage(),HttpStatus.FORBIDDEN);
    }
    catch (RuntimeException exe){
      return new ResponseEntity<>(exe.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

}
