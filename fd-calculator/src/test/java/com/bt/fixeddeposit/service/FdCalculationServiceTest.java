package com.bt.fixeddeposit.service;

import com.bt.fixeddeposit.dto.FdCalculationRequest;
import com.bt.fixeddeposit.dto.FdCalculationResponse;
import com.bt.fixeddeposit.dto.external.ProductResponse;
import com.bt.fixeddeposit.entity.FdCalculation;
import com.bt.fixeddeposit.event.CustomerValidationResponse;
import com.bt.fixeddeposit.event.KafkaProducerService;
import com.bt.fixeddeposit.event.ProductDetailsResponse;
import com.bt.fixeddeposit.event.RequestResponseStore;
import com.bt.fixeddeposit.exception.*;
import com.bt.fixeddeposit.repository.FdCalculationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FdCalculationServiceTest {

        @Mock
        private FdCalculationRepository calculationRepository;

        @Mock
        private KafkaProducerService kafkaProducerService;

        @Mock
        private RequestResponseStore requestResponseStore;

        @InjectMocks
        private FdCalculationService calculationService;

        private FdCalculationRequest validRequest;
        @SuppressWarnings("unused")
        private ProductResponse validProduct;
        private FdCalculation savedCalculation;
        private String authToken;

        @BeforeEach
        void setUp() throws InterruptedException {
                ReflectionTestUtils.setField(calculationService, "defaultCompoundingFrequency", 4);
                ReflectionTestUtils.setField(calculationService, "roundingScale", 2);
                ReflectionTestUtils.setField(calculationService, "requestTimeoutSeconds", 30L);

                authToken = "Bearer valid-token";

                validRequest = FdCalculationRequest.builder()
                                .customerId(1L)
                                .productCode("FD-001")
                                .principalAmount(BigDecimal.valueOf(100000))
                                .tenureMonths(12)
                                .compoundingFrequency(4)
                                .build();

                validProduct = ProductResponse.builder()
                                .id(1L)
                                .productCode("FD-001")
                                .productName("Fixed Deposit - Regular")
                                .productType("FIXED_DEPOSIT")
                                .minInterestRate(BigDecimal.valueOf(6.5))
                                .maxInterestRate(BigDecimal.valueOf(7.5))
                                .minTermMonths(6)
                                .maxTermMonths(120)
                                .minAmount(BigDecimal.valueOf(10000))
                                .maxAmount(BigDecimal.valueOf(10000000))
                                .currency("USD")
                                .status("ACTIVE")
                                .build();

                savedCalculation = FdCalculation.builder()
                                .id(1L)
                                .customerId(1L)
                                .productCode("FD-001")
                                .principalAmount(BigDecimal.valueOf(100000))
                                .tenureMonths(12)
                                .interestRate(BigDecimal.valueOf(6.5))
                                .compoundingFrequency(4)
                                .maturityAmount(BigDecimal.valueOf(106659.46))
                                .interestEarned(BigDecimal.valueOf(6659.46))
                                .effectiveRate(BigDecimal.valueOf(6.66))
                                .currency("USD")
                                .calculationDate(LocalDateTime.now())
                                .createdAt(LocalDateTime.now())
                                .build();

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenReturn(null);
                when(calculationRepository.findByCustomerIdOrderByCreatedAtDesc(any()))
                                .thenReturn(Collections.emptyList());
        }

        @Test
        void testCalculateFd_Success() throws InterruptedException {
                doNothing().when(kafkaProducerService).sendCustomerValidationRequest(any());
                doNothing().when(kafkaProducerService).sendProductDetailsRequest(any());
                doNothing().when(requestResponseStore).putRequest(any(), any());

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenAnswer(invocation -> {
                                        String requestId = invocation.getArgument(0);
                                        if (requestId.contains("validation")) {
                                                return CustomerValidationResponse.builder()
                                                                .customerId(1L)
                                                                .valid(true)
                                                                .active(true)
                                                                .requestId(requestId)
                                                                .build();
                                        } else {
                                                return ProductDetailsResponse.builder()
                                                                .productId(1L)
                                                                .productCode("FD-001")
                                                                .minAmount(BigDecimal.valueOf(10000))
                                                                .maxAmount(BigDecimal.valueOf(10000000))
                                                                .minTermMonths(6)
                                                                .maxTermMonths(120)
                                                                .minInterestRate(BigDecimal.valueOf(6.5))
                                                                .maxInterestRate(BigDecimal.valueOf(7.5))
                                                                .status("ACTIVE")
                                                                .requestId(requestId)
                                                                .build();
                                        }
                                });

                when(calculationRepository.save(any(FdCalculation.class))).thenReturn(savedCalculation);

                FdCalculationResponse response = calculationService.calculateFd(validRequest, authToken);

                assertNotNull(response);
                assertEquals(1L, response.getId());
                assertEquals(BigDecimal.valueOf(100000), response.getPrincipalAmount());
                assertEquals(12, response.getTenureMonths());
                assertNotNull(response.getMaturityAmount());
                assertTrue(response.getMaturityAmount().compareTo(response.getPrincipalAmount()) > 0);

                verify(calculationRepository).save(any(FdCalculation.class));
        }

        @Test
        void testCalculateFd_InvalidCustomer() throws InterruptedException {
                doNothing().when(kafkaProducerService).sendCustomerValidationRequest(any());
                doNothing().when(requestResponseStore).putRequest(any(), any());

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenReturn(CustomerValidationResponse.builder()
                                                .valid(false)
                                                .requestId("test-req-id")
                                                .build());

                assertThrows(ServiceIntegrationException.class,
                                () -> calculationService.calculateFd(validRequest, authToken));

                verify(calculationRepository, never()).save(any());
        }

        @Test
        void testCalculateFd_InactiveCustomer() throws InterruptedException {
                doNothing().when(kafkaProducerService).sendCustomerValidationRequest(any());
                doNothing().when(requestResponseStore).putRequest(any(), any());

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenReturn(CustomerValidationResponse.builder()
                                                .customerId(1L)
                                                .valid(true)
                                                .active(false)
                                                .requestId("test-req-id")
                                                .build());

                assertThrows(InvalidCalculationDataException.class,
                                () -> calculationService.calculateFd(validRequest, authToken));

                verify(calculationRepository, never()).save(any());
        }

        @Test
        void testCalculateFd_InvalidProduct() throws InterruptedException {
                doNothing().when(kafkaProducerService).sendCustomerValidationRequest(any());
                doNothing().when(kafkaProducerService).sendProductDetailsRequest(any());
                doNothing().when(requestResponseStore).putRequest(any(), any());

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenAnswer(invocation -> {
                                        String requestId = invocation.getArgument(0);
                                        if (requestId.contains("validation")) {
                                                return CustomerValidationResponse.builder()
                                                                .customerId(1L)
                                                                .valid(true)
                                                                .active(true)
                                                                .requestId(requestId)
                                                                .build();
                                        } else {
                                                return ProductDetailsResponse.builder()
                                                                .productId(1L)
                                                                .productCode("FD-001")
                                                                .status("INACTIVE")
                                                                .minAmount(BigDecimal.valueOf(10000))
                                                                .maxAmount(BigDecimal.valueOf(10000000))
                                                                .minTermMonths(6)
                                                                .maxTermMonths(120)
                                                                .requestId(requestId)
                                                                .build();
                                        }
                                });

                assertThrows(InvalidCalculationDataException.class,
                                () -> calculationService.calculateFd(validRequest, authToken));

                verify(calculationRepository, never()).save(any());
        }

        @Test
        void testCalculateFd_InvalidPrincipalAmount() throws InterruptedException {
                validRequest.setPrincipalAmount(BigDecimal.valueOf(5000));
                doNothing().when(kafkaProducerService).sendCustomerValidationRequest(any());
                doNothing().when(kafkaProducerService).sendProductDetailsRequest(any());
                doNothing().when(requestResponseStore).putRequest(any(), any());

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenAnswer(invocation -> {
                                        String requestId = invocation.getArgument(0);
                                        if (requestId.contains("validation")) {
                                                return CustomerValidationResponse.builder()
                                                                .customerId(1L)
                                                                .valid(true)
                                                                .active(true)
                                                                .requestId(requestId)
                                                                .build();
                                        } else {
                                                return ProductDetailsResponse.builder()
                                                                .productId(1L)
                                                                .productCode("FD-001")
                                                                .minAmount(BigDecimal.valueOf(10000))
                                                                .maxAmount(BigDecimal.valueOf(10000000))
                                                                .minTermMonths(6)
                                                                .maxTermMonths(120)
                                                                .status("ACTIVE")
                                                                .requestId(requestId)
                                                                .build();
                                        }
                                });

                assertThrows(InvalidCalculationDataException.class,
                                () -> calculationService.calculateFd(validRequest, authToken));

                verify(calculationRepository, never()).save(any());
        }

        @Test
        void testCalculateFd_InvalidTenure() throws InterruptedException {
                validRequest.setTenureMonths(3);
                doNothing().when(kafkaProducerService).sendCustomerValidationRequest(any());
                doNothing().when(kafkaProducerService).sendProductDetailsRequest(any());
                doNothing().when(requestResponseStore).putRequest(any(), any());

                when(requestResponseStore.getResponse(any(), anyLong(), any(TimeUnit.class)))
                                .thenAnswer(invocation -> {
                                        String requestId = invocation.getArgument(0);
                                        if (requestId.contains("validation")) {
                                                return CustomerValidationResponse.builder()
                                                                .customerId(1L)
                                                                .valid(true)
                                                                .active(true)
                                                                .requestId(requestId)
                                                                .build();
                                        } else {
                                                return ProductDetailsResponse.builder()
                                                                .productId(1L)
                                                                .productCode("FD-001")
                                                                .minAmount(BigDecimal.valueOf(10000))
                                                                .maxAmount(BigDecimal.valueOf(10000000))
                                                                .minTermMonths(6)
                                                                .maxTermMonths(120)
                                                                .status("ACTIVE")
                                                                .requestId(requestId)
                                                                .build();
                                        }
                                });

                assertThrows(InvalidCalculationDataException.class,
                                () -> calculationService.calculateFd(validRequest, authToken));

                verify(calculationRepository, never()).save(any());
        }

        @Test
        void testGetCalculationById_Success() {
                when(calculationRepository.findById(eq(1L))).thenReturn(Optional.of(savedCalculation));

                FdCalculationResponse response = calculationService.getCalculationById(1L, authToken);

                assertNotNull(response);
                assertEquals(1L, response.getId());
                assertEquals("FD-001", response.getProductCode());

                verify(calculationRepository).findById(eq(1L));
        }

        @Test
        void testGetCalculationById_NotFound() {
                when(calculationRepository.findById(eq(1L))).thenReturn(Optional.empty());

                assertThrows(CalculationNotFoundException.class,
                                () -> calculationService.getCalculationById(1L, authToken));

                verify(calculationRepository).findById(eq(1L));
        }

        @Test
        void testGetCalculationHistory_Success() {
                List<FdCalculation> calculations = Arrays.asList(savedCalculation, savedCalculation);
                when(calculationRepository.findByCustomerIdOrderByCreatedAtDesc(eq(1L))).thenReturn(calculations);

                List<FdCalculationResponse> response = calculationService.getCalculationHistory(1L, authToken);

                assertNotNull(response);
                assertEquals(2, response.size());

                verify(calculationRepository).findByCustomerIdOrderByCreatedAtDesc(eq(1L));
        }

        @Test
        void testGetRecentCalculations_Success() {
                List<FdCalculation> calculations = Arrays.asList(savedCalculation);
                when(calculationRepository.findRecentCalculationsByCustomer(eq(1L), any(LocalDateTime.class)))
                                .thenReturn(calculations);

                List<FdCalculationResponse> response = calculationService.getRecentCalculations(1L, 30, authToken);

                assertNotNull(response);
                assertEquals(1, response.size());

                verify(calculationRepository).findRecentCalculationsByCustomer(eq(1L), any(LocalDateTime.class));
        }
}
