package com.cdk.facemanager.common;

public class Constants {


    /**
     * IR预览数据相对于RGB预览数据的横向偏移量，注意：是预览数据，一般的摄像头的预览数据都是 width > height
     */
    public static final int HORIZONTAL_OFFSET = 0;
    /**
     * IR预览数据相对于RGB预览数据的纵向偏移量，注意：是预览数据，一般的摄像头的预览数据都是 width > height
     */
    public static final int VERTICAL_OFFSET = 0;
    public static String APP_ID;
    public static String SDK_KEY;

    public static void init(String app_id, String sdk_key){
        APP_ID  = app_id;
        SDK_KEY  = sdk_key;
    };
}
