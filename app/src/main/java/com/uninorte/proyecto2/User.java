package com.uninorte.proyecto2;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by LauryV on 07/05/2017.
 */

@IgnoreExtraProperties
public class User {
    private String email;
    private String role;

    public User(){

    }

    public User(String role,String email){
        this.role = role;
        this.email = email;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
