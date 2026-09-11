package dev.ktr.shot;

import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class ScreenshotStore {
    static Uri save(Context context, ScreenshotResult result) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        Bitmap wrapped = null;
        Bitmap bitmap = null;
        try (HardwareBuffer buffer = result.getHardwareBuffer()) {
            wrapped = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
            if (wrapped == null) throw new IOException("无法读取截图缓冲区");
            bitmap = wrapped.copy(Bitmap.Config.ARGB_8888, false);
            if (bitmap == null) throw new IOException("无法创建截图位图");
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "KTR_" + stamp + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KTRShots");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("无法创建相册文件");
            try (OutputStream output = resolver.openOutputStream(uri)) {
                if (output == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw new IOException("PNG 写入失败");
                }
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(uri, published, null, null) != 1) {
                throw new IOException("相册发布失败");
            }
            return uri;
        } catch (Exception failure) {
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null);
                } catch (Exception cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw new IOException(failure.getMessage(), failure);
        } finally {
            if (bitmap != null) bitmap.recycle();
            if (wrapped != null) wrapped.recycle();
        }
    }
}