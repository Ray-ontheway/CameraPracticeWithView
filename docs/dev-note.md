# 开发日志

## ViewBinding 封装
ViewBinding的封装, 是在网上找到的方案, [原文地址](https://juejin.cn/post/7064498827931156493)

- 这个方案是可以使用的, 到那时在使用过程中发现一个小情况
  - 在默认创建的 "Hello world" 的 `TextView` 的MainActivity 的页面当中, 如果没有显示的调用 `binding`， 默认的 `TextView` 不会显示出来
