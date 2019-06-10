package com.joe.springrediscachequickstart.pagecache;

/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;

/**
 * An abstract CachingFilter.
 * <p/>
 * This class should be sub-classed for each page to be cached.
 * <p/>
 * The filters must be declared in the web.xml deployment descriptor. Then a
 * mapping from a web resource, such as a JSP Page, FreeMarker page, Velocity
 * page, Servlet or static resouce needs to be defined. Finally, a succession of
 * mappings can be used to create a filter chain. See SRV.6 of the Servlet 2.3
 * specification for more details.
 * <p/>
 * Care should be taken not to define a filter chain such that the same
 * {@link CachingFilter} class is reentered. The {@link CachingFilter} uses the
 * {@link net.sf.ehcache.constructs.blocking.BlockingCache}. It blocks until the
 * thread which did a get which results in a null does a put. If reentry happens
 * a second get happens before the first put. The second get could wait
 * indefinitely. This situation is monitored and if it happens, an
 * IllegalStateException will be thrown.
 * <p/>
 * The following init-params are supported:
 * <ol>
 * <li>cacheName - the name in ehcache.xml used by the filter.
 * <li>blockingTimeoutMillis - the time, in milliseconds, to wait for the filter
 * chain to return with a response on a cache miss. This is useful to fail fast
 * in the event of an infrastructure failure.
 * </ol>
 * 
 * @author @author Greg Luck
 */
public abstract class CachingFilter extends Filter {

    private static final Logger LOG = LoggerFactory
            .getLogger(CachingFilter.class);
    private static final String BLOCKING_TIMEOUT_MILLIS = "blockingTimeoutMillis";
    private static final String CACHE_NAME = "cacheName";

    /**
     * The cache name can be set through init parameters. If it is set it is
     * stored here.
     */
    protected String cacheName;

    private final VisitLog visitLog = new VisitLog();
 

    /**
     * Reads the filterConfig for the parameter "blockingTimeoutMillis", and if
     * found, set the blocking timeout. If there is a parsing exception, no
     * timeout is set.
     */
    Integer parseBlockingCacheTimeoutMillis(FilterConfig filterConfig) {

        String timeout = filterConfig.getInitParameter(BLOCKING_TIMEOUT_MILLIS);
        try {
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            return null;
        }

    }

    /**
     * Sets the cacheName from the filterConfig
     */
    protected void setCacheNameIfAnyConfigured(FilterConfig filterConfig) {
        this.cacheName = filterConfig.getInitParameter(CACHE_NAME);

    }

    /**
     * Destroys the filter.
     */
    protected void doDestroy() {
        // noop
    }

    /**
     * Performs the filtering for a request. This method caches based responses
     * keyed by {@link #calculateKey(javax.servlet.http.HttpServletRequest)}
     * <p/>
     * By default this method will queue requests requesting the page response
     * for a given key until the first thread in the queue has completed. The
     * request which occurs when the page expires incurs the cost of waiting for
     * the downstream processing to return the respone.
     * <p/>
     * The maximum time to wait can be configured by setting
     * <code>setTimeoutMillis</code> on the underlying
     * <code>BlockingCache</code>.
     * 
     * @param request
     * @param response
     * @param chain
     * @throws AlreadyGzippedException
     *             if a double gzip is attempted
     * @throws AlreadyCommittedException
     *             if the response was committed on the way in or the on the way
     *             back
     * @throws FilterNonReentrantException
     *             if an attempt is made to reenter this filter in the same
     *             request.
     * @throws LockTimeoutException
     *             if this request is waiting on another that is populating the
     *             cache entry and timeouts while waiting. Only occurs if the
     *             BlockingCache has a timeout set.
     * @throws Exception
     *             for all other exceptions. They will be caught and logged in
     *             {@link Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
     */
    protected void doFilter(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain chain)
            throws AlreadyGzippedException, Exception {
        if (response.isCommitted()) {
            throw new AlreadyCommittedException(
                    "Response already committed before doing buildPage.");
        }
        logRequestHeaders(request);
        PageInfo pageInfo = buildPageInfo(request, response, chain);

        if (pageInfo.isOk()) {
            if (response.isCommitted()) {
                throw new AlreadyCommittedException(
                        "Response already committed after doing buildPage"
                                + " but before writing response from PageInfo.");
            }
            writeResponse(request, response, pageInfo);
        }
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
        return buildPage(request, response, chain);
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

        final Collection cookies = pageInfo.getSerializableCookies();
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
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
    protected abstract CacheManager getCacheManager();

    /**
     * CachingFilter works off a key.
     * <p/>
     * The key should be unique. Factors to consider in generating a key are:
     * <ul>
     * <li>The various hostnames that a request could come through
     * <li>Whether additional parameters used for referral tracking e.g. google
     * should be excluded to maximise cache hits
     * <li>Additional parameters can be added to any page. The page will still
     * work but will miss the cache. Consider coding defensively around this
     * issue.
     * </ul>
     * <p/>
     * Implementers should differentiate between GET and HEAD requests otherwise
     * blank pages can result. See SimplePageCachingFilter for an example
     * implementation.
     * 
     * @param httpRequest
     * @return the key, generally the URL plus request parameters
     */
    protected abstract String calculateKey(final HttpServletRequest httpRequest);

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

    
    /**
     * threadlocal class to check for reentry
     * 
     * @author hhuynh
     * 
     */
    private static class VisitLog extends ThreadLocal<Boolean> {
        @Override
        protected Boolean initialValue() {
            return false;
        }

        public boolean hasVisited() {
            return get();
        }

        public void markAsVisited() {
            set(true);
        }

        public void clear() {
            super.remove();
        }
    }
}
