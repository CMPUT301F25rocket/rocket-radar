package com.rocket.radar.profile;

import android.util.Patterns;

import com.google.android.material.textfield.TextInputEditText;


// Email validation is based on https://stackoverflow.com/questions/1819142/how-should-i-validate-an-e-mail-address, accessed: November 4, 2025
// Phone validation is based on https://stackoverflow.com/a/23675722, accessed: November 4, 2025

/**
 * This class is a utility for validating input in account settings.
 * Especially for phone and email. It also checks for null strings.
 */
public class InputValidator {
    /**
     * This function checks if an email is valid.
     * @param email the email to check, which is a String
     * @return True if the email is empty (since its optional) or matches the builtin pattern, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * This function checks if an phone number is valid.
     * @param phone the phone number to check, which is a String
     * @return True if the phone is empty (since its optional) or matches the builtin pattern, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        return phone.isEmpty() || Patterns.PHONE.matcher(phone).matches();
    }

    /**
     * This function returns the trimmed text from a TextInputEditText field.
     * If the field's text is null, returns an empty string.
     * @param field the TextInputEditText to get Text from
     * @return the trimmed text as a String, empty if null
     */
    public static String cleanText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}

