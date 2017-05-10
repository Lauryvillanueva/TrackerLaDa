package com.uninorte.proyecto2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class Principal extends AppCompatActivity implements View.OnClickListener{
    Button btnLogin,btnSignup;


    ScrollView activity_main;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        //View
        btnLogin = (Button)findViewById(R.id.login_btn_login);
        btnSignup = (Button) findViewById(R.id.login_btn_signup);

        btnSignup.setOnClickListener(this);
        btnLogin.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.login_btn_login)
        {
            Intent i = new Intent(Principal.this, Login.class);
            startActivity(i);
        }
        else if(v.getId() == R.id.login_btn_signup)
        {
            Intent i = new Intent(Principal.this, SignUp.class);
            startActivity(i);
        }
    }
}
