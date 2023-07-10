package com.fvbox.llk.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 压缩工具类。
 * zip是将文件打包为zip格式的压缩文件。
 * gzip是将文件打包为tar.gz格式的压缩文件。
 * gzip只能对一个文件进行压缩，如果想压缩一大堆文件，就需要使用tar进行打包了。
 */
public class CompressUtils {

    private static final int BUFF_SIZE = 1024;

    /**
     * GZip解压，tar解包
     *
     * @param srcFile
     * @param dstDir
     */
    public static boolean unTar(File srcFile, String dstDir) {
        boolean isSuccess = false;
        File file = new File(dstDir);
        //需要判断该文件存在，且是文件夹
        if (!file.exists() || !file.isDirectory()) file.mkdirs();
        byte[] buffer = new byte[BUFF_SIZE];
        FileInputStream fis = null;
        TarArchiveInputStream tais = null;
        try {
            fis = new FileInputStream(srcFile);
            tais = new TarArchiveInputStream(fis);
            TarArchiveEntry tarArchiveEntry;
            int len;
            while ((tarArchiveEntry = tais.getNextTarEntry()) != null) {
                File f = new File(dstDir + File.separator + tarArchiveEntry.getName());
                if (tarArchiveEntry.isDirectory()) f.mkdirs();
                else {
                    File parent = f.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                    FileOutputStream fos = new FileOutputStream(f);
                    while ((len = tais.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                    fos.close();
                }
            }
            isSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fis != null) fis.close();
                if(tais != null) tais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isSuccess;
    }
}

