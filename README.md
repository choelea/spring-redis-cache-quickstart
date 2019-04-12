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

完成上面三步后，当问到对应的方法的时候，结果就可以缓存至redis， 后面的访问注解从redis里面取。 没有其他配置的时候，默认永不过期。启动项目访问：http://localhost:9099/。 使用可视化工具查看redis，理解redis里面的key的值设置。
 
> 缓存的实体必须是可以是可序列化的，即实现接口`Serializable`
