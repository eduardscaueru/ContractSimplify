package com.team.contractsimplify;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private ProgressDialog mProgressDialog = null;
    private TesseractOCR mTessOCR;
    private Context context;
    protected String mCurrentPhotoPath;
    private Uri photoURI1;
    private Uri oldPhotoURI;
    private static final String errorFileCreate = "Error file create!";
    private static final String errorConvert = "Error convert!";
    private static final int REQUEST_IMAGE1_CAPTURE = 1;
    private ImageView firstImage;
    private TextView ocrText, summarizeText;
    private Button scan_button;
    private EditText nr;
    private int nrPages, pageIndex;
    private String srcText = "";
    private String summary = "";

    int PERMISSION_ALL = 1;
    boolean flagPermissions = false;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nr = (EditText) findViewById(R.id.editTextNumber);
        pageIndex = 0;
        firstImage = (ImageView) findViewById(R.id.ocr_image);
        ocrText = (TextView) findViewById(R.id.ocr_text);
        summarizeText = (TextView) findViewById(R.id.summarize_view) ;
        scan_button = (Button) findViewById(R.id.scan_button);
        scan_button.setOnClickListener(new View.OnClickListener() {

           @Override
           public void onClick(View v) {
               nrPages = Integer.parseInt(nr.getText().toString());
               // check permissions
               if (!flagPermissions) {
                   checkPermissions();
                   return;
               }
               //prepare intent
               Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
               while (pageIndex < nrPages) { pageIndex++;
               if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
                   File photoFile = null;
                   try {
                       photoFile = createImageFile();
                   } catch (IOException ex) {
                       Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                       Log.i("File error", ex.toString());
                   }
                   // Continue only if the File was successfully created
                   if (photoFile != null) {
                       oldPhotoURI = photoURI1;
                       photoURI1 = Uri.fromFile(photoFile);
                       takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI1);
                       startActivityForResult(takePictureIntent, REQUEST_IMAGE1_CAPTURE);
                   }
               }}
           }
        });
        context = MainActivity.this;
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (!flagPermissions) {
            checkPermissions();
        }
        String language = "eng";
        mTessOCR = new TesseractOCR(this, language);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void checkPermissions() {
        if (!hasPermissions(context, PERMISSIONS)) {
            requestPermissions(PERMISSIONS,
                    PERMISSION_ALL);
            flagPermissions = false;
        }
        flagPermissions = true;
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_IMAGE1_CAPTURE: {
                if (resultCode == RESULT_OK) {
                    Bitmap bmp = null;
                    try {
                        InputStream is = context.getContentResolver().openInputStream(photoURI1);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        bmp = BitmapFactory.decodeStream(is, null, options);

                    } catch (Exception ex) {
                        Log.i(getClass().getSimpleName(), ex.getMessage());
                        Toast.makeText(context, errorConvert, Toast.LENGTH_SHORT).show();
                    }

                    firstImage.setImageBitmap(bmp);
                    doOCR(bmp);

                    OutputStream os;
                    try {
                        os = new FileOutputStream(photoURI1.getPath());
                        if (bmp != null) {
                            bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                        }
                        os.flush();
                        os.close();
                    } catch (Exception ex) {
                        Log.e(getClass().getSimpleName(), ex.getMessage());
                        Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    {
                        photoURI1 = oldPhotoURI;
                        firstImage.setImageURI(photoURI1);
                    }
                }
            }
        }
    }

    private void doOCR(final Bitmap bitmap) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Doing OCR...", true);
        } else {
            mProgressDialog.show();
        }
        new Thread(new Runnable() {
            public void run() {
                srcText += mTessOCR.getOCRResult(bitmap);
                InputStream input = new ByteArrayInputStream(srcText.getBytes());
                final String summarized;
                SummaryTool summary = new SummaryTool();
                summary.in = input;
                summarized = summary.printsummary();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (srcText != null && !srcText.equals("")) {
                            ocrText.setText(srcText);
                        }
                        if (summarized != null && !summarized.equals("")) {
                            summarizeText.setText(summarized);
                        }
                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }

}
