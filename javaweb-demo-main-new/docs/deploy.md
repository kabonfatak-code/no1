# BBS 部署说明

## 本地 MySQL

默认连接配置：

- URL: `jdbc:mysql://localhost:3306/bbs?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
- 用户: `root`
- 密码: `123456`

如果你的 MySQL 密码不同，可以在 Tomcat VM options 中设置：

```text
-Dbbs.db.url=jdbc:mysql://localhost:3306/bbs?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
-Dbbs.db.user=root
-Dbbs.db.password=你的密码
```

初始化数据库：

```powershell
mysql -uroot -p < database/schema.sql
```

数据库结构以 `database/schema.sql` 为准；应用启动时不会自动创建或升级数据表。脚本会写入默认管理员：

```text
admin / admin123
```

## 公网发布和 50 人访问

把 `target/demo-1.0-SNAPSHOT.war` 部署到公网服务器的 Tomcat 10.1：

1. 服务器安装 JDK 17+、Tomcat 10.1、MySQL 8。
2. 开放安全组或防火墙端口 `80` 或 `8080`。
3. 导入 `database/schema.sql`。
4. 配置 Tomcat VM options 中的 `bbs.db.*` 数据库参数。
5. 把 WAR 放进 Tomcat `webapps`，启动 Tomcat。

50 人并发访问的建议配置：

```xml
<Connector port="8080"
           protocol="HTTP/1.1"
           maxThreads="100"
           acceptCount="100"
           connectionTimeout="20000" />
```

MySQL 建议 `max_connections >= 100`。

## 阿里云短信

正式服务器默认调用阿里云短信验证码服务；不要开启演示模式。需要在 Tomcat VM options 或服务器环境变量中配置：

```text
-Dbbs.sms.aliyun.accessKeyId=你的AccessKeyId
-Dbbs.sms.aliyun.accessKeySecret=你的AccessKeySecret
-Dbbs.sms.aliyun.signName=你的短信签名
-Dbbs.sms.aliyun.templateCode=你的模板Code
```

也支持环境变量：

```text
BBS_SMS_ALIYUN_ACCESS_KEY_ID
BBS_SMS_ALIYUN_ACCESS_KEY_SECRET
BBS_SMS_ALIYUN_SIGN_NAME
BBS_SMS_ALIYUN_TEMPLATE_CODE
```

本地调试时可以开启演示验证码，不会调用阿里云：

```text
-Dbbs.sms.demo=true
```

## 真实 IP 和地区

如果 Tomcat 前面有 Nginx、负载均衡或 CDN，需要转发真实 IP：

```nginx
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header Host $host;
```

项目已内置 `src/main/resources/ip2region/ip2region.xdb`，部署后的 WAR 会优先使用离线库解析省份，不依赖外部接口。
如需在服务器上使用外部更新后的 xdb 文件，可配置：

```text
-Dbbs.ip.location.xdb.path=/opt/bbs/ip2region.xdb
```

在线 IP 接口仅作为离线库不可用时的兜底；如服务器访问定位服务不稳定，可配置：

```text
-Dbbs.ip.location.timeout.ms=800
-Dbbs.ip.location.enabled=true
```
