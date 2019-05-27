package com.jason.nettydemo.utils;

import android.content.Context;

import java.io.*;

/**
 * @author Jason
 * @description:
 * @date :2019/5/5 7:52 PM
 */
public class FileUtil {
    public static String getStringFromAssets(Context context, String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            StringBuilder Result = new StringBuilder();
            while ((line = bufReader.readLine()) != null)
                Result.append(line);
            return Result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getByteFromAssets(Context context, String fileName) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(fileName.length());
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(context.getResources().getAssets().open(fileName));
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert in != null;
                in.close();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
