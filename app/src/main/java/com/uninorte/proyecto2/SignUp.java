package com.uninorte.proyecto2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    Button btnSignup;
    EditText input_email,input_pass;
    RelativeLayout activity_sign_up;
    DatabaseReference mDatabase;


    private ProgressDialog progressDialog;

    private FirebaseAuth auth;
    Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //-------------------------------------------------------------------
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUp.this,Principal.class));
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        //----------------------------------------------------------------------

        //progress
        progressDialog = new ProgressDialog(this,R.style.AppTheme_Dark_Dialog);

        //View
        btnSignup = (Button)findViewById(R.id.signup_btn_register);
        input_email = (EditText)findViewById(R.id.signup_email);
        input_pass = (EditText)findViewById(R.id.signup_password);
        activity_sign_up = (RelativeLayout)findViewById(R.id.activity_sign_up);

        btnSignup.setOnClickListener(this);


        //Init Firebase
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View view) {
             if(view.getId() == R.id.signup_btn_register){
              signUpUser(input_email.getText().toString(),input_pass.getText().toString());
            }
    }

    private void signUpUser(final String email, String password) {

        if(TextUtils.isEmpty(email)|| TextUtils.isEmpty(password)){
            Toast.makeText(SignUp.this,"Hay campos vacios",Toast.LENGTH_SHORT).show();

            //  input_email.setError("No puede estar vacio");
            //input_password.setError("No Puede Estar Vacio");

        }else{
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Creando usuario...");
            progressDialog.show();

        auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful())
                        {
                            snackbar = Snackbar.make(activity_sign_up,"Error: "+task.getException(), Snackbar.LENGTH_SHORT);
                            snackbar.show();
                            progressDialog.dismiss();

                        }
                        else{
                            mDatabase = FirebaseDatabase.getInstance().getReference("users");
                            String userId = mDatabase.push().getKey();
                            User user= new  User("Vendedor",email);
                            mDatabase.child(userId).setValue(user);

                            Toast.makeText(SignUp.this,"Registro exitoso! ",Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            //finish();

                        }
                    }
                });
    }
    }


}
