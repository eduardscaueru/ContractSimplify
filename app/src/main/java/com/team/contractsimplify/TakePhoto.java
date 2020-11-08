package com.team.contractsimplify;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class TakePhoto extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private EditText numberPages;
    private int nrPages, pageIndex;
    protected String mCurrentPhotoPath;
    private ProgressDialog mProgressDialog = null;
    private Context context;
    private TesseractOCR mTessOCR;
    private Uri oldPhotoURI, photoURI1;
    private static final String errorFileCreate = "Error file create!";
    private static final String errorConvert = "Error convert!";
    private TextView result;
    private ImageView firstImage;

    private Button submitbutton;

    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    Uri image_uri;

    //@RequiresApi(api = Build.VERSION_CODES.M)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        result = (TextView) findViewById(R.id.result);
        firstImage = (ImageView) findViewById(R.id.ocr_image);

        numberPages = (EditText) findViewById(R.id.insertPageNumbers);
        pageIndex = 0;

        submitbutton = (Button) findViewById(R.id.confirm);

        context = TakePhoto.this;

        String language = "eng";
        mTessOCR = new TesseractOCR(this, language);

        submitbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrPages = Integer.parseInt(numberPages.getText().toString());
                //if system os is >= marshmallow, request runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED) {
                        //permission not enabled, request it
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        //show popup to request permissions
                        requestPermissions(permission, PERMISSION_CODE);
                    } else {
                        //permission already granted
                        openCamera();
                    }
                } else {
                    //system os < marshmallow
                    openCamera();
                }
            }
        });
    }

    //handling permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission from popup was granted
                openCamera();
            } else {
                //permission from popup was denied
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //called when image was captured from camera
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            /*Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            /*Bitmap bmp = null;
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
            }*/
            //set the image captured to our ImageView
            //firstImage.setImageURI(image_uri);
        } /*else {
            {
                photoURI1 = oldPhotoURI;
                firstImage.setImageURI(photoURI1);
            }
        }*/
    }

    private void openCamera() {
        while (pageIndex < nrPages) {
            pageIndex++;
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
            image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Bitmap bitmap = decodeUriToBitmap(this.context, image_uri);
            /*try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
                doOCR(bitmap);
                OutputStream os;
                try {
                    os = new FileOutputStream(photoURI1.getPath());
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    }
                    os.flush();
                    os.close();
                } catch (Exception ex) {
                    Log.e(getClass().getSimpleName(), ex.getMessage());
                    Toast.makeText(context, errorFileCreate, Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            //Camera intent
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
            /*if (cameraIntent.resolveActivity(context.getPackageManager()) != null) {
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
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI1);
                    startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
                }
            }*/
            startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
        }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
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

    private void doOCR(final Bitmap bitmap) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Doing OCR...", true);
        } else {
            mProgressDialog.show();
        }
        new Thread(new Runnable() {
            public void run() {
                final String srcText = mTessOCR.getOCRResult(bitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (srcText != null && !srcText.equals("")) {
                            result.setText(srcText);
                        }
                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }

    public static Bitmap decodeUriToBitmap(Context mContext, Uri sendUri) {
        Bitmap getBitmap = null;
        InputStream image_stream;
        try {
            image_stream = mContext.getContentResolver().openInputStream(sendUri);
            getBitmap = BitmapFactory.decodeStream(image_stream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return getBitmap;
    }

}