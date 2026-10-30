package com.demo.example.mstransfercontrol.service.impl;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.entity.Account;
import com.demo.example.mstransfercontrol.entity.Transfer;
import com.demo.example.mstransfercontrol.mapper.TransferMapper;
import com.demo.example.mstransfercontrol.mapper.impl.TransferMapperImpl;
import com.demo.example.mstransfercontrol.repository.AccountRepository;
import com.demo.example.mstransfercontrol.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Spy
    private TransferMapper transferMapper = new TransferMapperImpl();

    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    void debeRegistrarTransferenciaCuandoSeCumplenTodasLasReglas() {
        // Arrange
        TransferRequest solicitud = crearSolicitud("1000.00");
        Account activeAccount = Account.builder()
                .accountNumber(solicitud.getSourceAccount())
                .status("ACTIVE")
                .build();
        when(accountRepository.findByAccountNumber(solicitud.getSourceAccount()))
                .thenReturn(Optional.of(activeAccount));
        when(transferRepository.totalTransferredToday(
                eq(solicitud.getSourceAccount()), any(), any()))
                .thenReturn(new BigDecimal("9000.00"));

        // Act
        TransferResponse respuesta = transferService.validateAndTransfer(solicitud);

        // Assert
        assertEquals("APPROVED", respuesta.getStatus());
        assertEquals(solicitud.getSourceAccount(), respuesta.getSourceAccount());
        assertEquals(solicitud.getTargetAccount(), respuesta.getTargetAccount());
        assertEquals(solicitud.getAmount(), respuesta.getAmount());
        verify(accountRepository).findByAccountNumber(solicitud.getSourceAccount());
        verify(transferRepository).totalTransferredToday(
                eq(solicitud.getSourceAccount()), any(), any());
        verify(transferMapper).toTransferEntity(solicitud);
        verify(transferRepository).save(any(Transfer.class));
        verify(transferMapper).toApprovedResponse(solicitud);
    }

    @Test
    void debeLanzarExcepcionCuandoLaCuentaOrigenNoExiste() {
        // Arrange
        TransferRequest solicitud = crearSolicitud("100.00");
        when(accountRepository.findByAccountNumber(solicitud.getSourceAccount()))
                .thenReturn(Optional.empty());

        // Act
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> transferService.validateAndTransfer(solicitud));

        // Assert
        assertEquals("Cuenta origen no existe", exception.getMessage());
        verify(accountRepository).findByAccountNumber(solicitud.getSourceAccount());
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @Test
    void debeLanzarExcepcionCuandoLaCuentaOrigenNoEstaActiva() {
        // Arrange
        TransferRequest solicitud = crearSolicitud("100.00");
        Account cuentaBloqueada = Account.builder()
                .accountNumber(solicitud.getSourceAccount())
                .status("BLOCKED")
                .build();
        when(accountRepository.findByAccountNumber(solicitud.getSourceAccount()))
                .thenReturn(Optional.of(cuentaBloqueada));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> transferService.validateAndTransfer(solicitud));

        // Assert
        assertEquals("Cuenta origen no esta activa", exception.getMessage());
        verify(accountRepository).findByAccountNumber(solicitud.getSourceAccount());
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01"})
    void debeLanzarExcepcionCuandoElMontoNoEsMayorQueCero(String monto) {
        // Arrange
        TransferRequest solicitud = crearSolicitud(monto);
        Account cuentaActiva = Account.builder()
                .accountNumber(solicitud.getSourceAccount())
                .status("ACTIVE")
                .build();
        when(accountRepository.findByAccountNumber(solicitud.getSourceAccount()))
                .thenReturn(Optional.of(cuentaActiva));

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.validateAndTransfer(solicitud));

        // Assert
        assertEquals("Monto debe ser mayor a cero", exception.getMessage());
        verify(accountRepository).findByAccountNumber(solicitud.getSourceAccount());
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @Test
    void noDebeGuardarTransferenciaCuandoSeExcedeElLimiteDiario() {
        // Arrange
        TransferRequest solicitud = crearSolicitud("1000.01");
        Account cuentaActiva = Account.builder()
                .accountNumber(solicitud.getSourceAccount())
                .status("ACTIVE")
                .build();
        when(accountRepository.findByAccountNumber(solicitud.getSourceAccount()))
                .thenReturn(Optional.of(cuentaActiva));
        when(transferRepository.totalTransferredToday(
                eq(solicitud.getSourceAccount()), any(), any()))
                .thenReturn(new BigDecimal("9000.00"));

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.validateAndTransfer(solicitud));

        // Assert
        assertEquals("Limite diario excedido", exception.getMessage());
        verify(transferRepository).totalTransferredToday(
                eq(solicitud.getSourceAccount()), any(), any());
        verify(transferRepository, never()).save(any(Transfer.class));
        verifyNoInteractions(transferMapper);
    }

    @Test
    void debeLanzarExcepcionCuandoElMontoEsNulo() {
        // Arrange
        TransferRequest solicitud = crearSolicitud(null);
        Account cuentaActiva = Account.builder()
                .accountNumber(solicitud.getSourceAccount())
                .status("ACTIVE")
                .build();
        when(accountRepository.findByAccountNumber(solicitud.getSourceAccount()))
                .thenReturn(Optional.of(cuentaActiva));

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.validateAndTransfer(solicitud));

        // Assert
        assertEquals("Monto debe ser mayor a cero", exception.getMessage());
        verify(accountRepository).findByAccountNumber(solicitud.getSourceAccount());
        verifyNoInteractions(transferRepository, transferMapper);
    }

    private TransferRequest crearSolicitud(String monto) {
        return TransferRequest.builder()
                .customerId("customer-1")
                .sourceAccount("account-1")
                .targetAccount("account-2")
                .amount(monto == null ? null : new BigDecimal(monto))
                .build();
    }
}
