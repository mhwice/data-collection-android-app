package mcmaster.ilos.datacollectionapp.LoginScreen;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import mcmaster.ilos.datacollectionapp.CustomDataTypes.Credentials;
import mcmaster.ilos.datacollectionapp.MainMenu.MainMenuActivity;
import mcmaster.ilos.datacollectionapp.MapsScreen.MapsActivity;
import mcmaster.ilos.datacollectionapp.R;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private LoginManager loginManager;
    private LoginUIManager loginUIManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(this.getResources().getColor(R.color.skyblue, null));

        loginManager = new LoginManager(this);
        if (loginManager.isLoggedIn()) {
            Intent mainMenuActivity = new Intent(this, MainMenuActivity.class);
            startActivity(mainMenuActivity);
        }

        loginUIManager = new LoginUIManager(this);
        loginUIManager.setInitialUI();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        forgotPasswordText.setOnClickListener((View v) -> {
            Toast.makeText(getApplicationContext(), "Forgot Password?", Toast.LENGTH_SHORT).show();
        });

        TextView learnMoreText = findViewById(R.id.learnMoreText);
        learnMoreText.setOnClickListener((View v) -> {
            Toast.makeText(getApplicationContext(), "Learn More", Toast.LENGTH_SHORT).show();
        });
    }

    public void loginButtonPressed(View view) {

        Credentials credentials = new Credentials(emailEditText.getText().toString(), passwordEditText.getText().toString());

        boolean acceptableEmail = loginManager.isEmailValid(credentials.getEmail());
        boolean acceptablePassword = loginManager.isPasswordValid(credentials.getPassword());
        if (acceptableEmail && acceptablePassword) {
            boolean proceedWithLogin = loginManager.isValidUser(credentials);
            if (proceedWithLogin) {
                try {
                    loginManager.saveCredentials(credentials);
                } catch (Exception e) {
                    Log.e("CRASH", "Failed to save users login credentials", e);
                } finally {
                    Intent mainMenuActivity = new Intent(this, MainMenuActivity.class);
                    startActivity(mainMenuActivity);
                }
            } else {
                emailEditText.startAnimation(loginUIManager.shakeError());
                passwordEditText.startAnimation(loginUIManager.shakeError());
            }
        } else if (acceptableEmail) {
            passwordEditText.startAnimation(loginUIManager.shakeError());
        } else if (acceptablePassword) {
            emailEditText.startAnimation(loginUIManager.shakeError());
        } else {
            emailEditText.startAnimation(loginUIManager.shakeError());
            passwordEditText.startAnimation(loginUIManager.shakeError());
        }
    }
}
