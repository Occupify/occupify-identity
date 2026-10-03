package com.occupify.identity.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

@Controller
@Hidden
public class DocsController {

    private static final String SCALAR_HTML = """
            <!doctype html>
            <html lang="en">
              <head>
                <title>Occupify Identity API Docs (Scalar)</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <link rel="icon" type="image/svg+xml" href="https://scalar.com/favicon.svg" />
                <style>
                  body {
                    margin: 0;
                    padding: 0;
                  }
                </style>
              </head>
              <body>
                <script
                  id="api-reference"
                  data-url="/v3/api-docs"
                  data-configuration='{
                    "theme": "purple",
                    "layout": "modern",
                    "showSidebar": true,
                    "searchHotKey": "k",
                    "hideDownloadButton": false
                  }'></script>
                <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
              </body>
            </html>
            """;

    @GetMapping(value = {"/scalar", "/docs"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Mono<String> getScalarDocs() {
        return Mono.just(SCALAR_HTML);
    }
}
