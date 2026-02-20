package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dummy")
public class DummyUserController {

    @Autowired
    private DummyUserService service;

    @GetMapping("")
    public String showDummyPage(Model model) {
        model.addAttribute("users", service.getAllUsers());
        return "users";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String nama) {
        service.registerUser(nama);
        return "redirect:/dummy";
    }
}
