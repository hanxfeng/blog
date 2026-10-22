# 前端依赖本地化说明

`static/vendor/` 存放从 npm 官方源取出的浏览器构建产物，**不再依赖 jsDelivr**。
国内网络访问 `cdn.jsdelivr.net` 经常被连接重置（`ERR_CONNECTION_RESET`），会让页面脚本直接中断
（典型症状：`Uncaught TypeError: window.markdownit is not a function`），所以改为同源托管。

| 目录 | 版本 | 许可 | 来源包 |
| --- | --- | --- | --- |
| `markdown-it/` | 14.1.0 | MIT | [`markdown-it`](https://www.npmjs.com/package/markdown-it) `dist/markdown-it.min.js` |
| `highlight.js/` | 11.12.0 | BSD-3-Clause | [`@highlightjs/cdn-assets`](https://www.npmjs.com/package/@highlightjs/cdn-assets) `highlight.min.js`、`styles/github.min.css` |
| `github-markdown-css/` | 5.9.0 | MIT | [`github-markdown-css`](https://www.npmjs.com/package/github-markdown-css) `github-markdown.css` |

## 页面引用方式

```html
<link rel="stylesheet" href="vendor/github-markdown-css/github-markdown.css">
<link rel="stylesheet" href="vendor/highlight.js/styles/github.min.css">
<script src="vendor/markdown-it/markdown-it.min.js"></script>
<script src="vendor/highlight.js/highlight.min.js"></script>
```

路径是相对路径，因此页面放在站点根目录或子路径下都能用；如果以后改成前后端分离、页面走 CDN，
记得把这些文件一起发布，或把引用改成绝对路径。

## 如何升级

```bash
# 任选其一，取到 tgz 后解包，把对应文件覆盖到本目录
curl -L -O https://registry.npmjs.org/markdown-it/-/markdown-it-14.1.0.tgz
tar -xzf markdown-it-14.1.0.tgz
```

升级 `markdown-it` 到 15.x 前先确认仍然提供 UMD 构建（`dist/markdown-it.min.js`），
页面目前依赖全局变量 `window.markdownit`。

## 仍未本地化的部分

- Tailwind：`https://cdn.tailwindcss.com`（浏览器端 JIT 构建）。它加载失败只影响样式，不影响数据与交互。
  需要彻底离线时，可换成 `@tailwindcss/browser` 的同源副本，但要重新核对与页面所用 Tailwind 3 类名的兼容性。
