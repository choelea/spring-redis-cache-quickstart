package com.joe.springrediscachequickstart.service;

import java.util.List;

import com.joe.springrediscachequickstart.entity.Category;

public interface CategoryService {
	List<Category> findCategories();
	List<Category> findCategories(Boolean isEnabled);
	List<Category> findCategories(Category.Type type, Boolean isEnabled);
	
	void cleanCache();
}
