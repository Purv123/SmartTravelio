package s.travelio;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
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

public class scanoutActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 1;
    DatabaseReference databaseLocation;
    DatabaseReference screenChange;
    LocationManager locationManager;
    ImageView image;
    private FirebaseAuth mAuth;
    private static final String TAG = "scanoutActivity";
    DataSnapshot dataSnapshot;
    String gotrandonString = null;
    String gotuserId = null;
    String qrData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanout);
        databaseLocation = FirebaseDatabase.getInstance().getReference("UserRegistration");
        screenChange = FirebaseDatabase.getInstance().getReference("Conductor");
        mAuth = FirebaseAuth.getInstance();
        image = (ImageView) findViewById(R.id.image);

        //Get the bundle
        Bundle bundle = getIntent().getExtras();
        final String message = bundle.getString("message");

        databaseLocation.orderByChild("UserRegistration").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot databaseSnapshot : dataSnapshot.getChildren()) {

                    AddLocationInDatabase addLocationInDatabase = databaseSnapshot.getValue(AddLocationInDatabase.class);
                    if (mAuth.getCurrentUser().getPhoneNumber().equals(addLocationInDatabase.getPhoneNumber())) {
                        gotrandonString = addLocationInDatabase.getRandomKey();
                        gotuserId = addLocationInDatabase.getUserId();
                        screenChange.orderByChild("Conductor").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot databaseSnapshot : dataSnapshot.getChildren()) {
                                    screenChange sc = databaseSnapshot.getValue(screenChange.class);
                                    if (sc.getCurrentId().equals(mAuth.getCurrentUser().getPhoneNumber())) {
                                        if (!sc.getAvailable().equals("true")) {
                                            Intent farr = new Intent(scanoutActivity.this, WalletActivity.class);
                                            farr.putExtra("message", message);
                                            startActivity(farr);
                                            return;
                                        }else {
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
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(scanoutActivity.this, "Error", Toast.LENGTH_LONG).show();
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
    public void onSaveInstanceState(Bundle outState, PersistableBundle persistableBundle) {
        super.onSaveInstanceState(outState, persistableBundle);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        return;
    }
}