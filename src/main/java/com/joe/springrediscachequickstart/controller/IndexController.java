package com.joe.springrediscachequickstart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.joe.springrediscachequickstart.service.CategoryService;

@Controller
@RequestMapping("/")
public class IndexController {
	@Autowired
	private CategoryService categoryService;

	@GetMapping
    public String index(Model model) {
		model.addAttribute("page", categoryService.findCategories(true));
        return "index";
    }
	@GetMapping("/all")
    public String all(Model model,String keyword) {
		model.addAttribute("page", categoryService.findCategories());
        return "index";
    }
	
}
