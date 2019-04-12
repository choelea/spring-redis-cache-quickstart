package com.joe.springrediscachequickstart.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.joe.springrediscachequickstart.entity.Category;

@Service
public class CategoryServiceImpl implements CategoryService{
	Logger LOG = LoggerFactory.getLogger(CategoryServiceImpl.class);

	@Override
	@Cacheable(cacheNames="allCategory")
	public List<Category> findCategories() {
		LOG.info("Normally, there is the logic to retrieve from database");
		List<Category> list = new ArrayList<Category>();
		list.add(new Category(1L, "Mobile"));
		list.add(new Category(2L, "Bike"));
		list.add(new Category(3L, "Car"));
		return list;
	}

	@Override
	@Cacheable(cacheNames="category")
	public List<Category> findCategories(Boolean isEnabled) {
		LOG.info("Normally, there is the logic to retrieve from database");
		List<Category> list = new ArrayList<Category>();
		list.add(new Category(1L, "Mobile"));
		list.add(new Category(2L, "Bike"));
		if(isEnabled) {
			list.add(new Category(3L, "Car"));			
		}
		return list;
	}
	 
}