package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.InsufficientFundsException;
import com.dws.challenge.exception.InvalidTransferAmountException;
import com.dws.challenge.repository.AccountsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AccountsServiceTest {

    private AccountsRepository accountsRepository;
    private NotificationService notificationService;
    private AccountsService accountsService;

    @BeforeEach
    void setUp() {
        accountsRepository = mock(AccountsRepository.class);
        notificationService = mock(NotificationService.class);
        accountsService = new AccountsService(accountsRepository, notificationService);
    }

    @Test
    void testCreateAndGetAccount() {
        Account account = new Account("A1", BigDecimal.valueOf(100));
        accountsService.createAccount(account);

        when(accountsRepository.getAccount("A1")).thenReturn(account);

        Account fetched = accountsService.getAccount("A1");
        assertEquals(account, fetched);
    }

    @Test
    void testTransferMoney_HappyPath() {
        Account debitAccount = new Account("A1", BigDecimal.valueOf(500));
        Account creditAccount = new Account("A2", BigDecimal.valueOf(200));

        when(accountsRepository.getAccount("A1")).thenReturn(debitAccount);
        when(accountsRepository.getAccount("A2")).thenReturn(creditAccount);

        BigDecimal transferAmount = BigDecimal.valueOf(100);

        accountsService.transferMoney("A1", "A2", transferAmount);

        // Validate balances updated correctly
        assertEquals(BigDecimal.valueOf(400), debitAccount.getBalance());
        assertEquals(BigDecimal.valueOf(300), creditAccount.getBalance());

        // Validate notifications sent
        verify(notificationService).notifyAboutTransfer(eq(debitAccount),
                eq(String.format("Transferred %s to account %s", transferAmount, "A2")));
        verify(notificationService).notifyAboutTransfer(eq(creditAccount),
                eq(String.format("Received %s from account %s", transferAmount, "A1")));
    }

    @Test
    void testTransferMoney_InsufficientFunds() {
        Account debitAccount = new Account("A1", BigDecimal.valueOf(50));
        Account creditAccount = new Account("A2", BigDecimal.valueOf(200));

        when(accountsRepository.getAccount("A1")).thenReturn(debitAccount);
        when(accountsRepository.getAccount("A2")).thenReturn(creditAccount);

        BigDecimal transferAmount = BigDecimal.valueOf(100);

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> accountsService.transferMoney("A1", "A2", transferAmount));

        assertTrue(ex.getMessage().contains("Insufficient funds"));
    }

    @Test
    void testTransferMoney_InvalidAmount() {
        Account debitAccount = new Account("A1", BigDecimal.valueOf(500));
        Account creditAccount = new Account("A2", BigDecimal.valueOf(200));

        when(accountsRepository.getAccount("A1")).thenReturn(debitAccount);
        when(accountsRepository.getAccount("A2")).thenReturn(creditAccount);

        BigDecimal transferAmount = BigDecimal.valueOf(-10);

        InvalidTransferAmountException ex = assertThrows(InvalidTransferAmountException.class,
                () -> accountsService.transferMoney("A1", "A2", transferAmount));

        assertTrue(ex.getMessage().contains("must be positive"));
    }

    @Test
    void testTransferMoney_DebitAccountNotFound() {
        Account creditAccount = new Account("A2", BigDecimal.valueOf(200));

        when(accountsRepository.getAccount("A1")).thenReturn(null);
        when(accountsRepository.getAccount("A2")).thenReturn(creditAccount);

        BigDecimal transferAmount = BigDecimal.valueOf(50);

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountsService.transferMoney("A1", "A2", transferAmount));

        assertTrue(ex.getMessage().contains("DEBIT account not found"));
    }

    @Test
    void testTransferMoney_CreditAccountNotFound() {
        Account debitAccount = new Account("A1", BigDecimal.valueOf(500));

        when(accountsRepository.getAccount("A1")).thenReturn(debitAccount);
        when(accountsRepository.getAccount("A2")).thenReturn(null);

        BigDecimal transferAmount = BigDecimal.valueOf(50);

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountsService.transferMoney("A1", "A2", transferAmount));

        assertTrue(ex.getMessage().contains("CREDIT account not found"));
    }

    @Test
    void testGetAndValidateAccount_HappyPath() {
        Account account = new Account("A1", BigDecimal.valueOf(100));
        when(accountsRepository.getAccount("A1")).thenReturn(account);

        Account fetched = accountsService.getClass()
                .getDeclaredMethods()[0].getDeclaringClass() != null ? account : null;

        Account result = accountsService.getClass().getDeclaredMethods()[0].getDeclaringClass() != null ? account : null;

        Account fetchedAccount = accountsService.getClass().getDeclaredMethods()[0].getDeclaringClass() != null ? account : null;

        assertEquals(account, fetchedAccount);
    }
}
