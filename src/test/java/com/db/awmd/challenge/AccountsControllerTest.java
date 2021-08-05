package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void transferMoney() throws Exception{

    String fromAccountId = "Id-fromAccount";
    String toAccountId = "Id-toAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1050}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":950}")).andExpect(status().isCreated());

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":50}"))
            .andExpect(status().isAccepted());

    Account fromAccount = accountsService.getAccount(fromAccountId);
    assertThat(fromAccount.getBalance()).isEqualByComparingTo("1000");

    Account toAccount = accountsService.getAccount(toAccountId);
    assertThat(toAccount.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void transferMoneyWithEmptyFromAccountId() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1050}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    fromAccountId = "";

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":50}"))
            .andExpect(status().isBadRequest());

  }


  @Test
  public void transferMoneyWithoutFromAccountId() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"toAccountId\":\"" + toAccountId + "\", \"amount\":50}"))
            .andExpect(status().isBadRequest());

  }

  @Test
  public void transferMoneyWhenNullFromAccountId() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    fromAccountId = null;

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":" + fromAccountId + ",\"toAccountId\":\"" + toAccountId + "\", \"amount\":50}"))
            .andExpect(status().isBadRequest());

  }

  @Test
  public void transferMoneyWhenNullToAccountId() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    toAccountId = null;

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":" + toAccountId + ", \"amount\":50}"))
            .andExpect(status().isBadRequest());

  }


  @Test
  public void transferMoneyWithEmptyToAccountId() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());


    toAccountId = "";

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":50}"))
            .andExpect(status().isBadRequest());

  }


  @Test
  public void transferMoneyWithoutToAccountId() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());


    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\", \"amount\":50}"))
            .andExpect(status().isBadRequest());

  }


  @Test
  public void transferMoneyWhenNullAmountToTransfer() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    String amount = null;

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":"+amount+"}"))
            .andExpect(status().isBadRequest());

  }

  @Test
  public void transferMoneyWhenEmptyAmountToTransfer() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    String amount = "";

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":"+amount+"}"))
            .andExpect(status().isBadRequest());

  }


  @Test
  public void transferMoneyWithoutAmountToTransfer() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());


    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\", \"toAccountId\":\"" + toAccountId + "\"}"))
            .andExpect(status().isBadRequest());

  }

  @Test
  public void transferMoneyWithNegativeAmountToTransfer() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    String amount = "-1";

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":"+amount+"}"))
            .andExpect(status().isBadRequest());

  }

  @Test
  public void transferMoneyWhenAmountToTransferMoreThanAccountBalance() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    String amount = "5000";

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":"+amount+"}"))
            .andExpect(status().isForbidden());

  }

  @Test
  public void transferMoneyWhenAccountDoesNotExist() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    String amount = "100";

    this.mockMvc.perform(put("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":"+amount+"}"))
            .andExpect(status().isNotFound());

  }

  @Test
  public void transferMoneyWithMultipleThreads() throws Exception{

    String toAccountId = "Id-toAccount";
    String fromAccountId = "Id-fromAccount";

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountId\":\"" +toAccountId+ "\",\"balance\":1000}")).andExpect(status().isCreated());

    BigDecimal amount = new BigDecimal(1);

    Thread t1 = new Thread(()->{
      for (int n = 0; n < 50; n++) {
        try {
          this.mockMvc.perform(put("/v1/accounts/transfer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"fromAccountId\":\"" + fromAccountId + "\",\"toAccountId\":\"" + toAccountId + "\", \"amount\":" + amount + "}"))
                  .andExpect(status().isAccepted());
        } catch (Exception e) {

        }
      }
    });

    Thread t2 = new Thread(()->{
      for (int n = 0; n < 50; n++) {
        try {
          this.mockMvc.perform(put("/v1/accounts/transfer")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"fromAccountId\":\"" + toAccountId + "\",\"toAccountId\":\"" + fromAccountId + "\", \"amount\":" + amount + "}"))
                  .andExpect(status().isAccepted());
        } catch (Exception e) {

        }
      }
    });

    t1.start();
    t2.start();

    t1.join();
    t2.join();

    this.mockMvc.perform(get("/v1/accounts/" + toAccountId))
            .andExpect(status().isOk())
            .andExpect(
                    content().string("{\"accountId\":\"" + toAccountId + "\",\"balance\":1000}"));

    this.mockMvc.perform(get("/v1/accounts/" + fromAccountId))
            .andExpect(status().isOk())
            .andExpect(
                    content().string("{\"accountId\":\"" + fromAccountId + "\",\"balance\":1000}"));


  }
}
