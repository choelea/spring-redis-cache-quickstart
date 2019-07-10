package com.joe.springrediscachequickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.joe.springrediscachequickstart.pagecache.SimplePageCachingFilter;

@SpringBootApplication
@EnableCaching
public class SpringRedisCacheQuickstartApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringRedisCacheQuickstartApplication.class, args);
	}

//	@Bean
//	public FilterRegistrationBean<PageCacheFilter> pageStringCacheFilter(CacheManager cacheManager) {
//		FilterRegistrationBean<PageCacheFilter> filterRegBean = new FilterRegistrationBean<>();
//		filterRegBean.setFilter(new PageCacheFilter(cacheManager));
//		filterRegBean.addUrlPatterns("/cache");
//		filterRegBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
//		return filterRegBean;
//	}
	@Bean
	public FilterRegistrationBean<SimplePageCachingFilter> pageCacheFilter(CacheManager cacheManager) {
		FilterRegistrationBean<SimplePageCachingFilter> filterRegBean = new FilterRegistrationBean<>();
		filterRegBean.setFilter(new SimplePageCachingFilter(cacheManager));
		filterRegBean.addUrlPatterns("/cache");
		filterRegBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
		return filterRegBean;
	}
	
}
