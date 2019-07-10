package com.joe.springrediscachequickstart.pagecache;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceType;
import org.springframework.mobile.device.DeviceUtils;


public class SimplePageCachingFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(SimplePageCachingFilter.class);
	private CacheManager cacheManager;	
	/**
	 * The cache name can be set through init parameters. If it is set it is
	 * stored here.
	 */
	protected String cacheName = "pageCache";
	
	public SimplePageCachingFilter(CacheManager cacheManager) {
		super();
		this.cacheManager = cacheManager;
	}

    

    /**
     * Build page info either using the cache or building the page directly.
     * <p/>
     * Some requests are for page fragments which should never be gzipped, or
     * for other pages which are not gzipped.
     */
    protected PageInfo buildPageInfo(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain chain)
            throws Exception {
    	// Look up the cached page
        final String key = calculateKey(request);
        PageInfo pageInfo = null;
        try {
//            checkNoReentry(request);
        	Cache cache = getCacheManager().getCache(getCacheName());
            ValueWrapper element = getCacheManager().getCache(getCacheName()).get(key);
            if (element == null || element.get() == null) {
                try {
                    // Page is not cached - build the response, cache it, and
                    // send to client
                    pageInfo = buildPage(request, response, chain);
                    if (pageInfo.isOk()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("PageInfo ok. Adding to cache "
                                    + getCacheName() + " with key "
                                    + key);
                        }
                        cache.put(key, pageInfo);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("PageInfo was not ok(200). Putting null into cache "
                                    + getCacheName()
                                    + " with key "
                                    + key);
                        }
                    }
                } catch (final Throwable throwable) {
                    // Must unlock the cache if the above fails. Will be logged
                    // at Filter
                    throw new Exception(throwable);
                }
            } else {
                pageInfo = (PageInfo) element.get();
            }
        } catch (Exception e) {
            // do not release the lock, because you never acquired it
            throw e;
        } finally {
            // all done building page, reset the re-entrant flag
//            visitLog.clear();
        }
        return pageInfo;
    }

    /**
     * Builds the PageInfo object by passing the request along the filter chain
     * 
     * @param request
     * @param response
     * @param chain
     * @return a Serializable value object for the page or page fragment
     * @throws AlreadyGzippedException
     *             if an attempt is made to double gzip the body
     * @throws Exception
     */
    protected PageInfo buildPage(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain chain)
            throws AlreadyGzippedException, Exception {

        // Invoke the next entity in the chain
        final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        final GenericResponseWrapper wrapper = new GenericResponseWrapper(
                response, outstr);
        chain.doFilter(request, wrapper);
        wrapper.flush();

        long timeToLiveSeconds = 360000l;

        // Return the page info
        return new PageInfo(wrapper.getStatus(), wrapper.getContentType(),
                wrapper.getCookies(), outstr.toByteArray(), true,
                timeToLiveSeconds, wrapper.getAllHeaders());
    }

    /**
     * Writes the response from a PageInfo object.
     * <p/>
     * Headers are set last so that there is an opportunity to override
     * 
     * @param request
     * @param response
     * @param pageInfo
     * @throws IOException
     * @throws DataFormatException
     * @throws ResponseHeadersNotModifiableException
     * 
     */
    protected void writeResponse(final HttpServletRequest request,
            final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, DataFormatException,
            ResponseHeadersNotModifiableException {
        boolean requestAcceptsGzipEncoding = acceptsGzipEncoding(request);

        setStatus(response, pageInfo);
        setContentType(response, pageInfo);
        setCookies(pageInfo, response);
        // do headers last so that users can override with their own header sets
        setHeaders(pageInfo, requestAcceptsGzipEncoding, response);
        writeContent(request, response, pageInfo);
    }

    /**
     * Set the content type.
     * 
     * @param response
     * @param pageInfo
     */
    protected void setContentType(final HttpServletResponse response,
            final PageInfo pageInfo) {
        String contentType = pageInfo.getContentType();
        if (contentType != null && contentType.length() > 0) {
            response.setContentType(contentType);
        }
    }

    /**
     * Set the serializableCookies
     * 
     * @param pageInfo
     * @param response
     */
    protected void setCookies(final PageInfo pageInfo,
            final HttpServletResponse response) {

        final Collection<SerializableCookie> cookies = pageInfo.getSerializableCookies();
        for (Iterator<SerializableCookie> iterator = cookies.iterator(); iterator.hasNext();) {
            final Cookie cookie = ((SerializableCookie) iterator.next())
                    .toCookie();
            response.addCookie(cookie);
        }
    }

    /**
     * Status code
     * 
     * @param response
     * @param pageInfo
     */
    protected void setStatus(final HttpServletResponse response,
            final PageInfo pageInfo) {
        response.setStatus(pageInfo.getStatusCode());
    }

    /**
     * Set the headers in the response object, excluding the Gzip header
     * 
     * @param pageInfo
     * @param requestAcceptsGzipEncoding
     * @param response
     */
    protected void setHeaders(final PageInfo pageInfo,
            boolean requestAcceptsGzipEncoding,
            final HttpServletResponse response) {

        final Collection<Header<? extends Serializable>> headers = pageInfo
                .getHeaders();

        // Track which headers have been set so all headers of the same name
        // after the first are added
        final TreeSet<String> setHeaders = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);

        for (final Header<? extends Serializable> header : headers) {
            final String name = header.getName();

            switch (header.getType()) {
            case STRING:
                if (setHeaders.contains(name)) {
                    response.addHeader(name, (String) header.getValue());
                } else {
                    setHeaders.add(name);
                    response.setHeader(name, (String) header.getValue());
                }
                break;
            case DATE:
                if (setHeaders.contains(name)) {
                    response.addDateHeader(name, (Long) header.getValue());
                } else {
                    setHeaders.add(name);
                    response.setDateHeader(name, (Long) header.getValue());
                }
                break;
            case INT:
                if (setHeaders.contains(name)) {
                    response.addIntHeader(name, (Integer) header.getValue());
                } else {
                    setHeaders.add(name);
                    response.setIntHeader(name, (Integer) header.getValue());
                }
                break;
            default:
                throw new IllegalArgumentException("No mapping for Header: "
                        + header);
            }
        }
    }

    /**
     * A meaningful name representative of the JSP page being cached.
     * <p/>
     * The <code>cacheName</code> field is be set by the <code>doInit</code>
     * method. Override to control the name used. The significance is that the
     * name is used to find a cache configuration in <code>ehcache.xml</code>
     * 
     * @return the name of the cache to use for this filter.
     */
    protected String getCacheName() {
        return cacheName;
    }

    /**
     * Gets the CacheManager for this CachingFilter. It is therefore up to
     * subclasses what CacheManager to use.
     * <p/>
     * This method was introduced in ehcache 1.2.1. Older versions used a
     * singleton CacheManager instance created with the default factory method.
     * 
     * @return the CacheManager to be used
     * @since 1.2.1
     */
    protected CacheManager getCacheManager() {
    	return cacheManager;
    }

    
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

    /**
     * Writes the response content. This will be gzipped or non gzipped
     * depending on whether the User Agent accepts GZIP encoding.
     * <p/>
     * If the body is written gzipped a gzip header is added.
     * 
     * @param response
     * @param pageInfo
     * @throws IOException
     */
    protected void writeContent(final HttpServletRequest request,
            final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, ResponseHeadersNotModifiableException {
        byte[] body;

        boolean shouldBodyBeZero = ResponseUtil.shouldBodyBeZero(request,
                pageInfo.getStatusCode());
        if (shouldBodyBeZero) {
            body = new byte[0];
        } else if (acceptsGzipEncoding(request)) {
            body = pageInfo.getGzippedBody();
            if (ResponseUtil.shouldGzippedBodyBeZero(body, request)) {
                body = new byte[0];
            } else {
                ResponseUtil.addGzipHeader(response);
            }

        } else {
            body = pageInfo.getUngzippedBody();
        }

        response.setContentLength(body.length);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }
    
    protected boolean acceptsGzipEncoding(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }
    /**
     * Logs the request headers, if debug is enabled.
     *
     * @param request
     */
    protected void logRequestHeaders(final HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            Map<String,String> headers = new HashMap<>();
            Enumeration<String> enumeration = request.getHeaderNames();
            StringBuffer logLine = new StringBuffer();
            logLine.append("Request Headers");
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                String headerValue = request.getHeader(name);
                headers.put(name, headerValue);
                logLine.append(": ").append(name).append(" -> ").append(headerValue);
            }
            LOG.debug(logLine.toString());
        }
    }
    /**
     * Checks if request accepts the named encoding.
     */
    protected boolean acceptsEncoding(final HttpServletRequest request, final String name) {
        final boolean accepts = headerContains(request, "Accept-Encoding", name);
        return accepts;
    }
    /**
     * Checks if request contains the header value.
     */
    private boolean headerContains(final HttpServletRequest request, final String header, final String value) {

        logRequestHeaders(request);

        final Enumeration<String> accepted = request.getHeaders(header);
        while (accepted.hasMoreElements()) {
            final String headerValue = (String) accepted.nextElement();
            if (headerValue.indexOf(value) != -1) {
                return true;
            }
        }
        return false;
    }



	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
		if (response.isCommitted()) {
		     throw new ServletException(
		             "Response already committed before doing buildPage.");
		 }
		 try {
			logRequestHeaders(httpRequest);
			 PageInfo pageInfo = buildPageInfo(httpRequest, httpResponse, filterChain);

			 if (pageInfo.isOk()) {
			     if (response.isCommitted()) {
			         throw new ServletException(
			                 "Response already committed after doing buildPage"
			                         + " but before writing response from PageInfo.");
			     }
			     writeResponse(httpRequest, httpResponse, pageInfo);
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
