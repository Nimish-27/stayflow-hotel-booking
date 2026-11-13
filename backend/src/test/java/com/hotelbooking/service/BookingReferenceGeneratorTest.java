package com.hotelbooking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingReferenceGeneratorTest {

    private final BookingReferenceGenerator generator = new BookingReferenceGenerator();

    @Test
    void referenceHasPrefixAndEightReadableCharacters() {
        assertThat(generator.generate()).matches("HB-[A-HJ-NP-Z2-9]{8}");
    }

    @Test
    void referenceNeverContainsConfusingCharacters() {
        for (int i = 0; i < 500; i++) {
            assertThat(generator.generate().substring(3)).doesNotContainPattern("[01OI]");
        }
    }

    @Test
    void generatedReferencesAreDifferent() {
        Set<String> references = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            references.add(generator.generate());
        }

        assertThat(references).hasSize(1000);
    }
}
