package com.uninorte.proyecto2;


import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login extends AppCompatActivity implements View.OnClickListener {

    Button btnLogin;
    EditText input_email,input_password;
    TextView btnSignup,btnForgotPass;

    ScrollView activity_main;

    private ProgressDialog progressDialog;

    private FirebaseAuth auth;

    //base de datos Firebase
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    DatabaseReference user = ref.child("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //-------------------------------------------------------------------
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this,Principal.class));
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        //----------------------------------------------------------------------
        FirebaseApp.initializeApp(this);

        //progress
        progressDialog = new ProgressDialog(this,R.style.AppTheme_Dark_Dialog);

        //View
        btnLogin = (Button)findViewById(R.id.login_btn_login);
        input_email = (EditText)findViewById(R.id.login_email);
        input_password = (EditText)findViewById(R.id.login_password);
        btnSignup = (TextView)findViewById(R.id.login_btn_signup);
        btnForgotPass = (TextView)findViewById(R.id.login_btn_forgot_password);


        String email = input_email.getText().toString();
        String password = input_password.getText().toString();

        btnSignup.setOnClickListener(this);
        btnForgotPass.setOnClickListener(this);
        btnLogin.setOnClickListener(this);

        //Init Firebase Auth
        auth = FirebaseAuth.getInstance();

        //Check already session , if ok-> DashBoard
        if(auth.getCurrentUser() != null)
            startActivity(new Intent(Login.this,DashBoard.class));
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.login_btn_forgot_password)
        {
            startActivity(new Intent(Login.this,ForgotPassword.class));
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
        else if(view.getId() == R.id.login_btn_signup)
        {
            startActivity(new Intent(Login.this,SignUp.class));
            finish();
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
        else if(view.getId() == R.id.login_btn_login)
        {
            loginUser(input_email.getText().toString(),input_password.getText().toString());
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
    }

    private void loginUser(final String email, final String password) {



        if(TextUtils.isEmpty(email)|| TextUtils.isEmpty(password)){
            Toast.makeText(Login.this,"Hay campos vacios",Toast.LENGTH_SHORT).show();
            //  input_email.setError("No puede estar vacio");
            //input_password.setError("No Puede Estar Vacio");

        }else{

            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Autenticando...");
            progressDialog.show();

                auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if(!task.isSuccessful())
                        {
                            if(password.length() < 6)
                            {
                                Toast.makeText(Login.this,"La contraseña debe tener mas de 6 caracteres",Toast.LENGTH_SHORT).show();


                            }else  if (!isValidEmail(email)) {
                                Toast.makeText(Login.this,"Email no valido",Toast.LENGTH_SHORT).show();

                            }else {
                                Toast.makeText(Login.this,"Email o contraseña incorrecta! Verifiquelo e intente de nuevo",Toast.LENGTH_SHORT).show();

                            }
                        }
                        else{

                            startActivity(new Intent(Login.this,DashBoard.class));
                            //guarda el id en la base de datos-------


                        }
                    }
                });
    }}

    // validating email id
    private boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }



}
