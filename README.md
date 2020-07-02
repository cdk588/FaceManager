# FaceManagerpublic class MainActivity extends AppCompatActivity {

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    private FaceEngine faceEngine = new FaceEngine();
    private String TAG = "MainActivity";
    private EditText mEdt_emp_code;
    private TextView tv_emp_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEdt_emp_code = findViewById(R.id.edt_emp_code);
        tv_emp_id = findViewById(R.id.tv_emp_id);
    }

    /**
     * 激活引擎
     *
     * @param view
     */
    public void activeEngine(final View view) {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        if (view != null) {
            view.setClickable(false);
        }
        ThreadUtils.runOnSubThread(new Runnable() {
            @Override
            public void run() {
                final int activeCode = faceEngine.activeOnline(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                ThreadUtils.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (activeCode == ErrorInfo.MOK) {
                            Toast.makeText(MainActivity.this, "激活成功", Toast.LENGTH_SHORT).show();
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            Toast.makeText(MainActivity.this, "引擎已激活，无需再次激活", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "引擎激活失败，错误码为 " + activeCode, Toast.LENGTH_SHORT).show();
                        }

                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = faceEngine.getActiveFileInfo(MainActivity.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                        }
                    }
                });
            }
        });
    }

    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    public void jumpToFaceRecognizeActivity(View view) {
        Intent intent = new Intent(getApplicationContext(), MyRegisterAndRecognizeActivity.class);
        intent.putExtra("useName", mEdt_emp_code.getText().toString());
        startActivityForResult(intent, 1);
//        startActivity(new Intent(this, RegisterAndRecognizeActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 3) {
            String userName = data.getStringExtra("useName");
            tv_emp_id.setText("注册成功:"+userName);
        }
    }

    public void jumpToFaceManagerActivity(View view) {
        Intent intent = new Intent(getApplicationContext(), FaceManageActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
