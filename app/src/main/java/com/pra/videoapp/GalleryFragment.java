package com.pra.videoapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.pra.videoapp.interFace.PermissionAllGranted;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static android.app.Activity.RESULT_OK;

public class GalleryFragment extends Fragment implements PermissionAllGranted {

    RadioButton mRbMale, mRbFemale;
    private String imageFilePath;
    private CircleImageView mCircleImageView, mDynamicImage;
    final private int REQUEST_CAPTURE_IMAGE = 0;
    final private int REQUEST_GALLERY = 1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mView = inflater.inflate(R.layout.fragment_gallery, container, false);
        initComponents(mView);
        setActionListener();
        return mView;
    }

    private void initComponents(View mView) {
        mCircleImageView =  mView.findViewById(R.id.profile_image);
        mDynamicImage = mView.findViewById(R.id.dynamic_image);

        mRbMale = mView.findViewById(R.id.rb_male);
        mRbFemale = mView.findViewById(R.id.rb_female);
        mRbMale.setChecked(true);
        mRbMale.setClickable(true);
        mRbFemale.setClickable(false);


        setImageFromUrlForProfilepic(getActivity(),mDynamicImage,"https://randomuser.me/api/portraits/women/73.jpg");
    }


    public static void setImageFromUrlForProfilepic(Context context, CircleImageView imageView, String url) {
        if (url.length() == 0) {
            url = "temp";
        }
        Picasso.get().
                load(url).
                placeholder(R.drawable.ic_default_user).
                error(R.drawable.ic_default_user).
                into(imageView);
    }


    private void setActionListener() {
        mCircleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTimePermissionForCameraGallery();
            }
        });
    }

    private void runTimePermissionForCameraGallery() {
        List<String> mListPermission = new ArrayList<>();
        mListPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        mListPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Utils.requestingMultiplePermission(getActivity(), mListPermission, this);
    }


    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    openCameraIntent();
                } else if (options[item].equals("Choose from Gallery")) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, REQUEST_GALLERY);
                } else {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    File mFileUPload;

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {

        switch (requestCode) {
            case REQUEST_CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {
                    if (imageReturnedIntent == null || imageReturnedIntent.getData() == null) {
                        mFileUPload = new File(imageFilePath);
                        mCircleImageView.setImageURI(Uri.parse(imageFilePath));
                    } else {
                        mFileUPload = new File(getPath(imageReturnedIntent.getData()));
                        mCircleImageView.setImageURI(imageReturnedIntent.getData());
                    }
                }
                break;
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    mCircleImageView.setImageURI(selectedImage);
                    mFileUPload = new File(getPath(selectedImage));
                }
                break;
        }
    }


    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s = cursor.getString(column_index);
        cursor.close();
        return s;
    }

    private void getMultipart(Uri mUri) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), mFileUPload);
        MultipartBody.Part body = MultipartBody.Part.createFormData("Image", mFileUPload.getName(), requestBody);
    }


    @Override
    public void allPermissionGranted() {
        selectImage();
    }


    private void openCameraIntent() {
        Intent pictureIntent = new Intent(
                MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Uri photoURI = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", photoFile);
                    pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            photoURI);
                    startActivityForResult(pictureIntent,
                            REQUEST_CAPTURE_IMAGE);
                } else {
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePicture, REQUEST_CAPTURE_IMAGE);//zero can be replaced with any action code

                }

            }
        }
    }


    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
                getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        imageFilePath = image.getAbsolutePath();
        return image;
    }


   /* private static boolean isImgRotationRequired(Context context, Uri selectedImage) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        return (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_180 || orientation == ExifInterface.ORIENTATION_ROTATE_270);
    }*/

   /* private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }*/

   /* private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }*/

/*
    private Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }*/

}
