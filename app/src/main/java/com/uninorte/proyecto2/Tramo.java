package com.uninorte.proyecto2;

import android.location.Location;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by daniel on 7/05/17.
 */
@IgnoreExtraProperties
public class Tramo {
    private Location posi,posf;


    public Tramo(Location posi, Location posf) {
        this.posi = posi;
        this.posf = posf;
    }

    public Tramo() {
    }

    public Location getPosi() {
        return posi;
    }

    public void setPosi(Location posi) {
        this.posi = posi;
    }

    public Location getPosf() {
        return posf;
    }

    public void setPosf(Location posf) {
        this.posf = posf;
    }


}
