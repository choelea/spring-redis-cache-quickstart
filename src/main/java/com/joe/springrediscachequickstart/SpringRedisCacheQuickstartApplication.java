package com.joe.springrediscachequickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.joe.springrediscachequickstart.filter.PageCacheFilter;

@SpringBootApplication
@EnableCaching
public class SpringRedisCacheQuickstartApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringRedisCacheQuickstartApplication.class, args);
	}

	@Bean
	public FilterRegistrationBean<PageCacheFilter> abcFilter(CacheManager cacheManager) {
		FilterRegistrationBean<PageCacheFilter> filterRegBean = new FilterRegistrationBean<>();
		filterRegBean.setFilter(new PageCacheFilter(cacheManager));
		filterRegBean.addUrlPatterns("/cache");
		filterRegBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		return filterRegBean;
	}
}
