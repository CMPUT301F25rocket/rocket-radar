package com.rocket.radar.profile;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.Timestamp;

public class ProfileModel {
    private String uid;
    private String name;
    private String phoneNumber;
    private String email;
    private Timestamp lastLogin;

    public ProfileModel() {}

    public ProfileModel(String uid, String name, String email, String phoneNumber, Timestamp lastLogin) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.lastLogin = lastLogin;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }

}
