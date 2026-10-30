package com.demo.example.mstransfercontrol.controller;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.service.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    @Test
    void debeInvocarAlServicioUnaVezYRetornarLaRespuesta() {
        // Arrange
        TransferRequest solicitud = TransferRequest.builder()
                .customerId("cliente-1")
                .sourceAccount("cuenta-origen")
                .targetAccount("cuenta-destino")
                .amount(new BigDecimal("100.00"))
                .build();
        TransferResponse respuestaEsperada = TransferResponse.builder()
                .status("APPROVED")
                .sourceAccount(solicitud.getSourceAccount())
                .targetAccount(solicitud.getTargetAccount())
                .amount(solicitud.getAmount())
                .build();
        Map<String, String> headers = Map.of("canal", "web");
        when(transferService.validateAndTransfer(solicitud))
                .thenReturn(respuestaEsperada);

        // Act
        TransferResponse respuesta = transferController.transfer(headers, solicitud);

        // Assert
        assertSame(respuestaEsperada, respuesta);
        verify(transferService, times(1)).validateAndTransfer(solicitud);
    }
}
