package com.treevalue.beself.js

import com.treevalue.beself.values.VIDEO_BLOCK_ID

private const val CSS_FORCE_DARK = """  
    /* 全局黑暗模式样式 */
    * {
        color: #F3F3F3 !important;
        border-color: #747A80 !important;
    }

    /* 主要内容容器的背景 */
    body, section, article, main, aside, header, footer, nav {
        background-color: #1a1a1a !important;
    }
        
    /* 主要内容区域添加背景 */
    table, th, td,img, video, .container, .content, .main-content, .sidebar, .card, .panel, .box,
    [class*="container"], [class*="content"], [class*="main"], [class*="sidebar"],
    [class*="card"], [class*="panel"], [class*="box"] {
        background-color: #2d2d2d !important;
    }

    th {
        background-color: #333333 !important;
    }

    /* 表单元素 */
    input, textarea, select {
        background-color: #2d2d2d !important;
        color: white !important;
    }

    button {
        background-color: #333333 !important;
        color: white !important;
    }

    button:hover {
        background-color: #555555 !important;
    }

    /* 链接样式 */
    a {
        color: #58a6ff !important;
        background-color: transparent !important;
    }

    a:hover {
        color: #79c0ff !important;
        background-color: rgba(88, 166, 255, 0.1) !important;
    }


    /* 代码块样式 */
    pre, code {
        background-color: #0d1117 !important;
        color: #f0f6fc !important;
    }

    /* 引用样式 */
    blockquote {
        background-color: #2d2d2d !important;
        border-left: 4px solid #58a6ff !important;
        border-top: none !important;
        border-right: none !important;
        border-bottom: none !important;
        color: white !important;
    }


    /* 特殊处理一些常见的布局元素 */
     :empty, .row, .col, .column, .grid, .flex, .flexbox,
    [class*="row"], [class*="col"], [class*="grid"], [class*="flex"] {
        background-color: transparent !important;
    }

    /* 导航元素 */
    nav, .nav, .navbar, .navigation {
        background-color: #161b22 !important;
    }

    /* 头部和尾部 */
    .header, .footer {
        background-color: #0d1117 !important;
    }

    /* 移除内联元素的过度样式 */
     p, h1, h2, h3, h4, h5, h6, span, label, ul, ol, li, strong, em, b, i, small, mark, del, ins, sub, sup {
        background-color: transparent !important;
        color: white !important;
    }

* {
    background-color: #1a1a1a !important;
}
// todo it will block video in dark mode, and cause is div
"""

fun getForceDarkModeScript(enable: Boolean): String {
    val script = if (enable) {
        """
        (function(){
            try {
                // 移除旧样式
                let style = document.getElementById('force-dark-style');
                if(style) {
                    style.remove();
                }
                
                // 创建新样式并添加保护机制
                style = document.createElement('style');
                style.id = 'force-dark-style';
                
                // 添加 !important 声明和更高的优先级
                style.textContent = `${CSS_FORCE_DARK}`;
                
                // 给样式元素添加保护属性
                style.setAttribute('data-dark-mode-protection', 'true');
                style.setAttribute('data-created-time', Date.now().toString());
                
                // 强制插入样式
                if (document.head) {
                    document.head.appendChild(style);
                } else if (document.documentElement) {
                    document.documentElement.appendChild(style);
                }
                
                // 设置保护机制：监听DOM变化，如果样式被删除就重新添加
                if (window.darkModeProtector) {
                    window.darkModeProtector.disconnect();
                }
                
                window.darkModeProtector = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.type === 'childList') {
                            mutation.removedNodes.forEach(function(node) {
                                if (node.id === 'force-dark-style') {
                                    setTimeout(function() {
                                        if (!document.getElementById('force-dark-style')) {
                                            const newStyle = document.createElement('style');
                                            newStyle.id = 'force-dark-style';
                                            newStyle.textContent = `${CSS_FORCE_DARK}`;
                                            newStyle.setAttribute('data-dark-mode-protection', 'true');
                                            if (document.head) {
                                                document.head.appendChild(newStyle);
                                            }
                                        }
                                    }, 100);
                                }
                            });
                        }
                    });
                });
                
                // 开始监听 head 的变化
                if (document.head) {
                    window.darkModeProtector.observe(document.head, {
                        childList: true,
                        subtree: false
                    });
                }
            } catch(e) {
                console.error('应用黑暗模式失败:', e);
            }
        })();
        """.trimIndent()
    } else {
        """
        (function(){
            try {
                // 停止保护器
                if (window.darkModeProtector) {
                    window.darkModeProtector.disconnect();
                }
                
                const style = document.getElementById('force-dark-style');
                if(style) {
                    style.remove();
                }
            } catch(e) {
                console.error('移除黑暗模式失败:', e);
            }
        })();
        """.trimIndent()
    }
    return script
}

fun getVideoRemovalScript(): String {
    return """
        (function() {
            
            // 移除视频元素的函数（更精准）
            function removeVideosCarefully() {
                let removedCount = 0;
                
                // 1. 只移除真正的video标签
                const videos = document.querySelectorAll('video');
                videos.forEach(video => {
                    try {
                        video.pause();
                        video.src = '';
                        video.load();
                        video.remove();
                        removedCount++;
                        console.log('移除video元素:', video);
                    } catch(e) {
                        console.log('移除video失败:', e);
                    }
                });
                
                // 2. 只移除明确的视频iframe（更精准的判断）
                const iframes = document.querySelectorAll('iframe');
                iframes.forEach(iframe => {
                    const src = iframe.src.toLowerCase();
                    const videoKeywords = [
                        'youtube.com/embed',
                        'youtu.be',
                        'bilibili.com/blackboard',
                        'bilibili.com/player',
                        'vimeo.com/video',
                        'player.twitch.tv',
                        'dailymotion.com/embed'
                    ];
                    
                    const isVideoIframe = videoKeywords.some(keyword => src.includes(keyword));
                    
                    if (isVideoIframe) {
                        try {
                            iframe.remove();
                            removedCount++;
                            console.log('移除视频iframe:', src);
                        } catch(e) {
                            console.log('移除iframe失败:', e);
                        }
                    }
                });
                
                // 3. 移除明确的视频embed/object
                const embeds = document.querySelectorAll('embed[type*="video"], object[type*="video"]');
                embeds.forEach(embed => {
                    try {
                        embed.remove();
                        removedCount++;
                        console.log('移除embed/object视频元素');
                    } catch(e) {
                        console.log('移除embed失败:', e);
                    }
                });
                
                // 4. 移除video相关的source标签
                const videoSources = document.querySelectorAll('source[type*="video"]');
                videoSources.forEach(source => {
                    try {
                        source.remove();
                        removedCount++;
                    } catch(e) {
                        console.log('移除source失败:', e);
                    }
                });
                
                return removedCount;
            }
            
            // 简单的CSS隐藏（不影响功能）
            function hideVideosWithCSS() {
                try {
                    const style = document.createElement('style');
                    style.id = $VIDEO_BLOCK_ID;
                    style.textContent = `
                        video {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            width: 0 !important;
                            height: 0 !important;
                        }
                        iframe[src*="youtube.com/embed"],
                        iframe[src*="bilibili.com/player"],
                        iframe[src*="vimeo.com/video"] {
                            display: none !important;
                        }
                    `;
                    
                    if (!document.getElementById($VIDEO_BLOCK_ID)) {
                        document.head.appendChild(style);
                    }
                } catch(e) {
                    console.log('应用CSS失败:', e);
                }
            }
            
            // 轻量级的DOM监听（只监听video相关）
            function setupLightweightObserver() {
                try {
                    const observer = new MutationObserver(function(mutations) {
                        mutations.forEach(function(mutation) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeType === 1) {
                                    // 只处理video元素
                                    if (node.tagName === 'VIDEO') {
                                        setTimeout(() => {
                                            try {
                                                node.remove();
                                                console.log('移除动态添加的video元素');
                                            } catch(e) {
                                                console.log('移除动态video失败:', e);
                                            }
                                        }, 100);
                                    }
                                    // 检查子元素中的video
                                    else if (node.querySelector) {
                                        const videos = node.querySelectorAll('video');
                                        if (videos.length > 0) {
                                            setTimeout(() => {
                                                videos.forEach(video => {
                                                    try {
                                                        video.remove();
                                                        console.log('移除子元素中的video');
                                                    } catch(e) {
                                                        console.log('移除子video失败:', e);
                                                    }
                                                });
                                            }, 100);
                                        }
                                    }
                                }
                            });
                        });
                    });
                    
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true
                    });
                    return observer;
                } catch(e) {
                    console.log('设置观察器失败:', e);
                    return null;
                }
            }
            
            // 主执行函数
            function executeConservativeRemoval() {
                try {
                    // 先应用CSS隐藏
                    hideVideosWithCSS();
                    
                    // 移除现有视频
                    const removed = removeVideosCarefully();
                    
                    // 设置轻量级监听
                    setupLightweightObserver();
                    
                    // 延迟再检查一次（给页面渲染时间）
                    setTimeout(() => {
                        try {
                            const laterRemoved = removeVideosCarefully();
                        } catch(e) {
                            console.log('延迟移除失败:', e);
                        }
                    }, 2000);
                    
                } catch(e) {
                    console.log('视频移除执行失败:', e);
                }
            }
            
            // 确保安全执行
            try {
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', executeConservativeRemoval);
                } else {
                    executeConservativeRemoval();
                }
            } catch(e) {
                console.log('脚本初始化失败:', e);
            }
            
        })();
    """.trimIndent()
}

fun getNewTabInterceptionScriptAndroid(): String {
    return """
            (function() {
                if (window.newTabInterceptionInjected) return;
                
                function notifyNewTab(url, title) {
                    var iframe = document.createElement('iframe');
                    iframe.style.display = 'none';
                    iframe.src = 'newtab://' + encodeURIComponent(url) + '?title=' + encodeURIComponent(title || '新标签页');
                    document.body.appendChild(iframe);
                    setTimeout(function() {
                        document.body.removeChild(iframe);
                    }, 100);
                }
                
                var originalOpen = window.open;
                window.open = function(url, name, specs) {
                    if (url) {
                        notifyNewTab(url, name || '新标签页');
                        return null;
                    }
                    return originalOpen.call(this, url, name, specs);
                };
                
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    
                    if (target && target.tagName === 'A' && target.getAttribute('target') === '_blank' && target.href) {
                        e.preventDefault();
                        e.stopPropagation();
                        notifyNewTab(target.href, target.textContent || '新标签页');
                        return false;
                    }
                });
                
                window.newTabInterceptionInjected = true;
            })();
        """.trimIndent()
}

fun getBilibiliFixScript(): String {
    return """(function() {
                try {
                    if (typeof window.TouchEvent === 'undefined') {
                        window.TouchEvent = function() {};
                    }
                    
                    document.addEventListener('touchstart', function() {}, {passive: true});
                    document.addEventListener('touchmove', function() {}, {passive: true});
                    
                    document.addEventListener('DOMContentLoaded', function() {
                        var videos = document.querySelectorAll('video');
                        videos.forEach(function(video) {
                            video.setAttribute('playsinline', 'true');
                            video.setAttribute('webkit-playsinline', 'true');
                            video.setAttribute('preload', 'metadata');
                        });
                    });
                    
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = 'width=device-width, initial-scale=1.0, user-scalable=yes';
                        document.head.appendChild(meta);
                    }
                    
                    console.log('Bilibili fix script injected successfully');
                } catch (e) {
                    console.error('Error in bilibili fix script:', e);
                }
            })();
        """.trimIndent()
}

fun getNewTabInterceptionScriptDeskTop(): String {
    return """
            (function() {
                if (window.newTabInterceptionInjected) {
                    console.log('桌面端新标签页拦截脚本已存在，跳过注入');
                    return;
                }
                
                console.log('桌面端开始注入新标签页拦截脚本...');
                
                // 用于与桌面端通信的函数
                function notifyNewTab(url, title) {
                    // 使用cefQuery与桌面端通信
                    if (window.cefQuery) {
                        window.cefQuery({
                            request: JSON.stringify({
                                id: Date.now(),
                                method: 'newTab',
                                params: JSON.stringify({
                                    url: url,
                                    title: title || '新标签页'
                                })
                            }),
                            onSuccess: function(response) {
                                console.log('新标签页请求已发送:', url);
                            },
                            onFailure: function(error_code, error_message) {
                                console.error('新标签页请求失败:', error_message);
                            }
                        });
                    } else {
                        console.warn('cefQuery不可用，无法发送新标签页请求');
                    }
                }
                
                // 1. 拦截window.open()
                var originalOpen = window.open;
                window.open = function(url, name, specs) {
                    console.log('桌面端拦截到window.open调用:', url, name, specs);
                    
                    if (url) {
                        notifyNewTab(url, name || '新标签页');
                        return null; // 阻止默认行为
                    }
                    
                    return originalOpen.call(this, url, name, specs);
                };
                
                // 2. 拦截target="_blank"链接
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    
                    // 查找最近的<a>标签
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    
                    if (target && target.tagName === 'A') {
                        var href = target.href;
                        var targetAttr = target.getAttribute('target');
                        
                        if (targetAttr === '_blank' && href) {
                            e.preventDefault();
                            e.stopPropagation();
                            
                            var linkText = target.textContent || target.title || target.alt || '新标签页';
                            notifyNewTab(href, linkText);
                            
                            console.log('桌面端已拦截target="_blank"链接:', href);
                            return false;
                        }
                    }
                });
                
                // 3. 拦截表单的target="_blank"
                document.addEventListener('submit', function(e) {
                    var form = e.target;
                    if (form.tagName === 'FORM' && form.getAttribute('target') === '_blank') {
                        e.preventDefault();
                        
                        var formData = new FormData(form);
                        var url = form.action || window.location.href;
                        
                        if (form.method.toLowerCase() === 'get') {
                            var params = new URLSearchParams(formData);
                            url += (url.includes('?') ? '&' : '?') + params.toString();
                        }
                        
                        console.log('桌面端拦截表单提交到新窗口:', url);
                        notifyNewTab(url, '表单结果');
                        return false;
                    }
                });
                
                // 标记已注入
                window.newTabInterceptionInjected = true;
                console.log('桌面端新标签页拦截脚本注入完成');
            })();
        """.trimIndent()
}

fun getHideScrollbarScript(): String {
    return """
        (function() {
            try {
                const style = document.createElement('style');
                style.id = 'hide-scrollbar-style';
                style.textContent = `
                    /* 隐藏滚动条但保持滚动功能 */
                    ::-webkit-scrollbar {
                        width: 0px;
                        height: 0px;
                    }
                    
                    html, body {
                        scrollbar-width: none; /* Firefox */
                        -ms-overflow-style: none; /* IE and Edge */
                    }
                    
                    /* 确保内容不会溢出 */
                    html, body {
                        overflow-x: hidden;
                        margin: 0;
                        padding: 0;
                    }
                `;
                
                if (!document.getElementById('hide-scrollbar-style')) {
                    (document.head || document.documentElement).appendChild(style);
                }
                
                console.log('隐藏滚动条样式已应用');
            } catch(e) {
                console.error('应用隐藏滚动条样式失败:', e);
            }
        })();
    """.trimIndent()
}
