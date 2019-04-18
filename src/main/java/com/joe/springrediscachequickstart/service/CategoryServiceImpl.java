package com.joe.springrediscachequickstart.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.stereotype.Service;

import com.joe.springrediscachequickstart.entity.Category;
import com.joe.springrediscachequickstart.entity.Category.Type;

@Service
public class CategoryServiceImpl implements CategoryService{
	Logger LOG = LoggerFactory.getLogger(CategoryServiceImpl.class);

	@TimeToLive
	private Long expiration = 6000L;
	
	@Override
	@Cacheable(cacheNames="category")
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

	@Override
	@Cacheable(cacheNames="category")	 
	public List<Category> findCategories(Type type, Boolean isEnabled) {
		LOG.info("Normally, there is the logic to retrieve from database");
		List<Category> list = new ArrayList<Category>();
		list.add(new Category(1L, "Mobile", type));
		list.add(new Category(2L, "Bike", type));
		list.add(new Category(3L, "Car", type));
		return list;
	}

	@Override
	@CacheEvict(cacheNames="category")
	public void cleanCache() {
		LOG.info("Clean all cache using annotation: @CacheEvict");		
	}
	 
	
}
