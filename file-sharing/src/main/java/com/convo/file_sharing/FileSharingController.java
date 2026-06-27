package com.convo.file_sharing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileSharingController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Convo File Sharing Service!";
    }
}
