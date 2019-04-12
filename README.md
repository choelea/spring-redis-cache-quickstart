# spring-redis-cache-quickstart

## 三步让Spring Boot拥有缓存的能力
### 引入spring-boot-starter-data-redis
```
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
### 配置redis，打开缓存开关
使用`@EnableCaching`开启缓存

### 添加Cacheale 注解
 在需要缓存的方法处使用注解`@Cacheable`。