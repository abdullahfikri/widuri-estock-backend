package dev.mfikri.widuriestock.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ImageUtil {
    public static boolean isImage (String contentType) {
        List<String> allowedImage = new ArrayList<>(List.of("image/png", "image/jpeg", "image/webp", "image/avif", "image/jpg"));

        return allowedImage.contains(contentType);
    }

    public static Path uploadPhoto(MultipartFile photo, String name, boolean isProduct) {
        String contentType = photo.getContentType();
        log.info(contentType);
        if (contentType == null || !isImage(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image photo is not valid");
        }
        String type = photo.getContentType().split("/")[1];

        String folder;
        if (isProduct) {
            folder = "product";
        } else {
            folder = "profile";
        }

        Path path = Path.of("upload/" + folder + "/"+ folder +"-" + name.trim().replaceAll("\\s", "-").toLowerCase() + "." + type);
        try {
            photo.transferTo(path);
            return path;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Server unavailable, try again later");
        }
    }
}
