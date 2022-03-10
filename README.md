## 简介

<div align="center">
 <img src="https://img.shields.io/badge/license-Apache2.0-brightgreen.svg" align=left />
 <img src="https://img.shields.io/badge/drouter--plugin--proxy-1.0.2-red.svg" align=left />
 <img src="https://img.shields.io/badge/drouter--api-2.2.2-blue.svg" align=left />
 <img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg" align=left />
</div>
<br/>
DRouter是18年滴滴乘客端自研的一套Android路由框架，基于平台化解耦的思想，为组件间通信服务。该项目以功能全面、易用为原则，支持各种路由场景，在页面路由、服务获取和过滤、跨进程及应用、VirtualApk插件支持等方面都能提供多样化的服务。目前已在滴滴乘客端、顺风车、单车、国际化、滴滴定制车等十多个滴滴的app内使用，得到各种场景的验证。

<img src="https://czp.s3.didiyunapi.com/image/drouter/DRouter_Architecture.jpg" width="700px" align="center" alt="DRouter架构图"/>

## 提供功能

- 使用URI字符串导航Activity、Fragment、View、RouterHandler，注解支持正则表达式、占位符
- 回调式onActivityResult
- RouterHandler、Activity支持等待异步完成(hold)，并可设置超时时间
- RouterHandler指定执行线程
- 注入拦截器，支持全局拦截器和局部拦截器，面向切面编程，拦截器可以使用字符串名字
- 更为多样化的Fragment页面跳转能力
- 使用接口或基类导航到实现类Service的Class以及实例
- 支持Service别名以及多维过滤器查找
- 导航Service可指定任意构造器、支持单例
- 支持动态注册RouterHandler、Service，绑定生命周期自动解绑
- 简单易用的跨进程执行RouterHandler、Service
- 跨进程访问无需提前绑定、如同本地调用一样进行访问
- 支持客户端进程和服务端进程自动重连
- 支持VirtualApk
- 适配AndroidX

### 技术特点
1. 插件不依赖gradle版本，支持增量编译，多线程扫描，提升编译效率
2. 初始化点对点加载路由表，无反射和遍历，异步加载，提升启动性能
3. 完整的Router功能
4. 强大的ServiceLoader实例化和过滤能力
5. 易用高效的跨进程通信机制，同步执行，就像调用本地方法
6. 框架内部尽可能规避反射，提升运行时性能
7. 动态下载与api匹配的plugin，无需升级plugin版本
8. 无需手动添加混淆规则

## 核心模块

模块 | 功能
|  :-- | :--  |
Router | Native和H5导航Activity、Fragment、View、RouterHandler，支持切入拦截器
Service | 基于spi通过接口的形式查找到实现类，支持过滤和优先级配置
Remote | 跨进程访问Router、Service
Page | 支持单Page、栈Page、ViewPager三种形式的Fragment加载

## 使用文档

接入方式及文档请移步 [Wiki](https://github.com/didi/DRouter/wiki)

``` java
/** 最新版本 **/
classpath "io.github.didi:drouter-plugin-proxy:1.0.2"
api "io.github.didi:drouter-api:2.2.2"   // 该版本为androidx, 如需support版本1.1.0
```

## 微信交流群

<img src="https://czp.s3.didiyunapi.com/image/drouter/drouter.jpg" width="250px" align="center" alt="WeiXin"/>

如果二维码到期，请微信搜索作者 gwball (备注DRouter)入群

## 相关文章

[滴滴开源DRouter：一款高效的Android路由框架](https://juejin.cn/post/6975818153381068831)

## License

<img alt="Apache-2.0 license" src="https://www.apache.org/img/ASF20thAnniversary.jpg" width="128">

DRouter 基于 Apache-2.0 协议进行分发和使用，更多信息参见 [协议文件](LICENSE)
