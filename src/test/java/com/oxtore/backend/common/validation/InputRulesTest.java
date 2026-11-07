package com.oxtore.backend.common.validation;

import com.oxtore.backend.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InputRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"محمد أمين", "Élodie Lefèvre", "Jean-Pierre O'Neil", "Sana B.", "N’Golo Kanté"})
    void acceptsUnicodePersonNames(String name) {
        assertTrue(InputRules.isPersonName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "Amine2", "-Amine", " ", "Amine@"})
    void rejectsNamesWithDigitsOrSymbols(String name) {
        assertFalse(InputRules.isPersonName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+21620123456", "20 123 456", "+216 (20) 12-34-56", "07.12.34.56.78"})
    void acceptsPhones(String phone) {
        assertTrue(InputRules.isPhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"20a23456", "+225", "1234567", "1234567890123456", "abc"})
    void rejectsPhonesWithLettersOrWrongLength(String phone) {
        assertFalse(InputRules.isPhone(phone));
    }

    @Test
    void normalisesPhones() {
        assertEquals("+21620123456", InputRules.normalizePhone(" +216 20 123 456 "));
        assertEquals("0712345678", InputRules.normalizePhone("07.12.34.56.78"));
    }

    @Test
    void shopNamesNeedALetter() {
        assertTrue(InputRules.isShopName("Oxtore Tunis Centre"));
        assertTrue(InputRules.isShopName("Auto Plus & Co. (Sfax)"));
        assertFalse(InputRules.isShopName("123"));
        assertFalse(InputRules.isShopName("Shop<script>"));
    }

    @Test
    void reportsTheField() {
        ApiException ex = assertThrows(ApiException.class,
                () -> InputRules.personName("", "customerName", "Full name", true));
        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertEquals("customerName", ex.getField());
        assertEquals("Full name is required", ex.getMessage());
    }
}
