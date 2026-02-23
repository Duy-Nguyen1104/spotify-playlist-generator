package com.example.playlistgenerator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {"/", "/{path:(?!api|actuator)[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
