package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferAccountType;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidTransferAmountException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository,
                         NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Transfers money from one account to another with proper validation,
   * concurrency control, and notifications.
   *
   * @param sourceAccountId
   * @param destinationAccountId
   * @param amount
   */
  public void transferMoney(String sourceAccountId, String destinationAccountId, BigDecimal amount) {
    log.info("Initiating transfer: from sourceAccountId : {} to destinationAccountId : {} with amount : {}", sourceAccountId, destinationAccountId, amount);

    // Step 1: Validate the transfer amount
    validateTransferAmount(amount);

    // Step 2: Fetch and validate both debit (source) and credit (destination) accounts
    Account debitAccountDetails = getAndValidateAccount(sourceAccountId, TransferAccountType.DEBIT);
    Account creditAccountDetails = getAndValidateAccount(destinationAccountId, TransferAccountType.CREDIT);

    // Step 3: Ensure consistent lock order based on accountId to prevent deadlocks
    Object firstLock = debitAccountDetails.getAccountId().compareTo(creditAccountDetails.getAccountId()) < 0
            ? debitAccountDetails : creditAccountDetails;
    Object secondLock = firstLock == debitAccountDetails ? creditAccountDetails : debitAccountDetails;

    synchronized (firstLock) {
      synchronized (secondLock) {

        // Step 4: Validate that debit account has sufficient balance
        validateSufficientFunds(debitAccountDetails, amount);

        // Step 5: Perform the actual debit & credit operations
        performTransfer(debitAccountDetails, creditAccountDetails, amount);

        // Step 6: Notify both account holders about the transfer
        sendDebitAndCreditNotifications(debitAccountDetails, creditAccountDetails, amount);
      }
    }

    log.info("Transfer completed successfully debitAccount : {}, creditAccount : {} and amount : {}",
            sourceAccountId, destinationAccountId, amount);
  }

  /**
   * Validates that the transfer amount is positive.
   */
  private void validateTransferAmount(BigDecimal amount) {
    log.info("Validating transfer amount");

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.error("Invalid transfer amount detected: {}, it must be greater than zero", amount);
      throw new InvalidTransferAmountException("Transfer amount must be positive");
    }

    log.info("Transfer amount validated successfully");
  }

  /**
   * Retrieves and validates that the account exists.
   *
   * @param accountId
   * @param accountType
   */
  private Account getAndValidateAccount(String accountId, TransferAccountType accountType) {

    // Fetch account details from repository using accountId
    Account accountDetails = accountsRepository.getAccount(accountId);

    // If no account is found, log an error and throw a custom exception
    if (ObjectUtils.isEmpty(accountDetails)) {
      log.error("{} account not found with accountId : {}", accountType, accountId);
      throw new AccountNotFoundException(accountType + " account not found with accountId : " + accountId);
    }

    log.info("Successfully found {} account with accountId : {}", accountType, accountId);

    return accountDetails;
  }

  /**
   * Validates that the source account has sufficient balance.
   *
   * @param debitAccountDetails
   * @param amount
   */
  private void validateSufficientFunds(Account debitAccountDetails, BigDecimal amount) {

    // Compare account balance with the transfer amount, log error and throw exception if balance is insufficient
    if (debitAccountDetails.getBalance().compareTo(amount) < 0) {
      log.error("Insufficient funds in account: {}", debitAccountDetails.getAccountId());
      throw new InsufficientFundsException("Insufficient funds in account " + debitAccountDetails.getAccountId());
    }

    log.debug("Sufficient funds available in account: {}", debitAccountDetails.getAccountId());
  }

  /**
   * Performs the actual money transfer between two accounts.
   *
   * @param debitAccountDetails
   * @param creditAccountDetails
   * @param amount
   */
  private void performTransfer(Account debitAccountDetails, Account creditAccountDetails,
                               BigDecimal amount) {
    log.info("Performing transfer: {} -> {} : {}", debitAccountDetails.getAccountId(),
            creditAccountDetails.getAccountId(), amount);

    // Debit the amount from source account
    debitAccountDetails.setBalance(debitAccountDetails.getBalance().subtract(amount));
    // Credit the amount from destination account
    creditAccountDetails.setBalance(creditAccountDetails.getBalance().add(amount));
  }

  /**
   * Sends notifications to both debit and credit account holders.
   *
   * @param debitAccountDetails
   * @param creditAccountDetails
   * @param amount
   */
  private void sendDebitAndCreditNotifications(Account debitAccountDetails, Account creditAccountDetails, BigDecimal amount) {
    log.info("Sending notifications for transfer to debitAccountId : {} and creditAccount : {}",
            debitAccountDetails.getAccountId(), creditAccountDetails.getAccountId());

    // Notify the account holder of the debited account about the transfer
    notificationService.notifyAboutTransfer(debitAccountDetails,
            String.format("Transferred %s to account %s", amount, creditAccountDetails.getAccountId()));

    // Notify the account holder of the credited account about the transfer
    notificationService.notifyAboutTransfer(creditAccountDetails,
            String.format("Received %s from account %s", amount, debitAccountDetails.getAccountId()));

    log.info("Notifications sent to both parties successfully");
  }
}
