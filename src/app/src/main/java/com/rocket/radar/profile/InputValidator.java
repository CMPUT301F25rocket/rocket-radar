package com.rocket.radar.profile;

import android.util.Patterns;

import com.google.android.material.textfield.TextInputEditText;


// Email validation is based on https://stackoverflow.com/questions/1819142/how-should-i-validate-an-e-mail-address
// Phone validation is based on https://stackoverflow.com/a/23675722
public class InputValidator {

    // empty emails and phones are valid since they are optional in the user story
    public static boolean isValidEmail(String email) {
        return email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone.isEmpty() || Patterns.PHONE.matcher(phone).matches();
    }

    public static String cleanText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}

