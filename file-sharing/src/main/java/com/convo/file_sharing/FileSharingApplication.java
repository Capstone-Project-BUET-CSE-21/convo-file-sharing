package com.convo.file_sharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileSharingApplication {
    public static void main(String[] args) {
        System.out.println("Hello from Convo File Sharing Service!");
        SpringApplication.run(FileSharingApplication.class, args);
    }
}
