# spring-redis-cache-quickstart

## 三步让Spring Boot拥有缓存的能力
**第一步： 引入spring-boot-starter-data-redis**

```
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
**第二步：  配置redis，打开缓存开关**

使用`@EnableCaching`开启缓存， 配置redis连接
```
spring.redis.host=192.168.1.90
spring.redis.password=123456
spring.redis.port=6379
```

**第三步：  添加Cacheale 注解**

 在需要缓存的方法处使用注解`@Cacheable`。
 
** 验证**

完成上面三步后，当问到对应的方法的时候，结果就可以缓存至redis， 后面的访问注解从redis里面取。 没有其他配置的时候，默认永不过期。启动项目访问：[http://localhost:9099](http://localhost:9099)。 分别访问下面的URL，使用可视化工具查看redis，理解redis里面的key的值设置

 - http://localhost:9099  
 - http://localhost:9099/online
 - http://localhost:9099/online/self
 
> 缓存的实体必须是可以是可序列化的，即实现接口`Serializable`

## 缓存过期的两种方式
### 手动过期
使用注解`@CacheEvict`触发; 浏览器访问[http://localhost:9099/clear](http://localhost:9099/clear)将清除redis中的category缓存。 
```
@Override
@CacheEvict(cacheNames="category")
public void cleanCache() {
	LOG.info("Clean all cache using annotation: @CacheEvict");		
}
```

### 设置全局TTL(Time To Live)
默认情况下缓存是永不过期的的；设置全局过期时间使其自动过期：
```
spring.cache.redis.time-to-live=3600000  // 一小时后自动过期
```
> https://docs.spring.io/spring-data/redis/docs/2.1.6.RELEASE/reference/html/#redis.repositories.expirations 的介绍并没有给更多的帮助， 而且spring boot并没有提高简单的可配置，细粒度的TTL的配置。 https://github.com/spring-projects/spring-boot/issues/10795
```

## 页面级的缓存
一般情况下spring 的缓存都用在了Entity或者DTO等业务层面。 如果想要缓存整个页面，改如何操作呢？这里结合https://github.com/choelea/spring-page-caching 这个组件实现页面级别的缓存。
页面级别的缓存通过Filter在请求到达页面之前就返回相关结果。 通过访问`http://localhost:9099/cache` 可以测试页面缓存的效果。

关键实现在于`com.joe.springrediscachequickstart.pagecache.SimplePageCachingFilter`。 
