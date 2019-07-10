package com.joe.springrediscachequickstart.pagecache;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceType;
import org.springframework.mobile.device.DeviceUtils;

import com.github.choelea.pagecache.AbstractPageCachingFilter;
import com.github.choelea.pagecache.PageInfo;


public class SimplePageCachingFilter extends AbstractPageCachingFilter {

	private CacheManager cacheManager;	
	
	public SimplePageCachingFilter(CacheManager cacheManager) {
		super();
		this.cacheManager = cacheManager;
	}

	@Override
	protected Boolean isCacheable(HttpServletRequest httpRequest) {
		return true;
	}

	@Override
	protected String calculateKey(HttpServletRequest request) {
		List<String> elements = new ArrayList<>();
		elements.add(request.getMethod());
		elements.add(request.getRequestURI());
		elements.add(request.getQueryString());
		Device device = DeviceUtils.getCurrentDevice(request);
		if (device == null) {
			elements.add("UNKNOWN");
		} else if (device.isNormal()) {
			elements.add(String.valueOf(DeviceType.NORMAL));
		} else if (device.isMobile()) {
			elements.add(String.valueOf(DeviceType.MOBILE));
		} else if (device.isTablet()) {
			elements.add(String.valueOf(DeviceType.TABLET));
		}
		elements.add(request.getHeader("Accept"));
		return StringUtils.join(elements, "|");
	}

	
	@Override
	public PageInfo getPageInfo(String cacheName, String key) {
		Cache cache = cacheManager.getCache(cacheName);
        ValueWrapper element = cache.get(key);
		return element==null?null:(PageInfo)element.get();
	}
	@Override
	public void putPageInfo(String cacheName, String key, PageInfo pageInfo) {
		Cache cache = cacheManager.getCache(cacheName);
		cache.put(key, pageInfo);
	}

	@Override
	protected String getCacheName() {
		return "pageCache";
	}

}
