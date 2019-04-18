package com.joe.springrediscachequickstart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.joe.springrediscachequickstart.entity.Category;
import com.joe.springrediscachequickstart.service.CategoryService;

@Controller
@RequestMapping("/")
public class IndexController {
	@Autowired
	private CategoryService categoryService;

	@GetMapping
	public String index(Model model) {
		model.addAttribute("page", categoryService.findCategories());
		return "index";
	}
	
	@GetMapping("/online")
    public String all(Model model) {
		model.addAttribute("page", categoryService.findCategories(true));
        return "index";
    }
	
	
	@GetMapping("/online/{type}")
    public String all(Model model,Category.Type type) {
		model.addAttribute("page", categoryService.findCategories(type, true));
        return "index";
    }
	
	@GetMapping("/clear")
	@ResponseBody
    public ResponseEntity<?> clean() {
		categoryService.cleanCache();
		return ResponseEntity.ok("success");
    }
	
}
