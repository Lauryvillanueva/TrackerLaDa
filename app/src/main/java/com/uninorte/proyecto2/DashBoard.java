package com.uninorte.proyecto2;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;

public class DashBoard extends AppCompatActivity implements View.OnClickListener{

    private TextView txtWelcome;
   private Button btnLogout,btnMap;
    private RelativeLayout activity_dashboard;

    private FirebaseAuth auth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);



        //View
        txtWelcome = (TextView) findViewById(R.id.dashboard_welcome);


        btnLogout = (Button) findViewById(R.id.dashboard_btn_logout);
        btnMap = (Button) findViewById(R.id.dashboard_btn_mapa);
        activity_dashboard = (RelativeLayout) findViewById(R.id.activity_dash_board);


        btnLogout.setOnClickListener(this);
        btnMap.setOnClickListener(this);

        //Init Firebase

        auth = FirebaseAuth.getInstance();

        //Session check
        if (auth.getCurrentUser() != null) {
            txtWelcome.setText("Bienvenido , " + auth.getCurrentUser().getEmail());
        }




    }
    //-----------------------------------------


    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.dashboard_btn_logout) {
            logoutUser();
        }else
        if(view.getId() == R.id.dashboard_btn_mapa) {
            Intent i = new Intent(this, MapsActivity.class);
            startActivity(i);
        }




    }

    private void logoutUser() {
        auth.signOut();
        if(auth.getCurrentUser() == null)
        {
            startActivity(new Intent(DashBoard.this,Principal.class));
            finish();
        }
    }




}
