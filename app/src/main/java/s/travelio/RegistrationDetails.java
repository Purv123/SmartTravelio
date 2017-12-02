package s.travelio;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RegistrationDetails extends AppCompatActivity implements View.OnClickListener {

    DatabaseReference databaseArtists;
    EditText userid;
    EditText username;
    EditText mPhoneNumberField;
    EditText mVerificationField;
    EditText email;
    EditText pass;
    EditText confirmpass;
    RadioGroup radioGroup;
    Boolean flag = false;
    Button register;
    Button mStartButton, mVerifyButton, mResendButton;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    String mVerificationId;

    private static final String TAG = "RegistrationDetails";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_registration_details);
        databaseArtists = FirebaseDatabase.getInstance().getReference("UserRegistration");

        userid = (EditText) findViewById(R.id.userid);
        username = (EditText) findViewById(R.id.username);

        mPhoneNumberField = (EditText) findViewById(R.id.mobile);
        mVerificationField = (EditText) findViewById(R.id.mobileotp);

        mStartButton = (Button) findViewById(R.id.sendotp);
        mVerifyButton = (Button) findViewById(R.id.verifyotp);
        mResendButton = (Button) findViewById(R.id.resendotp);

        email = (EditText) findViewById(R.id.email);
        radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

        pass = (EditText) findViewById(R.id.pass);
        confirmpass = (EditText) findViewById(R.id.confirmpass);
        register = (Button) findViewById(R.id.register);

        mStartButton.setOnClickListener(this);
        mVerifyButton.setOnClickListener(this);
        mResendButton.setOnClickListener(this);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(userid.getText().toString())) {
                    userid.setError("Enter User ID");
                    return;
                }
                if (userid.getText().length() != 12) {
                    userid.setError("User ID length must be 12");
                    return;
                }
                if (TextUtils.isEmpty(username.getText().toString())) {
                    username.setError("Enter User Name");
                    return;
                }
                if (username.getText().length() >= 10) {
                    username.setError("Username max length is 10");
                    return;
                }
                if (TextUtils.isEmpty(mPhoneNumberField.getText().toString())) {
                    mPhoneNumberField.setError("Enter Mobile Number");
                    return;
                }
                if (mPhoneNumberField.length() != 10) {
                    mPhoneNumberField.setError("Type Mobile Number Properly");
                    return;
                }

                if (TextUtils.isEmpty(pass.getText().toString())) {
                    pass.setError("Enter Your Password");
                    return;
                }
                if (TextUtils.isEmpty(confirmpass.getText().toString())) {
                    confirmpass.setError("Enter Confirm Password");
                    return;
                }
                if (pass.getText().toString().compareToIgnoreCase(confirmpass.getText().toString()) != 0) {
                    confirmpass.setError("Password and Confirm Password are Different");
                    return;
                }
                if(flag == false){
                    mPhoneNumberField.setError("Verify Phone Number First");
                    return ;
                }

                boolean result = Verhoeff.validateVerhoeff(userid.getText().toString());
                String validate = String.valueOf(result);
                if(validate=="true"){
                    Toast.makeText(register.getContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                    String rk = random();
                    addDatabase(rk);
                    //flag = true;
                    startActivity(new Intent(RegistrationDetails.this, PhoneAuthActivity.class));
                }else{
                    Toast.makeText(getApplicationContext(),"Enter True Aadhar Number",Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mPhoneNumberField.setError("Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                mVerificationId = verificationId;
                mResendToken = token;

            }
        };
    }

    private void addDatabase (String random1){
        String userId1 = userid.getText().toString().trim();
        String userName1 = username.getText().toString().trim();
        String MPhoneNumberField1 =  "+91"+mPhoneNumberField.getText().toString().trim();
        String EMail1 = email.getText().toString().trim();
        String Student1 = ((RadioButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString().trim();
        String Password1 = pass.getText().toString().trim();

        if(!TextUtils.isEmpty(userId1)){
            AddLocationInDatabase addLocationInDatabase = new AddLocationInDatabase(userId1,userName1,MPhoneNumberField1,EMail1,Student1,Password1,random1);
            databaseArtists.child(MPhoneNumberField1).setValue(addLocationInDatabase);
            Toast.makeText(this,"Added in database successfully",Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this,"Error While Adding into Database",Toast.LENGTH_LONG).show();
        }
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        //int randomLength = generator.nextInt(8);
        char tempChar;
        for (int i = 0; i < 16 ; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            Toast.makeText(RegistrationDetails.this,"Profile Verified",Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                            flag = true;
                        }
                        else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                mVerificationField.setError("Invalid code.");
                            }
                        }
                    }
                });
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber,PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber ) || mPhoneNumberField.length() != 10) {
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sendotp:
                if (!validatePhoneNumber()) {
                    return;
                }
                startPhoneNumberVerification(mPhoneNumberField.getText().toString());
                break;

            case R.id.verifyotp:

                String phoneNumber = mPhoneNumberField.getText().toString();
                if (TextUtils.isEmpty(phoneNumber) || mPhoneNumberField.length() != 10) {
                    mPhoneNumberField.setError("Type phone number Properly.");
                    return ;
                }

                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                }
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;

            case R.id.resendotp:
                phoneNumber = mPhoneNumberField.getText().toString();
                if (TextUtils.isEmpty(phoneNumber) || mPhoneNumberField.length() != 10) {
                    mPhoneNumberField.setError("Type phone number Properly.");
                    return ;
                }

                resendVerificationCode(mPhoneNumberField.getText().toString(), mResendToken);
                break;
        }
    }
}