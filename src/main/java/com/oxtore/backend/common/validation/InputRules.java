package com.oxtore.backend.common.validation;

import com.oxtore.backend.common.error.ApiException;

import java.util.regex.Pattern;

/**
 * Server-side copy of the app's form rules (libs/shared/data-access validators), so they can't be bypassed:
 * <ul>
 *   <li>person name: Unicode letters and marks (Arabic, French accents...), spaces, apostrophes, dots, hyphens;
 *       starts with a letter; no digits;</li>
 *   <li>shop name: letters, digits and {@code ' . , & ( ) -}, with at least one letter;</li>
 *   <li>phone: optional leading +, digits with spaces / dots / dashes / parentheses, 8 to 15 digits; never letters.</li>
 * </ul>
 * The *_REGEX constants are for {@code @Pattern} on request DTOs; the methods for hand-parsed bodies.
 */
public final class InputRules {

    /** Surrounding blanks are tolerated (values are trimmed before being stored). */
    public static final String PERSON_NAME_REGEX = "^\\s*[\\p{L}\\p{M}][\\p{L}\\p{M}' .’-]*\\s*$";
    public static final String PHONE_REGEX = "^\\s*\\+?(?:[ .()\\-]*\\d){8,15}[ .()\\-]*\\s*$";

    public static final String PERSON_NAME_MESSAGE = "Name may only contain letters, spaces, apostrophes, dots and hyphens";
    public static final String PHONE_MESSAGE = "Phone number must contain 8 to 15 digits and no letters";
    public static final String SHOP_NAME_MESSAGE = "Shop name may only contain letters, digits and ' . , & ( ) -";

    private static final Pattern PERSON_NAME = Pattern.compile(PERSON_NAME_REGEX);
    private static final Pattern PHONE = Pattern.compile(PHONE_REGEX);
    private static final Pattern SHOP_NAME = Pattern.compile("^[\\p{L}\\p{M}\\p{N}' .,&()’-]+$");
    private static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");

    private InputRules() {}

    public static boolean isPersonName(String value) {
        return value != null && PERSON_NAME.matcher(value).matches();
    }

    public static boolean isPhone(String value) {
        return value != null && PHONE.matcher(value).matches();
    }

    public static boolean isShopName(String value) {
        return value != null && SHOP_NAME.matcher(value.trim()).matches() && HAS_LETTER.matcher(value).find();
    }

    /** "+216 20 123 456" → "+21620123456" (the format the app sends). */
    public static String normalizePhone(String value) {
        String trimmed = value.trim();
        String digits = trimmed.replaceAll("\\D", "");
        return trimmed.startsWith("+") ? "+" + digits : digits;
    }

    /** Trimmed person name; 400 VALIDATION_ERROR on {@code field} if missing (when required) or invalid. */
    public static String personName(String value, String field, String label, boolean required) {
        String v = trimToNull(value);
        if (v == null) {
            if (required) throw ApiException.validation(field, label + " is required");
            return null;
        }
        if (v.length() < 2 || v.length() > 100) {
            throw ApiException.validation(field, label + " must be between 2 and 100 characters");
        }
        if (!isPersonName(v)) throw ApiException.validation(field, PERSON_NAME_MESSAGE);
        return v;
    }

    /** Normalised phone; 400 VALIDATION_ERROR on {@code field} if missing (when required) or invalid. */
    public static String phone(String value, String field, boolean required) {
        String v = trimToNull(value);
        if (v == null) {
            if (required) throw ApiException.validation(field, "Phone number is required");
            return null;
        }
        if (!isPhone(v)) throw ApiException.validation(field, PHONE_MESSAGE);
        return normalizePhone(v);
    }

    public static String shopName(String value, String field, boolean required) {
        String v = trimToNull(value);
        if (v == null) {
            if (required) throw ApiException.validation(field, "Shop name is required");
            return null;
        }
        if (v.length() < 2 || v.length() > 100) {
            throw ApiException.validation(field, "Shop name must be between 2 and 100 characters");
        }
        if (!isShopName(v)) throw ApiException.validation(field, SHOP_NAME_MESSAGE);
        return v;
    }

    /** Trimmed free text, null when blank; 400 VALIDATION_ERROR when required and missing or longer than {@code max}. */
    public static String text(String value, String field, String label, int max, boolean required) {
        String v = trimToNull(value);
        if (v == null) {
            if (required) throw ApiException.validation(field, label + " is required");
            return null;
        }
        if (v.length() > max) throw ApiException.validation(field, label + " is too long (max " + max + ")");
        return v;
    }

    public static String trimToNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }
}
