package com.desafio.sea.domain.dto.solicitation;

import com.desafio.sea.domain.enums.Priority;
import com.desafio.sea.domain.enums.ServiceType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitationDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidStepDtos() {
        assertTrue(validator.validate(new SolicitationStep1DTO(
                ServiceType.INSTALLATION, "Instalação solar", "Descrição válida para instalação solar."
        )).isEmpty());
        assertTrue(validator.validate(new SolicitationStep2DTO("70040-900", "100", null)).isEmpty());
        assertTrue(validator.validate(new SolicitationStep3DTO(
                Priority.MEDIUM, LocalDate.now(), BigDecimal.ZERO, true
        )).isEmpty());
    }

    @Test
    void shouldRejectInvalidStep1Fields() {
        Set<String> fields = invalidFields(new SolicitationStep1DTO(null, "ab", "curta"));

        assertEquals(Set.of("serviceType", "title", "description"), fields);
    }

    @Test
    void shouldRejectInvalidStep2Fields() {
        Set<String> fields = invalidFields(new SolicitationStep2DTO("123", "", "x".repeat(101)));

        assertEquals(Set.of("cep", "number", "complement"), fields);
    }

    @Test
    void shouldRejectInvalidStep3Fields() {
        Set<String> fields = invalidFields(new SolicitationStep3DTO(
                null, LocalDate.now().minusDays(1), new BigDecimal("-0.01"), false
        ));

        assertEquals(Set.of("priority", "preferredDate", "estimatedValue", "termsAccepted"), fields);
    }

    private Set<String> invalidFields(Object dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
