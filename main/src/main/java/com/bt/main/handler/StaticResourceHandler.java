package com.bt.main.handler;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class StaticResourceHandler {

    public Mono<ServerResponse> serveIndex(ServerRequest request) {
        Resource resource = new ClassPathResource("static/index.html");

        if (!resource.exists()) {
            return ServerResponse.notFound().build();
        }

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noCache())
                .body(BodyInserters.fromResource(resource));
    }

    public Mono<ServerResponse> serveStaticResource(ServerRequest request) {
        String path = request.path();
        String resourcePath = "static" + path;
        Resource resource = new ClassPathResource(resourcePath);

        if (!resource.exists()) {
            return ServerResponse.notFound().build();
        }

        MediaType mediaType = determineMediaType(path);
        CacheControl cacheControl = path.startsWith("/assets/")
                ? CacheControl.maxAge(Duration.ofHours(1)).cachePublic()
                : CacheControl.noCache();

        return ServerResponse.ok()
                .contentType(mediaType)
                .cacheControl(cacheControl)
                .body(BodyInserters.fromResource(resource));
    }

    private MediaType determineMediaType(String path) {
        if (path.endsWith(".js"))
            return MediaType.valueOf("application/javascript");
        if (path.endsWith(".css"))
            return MediaType.valueOf("text/css");
        if (path.endsWith(".html"))
            return MediaType.TEXT_HTML;
        if (path.endsWith(".json"))
            return MediaType.APPLICATION_JSON;
        if (path.endsWith(".png"))
            return MediaType.IMAGE_PNG;
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return MediaType.IMAGE_JPEG;
        if (path.endsWith(".svg"))
            return MediaType.valueOf("image/svg+xml");
        if (path.endsWith(".ico"))
            return MediaType.valueOf("image/x-icon");
        if (path.endsWith(".woff") || path.endsWith(".woff2"))
            return MediaType.valueOf("font/woff2");
        if (path.endsWith(".ttf"))
            return MediaType.valueOf("font/ttf");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
