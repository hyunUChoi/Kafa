package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
public class KafkaController {

    @RequestMapping("/{id}")
    public String chattingRoom(@PathVariable String id, HttpSession session, Model model){
        if(id != null && !id.equals("")){
            model.addAttribute("name", id);
        }
        return "chattingRoom";
    }
}