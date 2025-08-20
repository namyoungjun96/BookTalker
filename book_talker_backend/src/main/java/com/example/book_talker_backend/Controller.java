package com.example.book_talker_backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class Controller {

    @GetMapping
    public String hello() {
        return "Hello World";
    }

    @GetMapping(value = "/save/{name}")
    public String save(HttpServletRequest request, @PathVariable String name) {
        HttpSession session = request.getSession();
        session.setAttribute("name", name);
        return "success";
    }

    @GetMapping(value = "/session-info")
    public String sessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return session.getAttribute("name").toString();
    }
}
