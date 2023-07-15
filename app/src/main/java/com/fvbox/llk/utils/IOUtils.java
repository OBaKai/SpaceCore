package com.fvbox.llk.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <p>  </p>
 *
 * @author jiahui
 * @date 2018/2/5
 */
public class IOUtils {
    private IOUtils() {
    }

    public static void close(Closeable... closeables) {
        if (closeables == null || closeables.length == 0) return;
        try {
            for (Closeable closeable : closeables) {
                if (closeable != null)
                    closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制文件夹
     *
     * @param src   原文件夹
     * @param dest   目标文件夹
     */
    public static File copyFolder(File src, String dest) throws IOException {
        File file2 = new File(dest);
        String rootPath = src.getAbsolutePath();
        String finishPath = file2.getAbsolutePath();
        if (!file2.exists()) {
            file2.mkdirs();
        }
        generateAllFile(src, rootPath, finishPath);
        return file2;
    }

    public static void generateAllFile(File src, String rootPath, String finishPath) throws IOException {
        File[] listFiles = src.listFiles();
        for (File file3 : listFiles) {
            if (file3.isDirectory()) {
                String subString = file3.getAbsolutePath().substring(rootPath.length());
                File file = new File(finishPath + subString);
                file.mkdirs();
                generateAllFile(file3, rootPath, finishPath);
            } else {
                String subString = file3.getAbsolutePath().substring(rootPath.length());
                File file = new File(finishPath + subString);
                file.createNewFile();
                copy(file3,file);
            }

        }

    }

    /**
     * @description  复制数据
     * @param src
     */
    public static void copy(File src, File dest) throws IOException {

        FileInputStream fileInputStream = new FileInputStream(src);
        FileOutputStream fileOutputStream = new FileOutputStream(dest);
        byte[] data = new byte[1024 * 8];
        int len = -1;
        while ((len = fileInputStream.read(data)) != -1) {
            fileOutputStream.write(data, 0, len);
        }
        fileInputStream.close();
        fileOutputStream.close();

    }
}
