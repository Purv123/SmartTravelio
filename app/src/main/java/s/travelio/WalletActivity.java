package s.travelio;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class WalletActivity extends AppCompatActivity {

    TextView ShowDistanceDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        ShowDistanceDuration = (TextView) findViewById(R.id.show_distance_time);

        Bundle bundle = getIntent().getExtras();
        final String message = bundle.getString("message");
        ShowDistanceDuration.setText("Fare is " + message + " Rs.");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        return;
    }
}