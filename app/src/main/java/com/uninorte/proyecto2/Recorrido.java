package com.uninorte.proyecto2;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by daniel on 7/05/17.
 */
@IgnoreExtraProperties
public class Recorrido {
    private String Nombre;
    private String User_id;

    public Recorrido() {

    }

    public Recorrido(String nombre, String user_id) {
        Nombre = nombre;
        User_id = user_id;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getUser_id() {
        return User_id;
    }

    public void setUser_id(String user_id) {
        User_id = user_id;
    }
}
