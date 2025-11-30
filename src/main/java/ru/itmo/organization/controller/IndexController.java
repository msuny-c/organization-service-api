package ru.itmo.organization.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping(value = {"/", "/organizations", "/organizations/**", 
                         "/addresses", "/addresses/**", 
                         "/coordinates", "/coordinates/**",
                         "/locations", "/locations/**",
                         "/create", "/create/**"})
    public String index() {
        return "forward:/index.html";
    }
}
