package com.joe.springrediscachequickstart.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

public class PageCacheFilter extends OncePerRequestFilter{

	private static final Logger LOG = LoggerFactory.getLogger(PageCacheFilter.class);

	private CacheManager cacheManager;
	
	public PageCacheFilter(CacheManager cacheManager) {
		super();
		this.cacheManager = cacheManager;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		HttpServletResponse resp = (HttpServletResponse) response;
        String html = getHtmlFromCache();
        if (null == html) {
        	LOG.info("Cannot find from cache-------------");
            ResponseWrapper wrapper = new ResponseWrapper(resp); 
            filterChain.doFilter(request, wrapper);
            html = wrapper.getResult();
            putIntoCache(html);
 
        }
        resp.setContentType("text/html; charset=utf-8");
        resp.getWriter().print(html);
	}
	 
	
 
     
 
    private String getHtmlFromCache() {
    	ValueWrapper  vw = cacheManager.getCache("pageCache").get("index");
    	return vw==null?null:(String)vw.get();
    }
 
    private void putIntoCache(String html) {
    	cacheManager.getCache("pageCache").put("index", html);
    }
}
