package com.tencent.qcloud.suixinbo.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.qcloud.suixinbo.R;
import com.tencent.qcloud.suixinbo.model.MyCurrentLiveInfo;
import com.tencent.qcloud.suixinbo.presenters.LocationHelper;
import com.tencent.qcloud.suixinbo.model.MySelfInfo;
import com.tencent.qcloud.suixinbo.presenters.PublishHelper;
import com.tencent.qcloud.suixinbo.presenters.viewinface.LocationView;
import com.tencent.qcloud.suixinbo.utils.Constants;
import com.tencent.qcloud.suixinbo.views.customviews.CustomSwitch;

import java.io.File;
import java.io.IOException;

/**
 * Created by admin on 16/4/21.
 */
public class PublishLiveActivity extends Activity implements View.OnClickListener, LocationView {
    private PublishHelper mPublishLivePresenter;
    private LocationHelper mLocationHelper;
    private TextView BtnBack, BtnPublish;
    private Dialog mPicChsDialog;
    private ImageView cover;
    private Uri fileUri;
    private TextView tvLBS;
    private TextView tvTitle;
    private CustomSwitch btnLBS;
    private static final int CAPTURE_IMAGE_CAMERA = 100;
    private static final int IMAGE_STORE = 200;
    private static final String TAG = PublishLiveActivity.class.getSimpleName();

    private static final int CROP_CHOOSE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_publish);
        mPublishLivePresenter = new PublishHelper(this);
        mLocationHelper = new LocationHelper(this);
        tvTitle = (TextView) findViewById(R.id.live_title);
        BtnBack = (TextView) findViewById(R.id.btn_cancel);
        BtnPublish = (TextView) findViewById(R.id.btn_publish);
        cover = (ImageView) findViewById(R.id.cover);
        tvLBS = (TextView)findViewById(R.id.address);
        btnLBS = (CustomSwitch)findViewById(R.id.btn_lbs);
        cover.setOnClickListener(this);
        BtnBack.setOnClickListener(this);
        BtnPublish.setOnClickListener(this);
        btnLBS.setOnClickListener(this);

        initPhotoDialog();

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel:
                finish();
                break;
            case R.id.btn_publish:
                mPublishLivePresenter.uploadCover();
                Intent intent = new Intent(this, LiveActivity.class);
                intent.putExtra(Constants.ID_STATUS, Constants.HOST);
                MySelfInfo.getInstance().setIdStatus(Constants.HOST);
                MyCurrentLiveInfo.setTitle(tvTitle.getText().toString());
                MyCurrentLiveInfo.setHostID(MySelfInfo.getInstance().getId());
                MyCurrentLiveInfo.setRoomNum(MySelfInfo.getInstance().getMyRoomNum());
                startActivity(intent);
                this.finish();
                break;
            case R.id.cover:
                mPicChsDialog.show();
                break;
            case R.id.btn_lbs:
                if (btnLBS.getChecked()){
                    btnLBS.setChecked(false, true);
                    tvLBS.setText(R.string.text_live_close_lbs);
                }else{
                    btnLBS.setChecked(true, true);
                    tvLBS.setText(R.string.text_live_location);
                    if (mLocationHelper.checkPermission()){
                        if (!mLocationHelper.getMyLocation(getApplicationContext(), this)){
                            tvLBS.setText(getString(R.string.text_live_lbs_fail));
                            btnLBS.setChecked(false, false);
                        }
                    }
                }
                break;
        }
    }

    /**
     * 图片选择对话框
     */
    private void initPhotoDialog() {
        mPicChsDialog = new Dialog(this, R.style.dialog);
        mPicChsDialog.setContentView(R.layout.pic_choose);

        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Window dlgwin = mPicChsDialog.getWindow();
        WindowManager.LayoutParams lp = dlgwin.getAttributes();
        dlgwin.setGravity(Gravity.BOTTOM);
        lp.width = (int)(display.getWidth()); //设置宽度

        mPicChsDialog.getWindow().setAttributes(lp);

        TextView camera = (TextView) mPicChsDialog.findViewById(R.id.chos_camera);
        TextView picLib = (TextView) mPicChsDialog.findViewById(R.id.pic_lib);
        TextView cancel = (TextView) mPicChsDialog.findViewById(R.id.btn_cancel);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPicFrom(CAPTURE_IMAGE_CAMERA);
                mPicChsDialog.dismiss();
            }
        });

        picLib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPicFrom(IMAGE_STORE);
                mPicChsDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPicChsDialog.dismiss();
            }
        });
    }


    /**
     * 获取图片资源
     *
     * @param type
     */
    private void getPicFrom(int type) {
        switch (type) {
            case CAPTURE_IMAGE_CAMERA:
                Intent intent_photo = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent_photo.putExtra(MediaStore.EXTRA_OUTPUT, createCoverUri());
                startActivityForResult(intent_photo, CAPTURE_IMAGE_CAMERA);
                break;
            case IMAGE_STORE:
                Intent intent_album = new Intent("android.intent.action.GET_CONTENT");
                intent_album.setType("image/*");
                startActivityForResult(intent_album, IMAGE_STORE);
                break;

        }
    }

    private Uri createCoverUri() {
        String filename = MySelfInfo.getInstance().getId() + ".jpg";
        File outputImage = new File(Environment.getExternalStorageDirectory(), filename);
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileUri = Uri.fromFile(outputImage);
        return fileUri;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("XIAO", "onActivityResult->result: "+resultCode+", req code:"+requestCode);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAPTURE_IMAGE_CAMERA:
                    startPhotoZoom(fileUri);
                    break;
                case IMAGE_STORE:
                    startPhotoZoom(data.getData());
                    break;
                case CROP_CHOOSE:
                    cover.setImageURI(data.getData());
                    break;

            }
        }

    }

    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        Log.e("XIAO", "zoom url: " + uri);
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
//        intent.putExtra("return-data", true);
//        intent.putExtra("output", fileUri);
        startActivityForResult(intent, CROP_CHOOSE);
    }

    @Override
    public void onLocationChanged(int code, double lat1, double long1, String location) {
        if (btnLBS.getChecked()) {
            if (0 == code) {
                tvLBS.setText(location);
                MyCurrentLiveInfo.setLat1(lat1);
                MyCurrentLiveInfo.setLong1(long1);
                MyCurrentLiveInfo.setAddress(location);
            } else {
                tvLBS.setText(getString(R.string.text_live_lbs_fail));
            }
        }else{
            MyCurrentLiveInfo.setLat1(0);
            MyCurrentLiveInfo.setLong1(0);
            MyCurrentLiveInfo.setAddress("");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
        case Constants.LOCATION_PERMISSION_REQ_CODE:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (!mLocationHelper.getMyLocation(getApplicationContext(), this)){
                    tvLBS.setText(getString(R.string.text_live_lbs_fail));
                    btnLBS.setChecked(false, false);
                }
            }
            break;
        default:
            break;
        }
    }
}
