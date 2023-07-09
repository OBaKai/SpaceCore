package com.fvbox.llk.utils;

import java.io.Closeable;
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
}
