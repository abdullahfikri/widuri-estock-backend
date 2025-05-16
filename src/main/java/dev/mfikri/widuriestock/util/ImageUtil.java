package dev.mfikri.widuriestock.util;

import java.util.ArrayList;
import java.util.List;

public class ImageUtil {
    public static boolean isImage (String contentType) {
        List<String> allowedImage = new ArrayList<>(List.of("image/png", "image/jpeg", "image/webp", "image/avif", "image/jpg"));

        return allowedImage.contains(contentType);
    }
}
