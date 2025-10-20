package com.treevalue.beself.js

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
                    style.id = 'video-blocker-style';
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
                    
                    if (!document.getElementById('video-blocker-style')) {
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

fun getNewTabInterceptionScript(): String {
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

fun getTranslateScript(targetLang: String = "zh-CN"): String {
    return """
(function() {
    console.log('[翻译器] 开始注入');
    
    // 防止重复注入
    if (window.__translatorInjected) {
        console.log('[翻译器] 已注入，跳过');
        return;
    }
    window.__translatorInjected = true;
    
    // 创建翻译按钮
    var btn = document.createElement('button');
    btn.id = 'translator-btn';
    btn.innerHTML = '🌐 翻译';
    btn.style.cssText = 
        'position: fixed !important;' +
        'top: 10px !important;' +
        'right: 10px !important;' +
        'z-index: 2147483647 !important;' +
        'padding: 8px 16px !important;' +
        'background: #4285f4 !important;' +
        'color: white !important;' +
        'border: none !important;' +
        'border-radius: 6px !important;' +
        'cursor: pointer !important;' +
        'font-size: 14px !important;' +
        'font-weight: 500 !important;' +
        'box-shadow: 0 2px 8px rgba(0,0,0,0.3) !important;' +
        'transition: all 0.3s ease !important;' +
        'font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif !important;';
    
    // 悬停效果
    btn.onmouseenter = function() {
        if (!this.translating) {
            this.style.background = '#3367d6';
            this.style.boxShadow = '0 4px 12px rgba(0,0,0,0.4)';
        }
    };
    btn.onmouseleave = function() {
        if (!this.translating) {
            this.style.background = '#4285f4';
            this.style.boxShadow = '0 2px 8px rgba(0,0,0,0.3)';
        }
    };
    
    // 翻译逻辑
    btn.onclick = function() {
        if (this.translating) {
            console.log('[翻译器] 正在翻译中，请稍候');
            return;
        }
        
        this.translating = true;
        this.innerHTML = '⏳ 翻译中...';
        this.style.background = '#fbbc04';
        this.style.cursor = 'wait';
        
        console.log('[翻译器] 开始收集文本');
        
        // 收集所有需要翻译的文本节点
        var textsToTranslate = [];
        var textNodes = [];
        var translatedCount = 0;
        var failedCount = 0;
        
        function collectText(node) {
            if (node.nodeType === 3) { // 文本节点
                var text = node.textContent.trim();
                if (text && text.length > 0 && !/^[\d\s\p{P}]+$/u.test(text)) {
                    textNodes.push(node);
                    textsToTranslate.push(text);
                }
            } else if (node.nodeType === 1) { // 元素节点
                // 跳过这些标签
                var skipTags = ['SCRIPT', 'STYLE', 'NOSCRIPT', 'IFRAME', 'CODE', 'PRE', 'SVG'];
                var skipClasses = ['notranslate', 'no-translate'];
                
                if (skipTags.includes(node.tagName)) {
                    return;
                }
                
                // 检查是否有不翻译的class
                if (node.className) {
                    var hasSkipClass = skipClasses.some(function(cls) {
                        return node.className.indexOf(cls) !== -1;
                    });
                    if (hasSkipClass) return;
                }
                
                // 遍历子节点
                var children = node.childNodes;
                for (var i = 0; i < children.length; i++) {
                    collectText(children[i]);
                }
            }
        }
        
        collectText(document.body || document.documentElement);
        console.log('[翻译器] 找到 ' + textsToTranslate.length + ' 个文本节点');
        
        if (textsToTranslate.length === 0) {
            this.innerHTML = '⚠️ 无文本';
            this.style.background = '#ea4335';
            this.translating = false;
            this.style.cursor = 'pointer';
            var self = this;
            setTimeout(function() {
                self.innerHTML = '🌐 翻译';
                self.style.background = '#4285f4';
            }, 2000);
            return;
        }
        
        var btn = this;
        
        // 批量翻译（每次20个，避免过多请求）
        var batchSize = 20;
        var batches = [];
        for (var i = 0; i < textsToTranslate.length; i += batchSize) {
            batches.push(textsToTranslate.slice(i, i + batchSize));
        }
        
        console.log('[翻译器] 分成 ' + batches.length + ' 批进行翻译');
        
        // 翻译单个文本
        function translateText(text, targetLang) {
            var url = 'https://translate.googleapis.com/translate_a/single' +
                      '?client=gtx' +
                      '&sl=auto' +
                      '&tl=' + targetLang +
                      '&dt=t' +
                      '&q=' + encodeURIComponent(text);
            
            return fetch(url, {
                method: 'GET',
                headers: {
                    'User-Agent': 'Mozilla/5.0'
                }
            })
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('HTTP error ' + response.status);
                }
                return response.text();
            })
            .then(function(data) {
                try {
                    // 解析Google返回的JSON
                    // 格式: [[["翻译结果","原文",null,null,3]],null,"源语言"]
                    var match = data.match(/\[\[\["([^"]*?)"/);
                    if (match && match[1]) {
                        var translated = match[1]
                            .replace(/\\n/g, '\n')
                            .replace(/\\t/g, '\t')
                            .replace(/\\"/g, '"')
                            .replace(/\\\\/g, '\\');
                        return translated || text;
                    }
                    return text;
                } catch(e) {
                    console.error('[翻译器] 解析失败:', e);
                    return text;
                }
            })
            .catch(function(err) {
                console.error('[翻译器] 请求失败:', text.substring(0, 50), err);
                return text;
            });
        }
        
        // 处理单批翻译
        function processBatch(batch, startIndex) {
            var promises = batch.map(function(text, idx) {
                var globalIndex = startIndex + idx;
                return translateText(text, '$targetLang')
                    .then(function(translated) {
                        if (textNodes[globalIndex]) {
                            textNodes[globalIndex].textContent = translated;
                            translatedCount++;
                        }
                        return true;
                    })
                    .catch(function(err) {
                        console.error('[翻译器] 翻译失败:', err);
                        failedCount++;
                        return false;
                    });
            });
            
            return Promise.all(promises);
        }
        
        // 顺序处理所有批次
        function processAllBatches() {
            var promise = Promise.resolve();
            var currentIndex = 0;
            
            batches.forEach(function(batch) {
                promise = promise.then(function() {
                    var batchIndex = currentIndex;
                    currentIndex += batch.length;
                    
                    // 更新进度
                    var progress = Math.round((currentIndex / textsToTranslate.length) * 100);
                    btn.innerHTML = '⏳ ' + progress + '%';
                    
                    return processBatch(batch, batchIndex);
                }).then(function() {
                    // 每批之间延迟200ms，避免请求过快
                    return new Promise(function(resolve) {
                        setTimeout(resolve, 200);
                    });
                });
            });
            
            return promise;
        }
        
        // 执行翻译
        processAllBatches()
            .then(function() {
                console.log('[翻译器] 翻译完成！成功: ' + translatedCount + ', 失败: ' + failedCount);
                btn.innerHTML = '✓ 已翻译';
                btn.style.background = '#34a853';
                btn.style.cursor = 'pointer';
                btn.translating = false;
                
                // 3秒后恢复按钮
                setTimeout(function() {
                    btn.innerHTML = '🌐 翻译';
                    btn.style.background = '#4285f4';
                }, 3000);
            })
            .catch(function(err) {
                console.error('[翻译器] 翻译过程出错:', err);
                btn.innerHTML = '✗ 失败';
                btn.style.background = '#ea4335';
                btn.style.cursor = 'pointer';
                btn.translating = false;
                
                setTimeout(function() {
                    btn.innerHTML = '🌐 翻译';
                    btn.style.background = '#4285f4';
                }, 3000);
            });
    };
    
    // 将按钮添加到页面
    function addButton() {
        if (document.body) {
            document.body.appendChild(btn);
            console.log('[翻译器] 翻译按钮已添加');
        } else {
            setTimeout(addButton, 100);
        }
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', addButton);
    } else {
        addButton();
    }
})();
    """.trimIndent()
}

fun getTranslatorWithLanguageSelector(): String {
    return """
(function() {
    if (window.__translatorInjected) return;
    window.__translatorInjected = true;
    
    // 创建容器
    var container = document.createElement('div');
    container.style.cssText = 
        'position: fixed !important;' +
        'top: 10px !important;' +
        'right: 10px !important;' +
        'z-index: 2147483647 !important;' +
        'display: flex !important;' +
        'gap: 8px !important;' +
        'align-items: center !important;' +
        'background: white !important;' +
        'padding: 6px !important;' +
        'border-radius: 8px !important;' +
        'box-shadow: 0 2px 12px rgba(0,0,0,0.15) !important;';
    
    // 语言选择
    var langSelect = document.createElement('select');
    langSelect.style.cssText = 
        'padding: 6px 10px !important;' +
        'border: 1px solid #ddd !important;' +
        'border-radius: 4px !important;' +
        'font-size: 13px !important;' +
        'cursor: pointer !important;';
    
    var languages = {
        'zh-CN': '简体中文',
        'zh-TW': '繁體中文',
        'en': 'English',
        'ja': '日本語',
        'ko': '한국어',
        'fr': 'Français',
        'de': 'Deutsch',
        'es': 'Español',
        'ru': 'Русский',
        'ar': 'العربية'
    };
    
    for (var code in languages) {
        var option = document.createElement('option');
        option.value = code;
        option.textContent = languages[code];
        langSelect.appendChild(option);
    }
    
    // 翻译按钮
    var btn = document.createElement('button');
    btn.innerHTML = '🌐 翻译';
    btn.style.cssText = 
        'padding: 6px 14px !important;' +
        'background: #4285f4 !important;' +
        'color: white !important;' +
        'border: none !important;' +
        'border-radius: 4px !important;' +
        'cursor: pointer !important;' +
        'font-size: 13px !important;' +
        'font-weight: 500 !important;';
    
    btn.onclick = function() {
        var targetLang = langSelect.value;
        console.log('翻译到: ' + languages[targetLang]);
        // 调用翻译逻辑（复用上面的代码）
        window.translatePage && window.translatePage(targetLang);
    };
    
    container.appendChild(langSelect);
    container.appendChild(btn);
    document.body.appendChild(container);
})();
    """.trimIndent()
}
