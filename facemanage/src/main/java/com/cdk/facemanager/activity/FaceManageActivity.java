package com.cdk.facemanager.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arcsoft.face.util.ImageUtils;
import com.cdk.facemanager.R;
import com.cdk.facemanager.faceserver.FaceServer;
import com.cdk.facemanager.util.LogUtils;
import com.cdk.facemanager.util.ThreadUtils;
import com.cdk.facemanager.widget.ProgressDialog;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 批量注册页面
 */
public class FaceManageActivity extends AppCompatActivity {
    //注册图所在的目录
    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "arcface";
//    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "arcfacedemo";
    private static final String REGISTER_DIR = ROOT_DIR + File.separator + "register";
    private static final String REGISTER_FAILED_DIR = ROOT_DIR + File.separator + "failed";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private TextView tvNotificationRegisterResult;

    ProgressDialog progressDialog = null;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private String mCardCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_manage);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tvNotificationRegisterResult = findViewById(R.id.notification_register_result);
        progressDialog = new ProgressDialog(this);
        FaceServer.getInstance().init(this);
        createSDCardDir();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FaceServer.getInstance().activeEngine(FaceManageActivity.this);
    }

    @Override
    protected void onDestroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        FaceServer.getInstance().unInit();
        super.onDestroy();
    }

    public void batchRegister(View view) {
        if (checkPermissions(NEEDED_PERMISSIONS)) {
            doRegister();
        } else {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        }
    }

    private void doRegister() {
        File dir = new File(REGISTER_DIR);
        if (!dir.exists()) {
            Toast.makeText(this, "path \n" + REGISTER_DIR + "\n is not exists", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!dir.isDirectory()) {
            Toast.makeText(this, "path \n" + REGISTER_DIR + "\n is not a directory", Toast.LENGTH_SHORT).show();
            return;
        }
        final File[] jpgFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(FaceServer.IMG_SUFFIX);
            }
        });
        executorService.execute(new Runnable() {
            @Override
            public void run() {



                final int totalCount = jpgFiles.length;

                int successCount = 0;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setMaxProgress(totalCount);
                        progressDialog.show();
                        tvNotificationRegisterResult.setText("");
                        tvNotificationRegisterResult.append("process start,please wait\n\n");
                    }
                });
                for (int i = 0; i < totalCount; i++) {
                    final int finalI = i;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null) {
                                progressDialog.refreshProgress(finalI);
                            }
                        }
                    });
                    final File jpgFile = jpgFiles[i];
                    Bitmap bitmap = BitmapFactory.decodeFile(jpgFile.getAbsolutePath());
                    if (bitmap == null) {
                        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                        if (!failedFile.getParentFile().exists()) {
                            failedFile.getParentFile().mkdirs();
                        }
                        jpgFile.renameTo(failedFile);
                        continue;
                    }
                    bitmap = ImageUtils.alignBitmapForBgr24(bitmap);
                    if (bitmap == null) {
                        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                        if (!failedFile.getParentFile().exists()) {
                            failedFile.getParentFile().mkdirs();
                        }
                        jpgFile.renameTo(failedFile);
                        continue;
                    }
                    byte[] bgr24 = ImageUtils.bitmapToBgr24(bitmap);
                    boolean success = FaceServer.getInstance().registerBgr24(FaceManageActivity.this, bgr24, bitmap.getWidth(), bitmap.getHeight(),
                            jpgFile.getName().substring(0, jpgFile.getName().lastIndexOf(".")));
                    if (!success) {
                        Toast.makeText(FaceManageActivity.this, "有照片注册识别", Toast.LENGTH_SHORT).show();
                        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
                        if (!failedFile.getParentFile().exists()) {
                            failedFile.getParentFile().mkdirs();
                        }
                        jpgFile.renameTo(failedFile);
                    } else {
                        successCount++;
                    }
                }
                final int finalSuccessCount = successCount;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        tvNotificationRegisterResult.append("process finished!\ntotal count = " + totalCount + "\nsuccess count = " + finalSuccessCount + "\nfailed count = " + (totalCount - finalSuccessCount)
                                + "\nfailed images are in directory '" + REGISTER_FAILED_DIR + "'");
                    }
                });
                Log.i(FaceManageActivity.class.getSimpleName(), "run: " + executorService.isShutdown());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                doRegister();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this.getApplicationContext(), neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    public void clearFaces(View view) {
        int faceNum = FaceServer.getInstance().getFaceNumber(this);
        if (faceNum == 0) {
            Toast.makeText(this, R.string.no_face_need_to_delete, Toast.LENGTH_SHORT).show();
        } else {
            final EditText password = new EditText(FaceManageActivity.this);
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.notification)
                    .setView(password).setCancelable(false)
                    .setMessage(getString(R.string.confirm_delete, faceNum))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!password.getText().toString().isEmpty()) {
                                if (!password.getText().toString().equals("40068000")) {
                                    Toast.makeText(FaceManageActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                                } else {
                                    int deleteCount = FaceServer.getInstance().clearAllFaces(FaceManageActivity.this);
                                    Toast.makeText(FaceManageActivity.this, deleteCount + " faces cleared!", Toast.LENGTH_SHORT).show();
                                }
                            }

                        }
                    })
                    .setNegativeButton("取消", null)
                    .create();
            dialog.show();
        }
    }

    /**
     * 新建视频播放文件夹 在SD卡上创建一个文件夹
     */
    public void createSDCardDir() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // 创建一个文件夹对象，赋值为外部存储器的目录
            File path = new File(REGISTER_DIR);
            if (!path.exists()) {
                //若不存在，创建目录，可以在应用启动的时候创建
                path.mkdirs();
                LogUtils.e("paht ok,path:" + REGISTER_DIR);
            }
        } else {
            LogUtils.e("creatDriFalse");
        }
    }

    public void backClicked(View view) {
        finish();
    }

    public void oneRegister(View view) {
        final EditText password = new EditText(FaceManageActivity.this);
        password.setInputType(InputType.TYPE_CLASS_TEXT);
        final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(FaceManageActivity.this).setTitle("请输入工号:")
                .setView(password).setCancelable(false)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!password.getText().toString().isEmpty()) {

                            Intent intent = new Intent(getApplicationContext(), MyRegisterAndRecognizeActivity.class);
                            mCardCode = password.getText().toString();
                            intent.putExtra("useName", password.getText().toString());
                            startActivityForResult(intent, 1);

                        }
                    }
                }).setNegativeButton("取消", null);
        dialog.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 3) {
            // TODO: 2020/7/1  校验数据
        }
    }
}
