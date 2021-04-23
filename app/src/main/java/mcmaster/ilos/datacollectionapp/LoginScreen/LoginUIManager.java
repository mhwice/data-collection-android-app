package mcmaster.ilos.datacollectionapp.LoginScreen;

import android.graphics.Typeface;
import android.util.Log;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import mcmaster.ilos.datacollectionapp.R;

class LoginUIManager {

    private LoginActivity activity;

    LoginUIManager(LoginActivity activity) {
        this.activity = activity;
    }

    void setInitialUI() {

        Typeface avenirnext_demibold;
        Typeface avenirnext_heavy;
        Typeface avenirnext_medium;

        try {
            avenirnext_demibold = Typeface.createFromAsset(activity.getAssets(), "fonts/AvenirNext-DemiBold.ttf");
            avenirnext_heavy = Typeface.createFromAsset(activity.getAssets(), "fonts/AvenirNext-Heavy.ttf");
            avenirnext_medium = Typeface.createFromAsset(activity.getAssets(), "fonts/AvenirNext-Medium.ttf");
        } catch (Exception e) {
            Log.e("CRASH", "Could not load fonts", e);
            return;
        }

        TextView learnMoreText = activity.findViewById(R.id.learnMoreText);
        learnMoreText.setTypeface(avenirnext_demibold);

        TextView forgotPasswordText = activity.findViewById(R.id.forgotPasswordText);
        forgotPasswordText.setTypeface(avenirnext_demibold);

        TextView emailText = activity.findViewById(R.id.emailText);
        emailText.setTypeface(avenirnext_medium);

        TextView passwordText = activity.findViewById(R.id.passwordText);
        passwordText.setTypeface(avenirnext_medium);

        TextView ilosText = activity.findViewById(R.id.ilosText);
        ilosText.setTypeface(avenirnext_heavy);

        Button getStartedButton = activity.findViewById(R.id.getStartedButton);
        getStartedButton.setTypeface(avenirnext_demibold);

        EditText emailEditText = activity.findViewById(R.id.emailEditText);
        emailEditText.setTypeface(avenirnext_demibold);

        EditText passwordEditText = activity.findViewById(R.id.passwordEditText);
        passwordEditText.setTypeface(avenirnext_demibold);
    }

    TranslateAnimation shakeError() {
        TranslateAnimation shake = new TranslateAnimation(0, 30, 0, 0);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(7));
        return shake;
    }
}
