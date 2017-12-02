package s.travelio;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

import static android.graphics.Color.WHITE;

public class ProfileActivity extends AppCompatActivity  {//implements View.OnClickListener

    DatabaseReference databaseLocation;
    DatabaseReference screenChange;

    ImageView image;
    TextView tvEmail;
    private FirebaseAuth mAuth;
    Spinner s;
    String gotrandonString = null;
    String gotuserId = null;
    String qrData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        databaseLocation = FirebaseDatabase.getInstance().getReference("UserRegistration");
        screenChange = FirebaseDatabase.getInstance().getReference("Conductor");
        mAuth = FirebaseAuth.getInstance();
        s = (Spinner)findViewById(R.id.spinner);
        image = (ImageView) findViewById(R.id.image);
        tvEmail = (TextView) findViewById(R.id.tvEmailProfile);

        if (mAuth != null) {
            qrData = tvEmail.getText().toString();
        }
        databaseLocation.orderByChild("UserRegistration").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot databaseSnapshot : dataSnapshot.getChildren()) {

                    AddLocationInDatabase addLocationInDatabase = databaseSnapshot.getValue(AddLocationInDatabase.class);
                    if (mAuth.getCurrentUser().getPhoneNumber().equals(addLocationInDatabase.getPhoneNumber())) {
                        tvEmail.setText(addLocationInDatabase.getUserName());
                        gotrandonString = addLocationInDatabase.getRandomKey();
                        gotuserId = addLocationInDatabase.getUserId();
                        screenChange.orderByChild("Conductor").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot databaseSnapshot : dataSnapshot.getChildren()) {
                                    screenChange sc = databaseSnapshot.getValue(screenChange.class);
                                    if (sc.getCurrentId().equals(mAuth.getCurrentUser().getPhoneNumber())&&sc.getAvailable().equals("true")) {
                                        if (!sc.getCurrentId().equals(null)) {
                                            startActivity(new Intent(ProfileActivity.this, MapsActivity.class));
                                            return;
                                        }
                                        return;
                                    }
                                }
                                try {
                                    //setting size of qr code
                                    int width = 850;
                                    int height = 850;
                                    int smallestDimension = width < height ? width : height;
                                    String qrCodeData = gotrandonString + mAuth.getCurrentUser().getPhoneNumber() + gotuserId;
                                    //setting parameters for qr code
                                    String charset = "UTF-8";
                                    Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();
                                    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
                                    CreateQRCode(qrCodeData, charset, hintMap, smallestDimension, smallestDimension);
                                    return;
                                } catch (Exception ex) {
                                    Log.e("QrGenerate", ex.getMessage());
                                    return;
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(ProfileActivity.this, "Error", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    return;
                }
                return;
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    public  void CreateQRCode(String qrCodeData, String charset, Map hintMap, int qrCodeheight, int qrCodewidth){
        try {
            //generating qr code in bitmatrix type
            BitMatrix matrix = new MultiFormatWriter().encode(new String(qrCodeData.getBytes(charset), charset),
                    BarcodeFormat.QR_CODE, qrCodewidth, qrCodeheight, hintMap);
            //converting bitmatrix to bitmap
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            // All are 0, or black, by default
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    //pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
                    pixels[offset + x] = matrix.get(x, y) ?
                            ResourcesCompat.getColor(getResources(),R.color.qr,null) :WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            //setting bitmap to image view
            Bitmap overlay = BitmapFactory.decodeResource(getResources(), R.drawable.cycle);
            image.setImageBitmap(mergeBitmaps(overlay,bitmap));

        }catch (Exception er){
            Log.e("QrGenerate",er.getMessage());
        }
    }
    public Bitmap mergeBitmaps(Bitmap overlay, Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        Bitmap combined = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Canvas canvas = new Canvas(combined);
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        canvas.drawBitmap(bitmap, new Matrix(), null);
        int centreX = (canvasWidth  - overlay.getWidth()) /2;
        int centreY = (canvasHeight - overlay.getHeight()) /2 ;
        canvas.drawBitmap(overlay, centreX, centreY, null);
        return combined;
    }

    protected void buildAlertMessageNoGps() {

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Please Turn ON your GPS Connection")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle persistableBundle) {
        super.onSaveInstanceState(outState, persistableBundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            Toast.makeText(this,"Pressed Profile Menu",Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_account) {
            Toast.makeText(this,"Pressed Account Details Menu",Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        return;
    }
}