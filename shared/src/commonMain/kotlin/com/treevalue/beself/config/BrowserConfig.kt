package com.treevalue.beself.config

data class AllowedSite(val label: String, val host: String)

object BrowserConfig {
    const val useDebugModel = true

    const val useJumpInitPage = false

    var INITIAL_HTML: String =
        """<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"><style>*{margin:0;padding:0;box-sizing:border-box;}html,body{height:100vh;width:100vw;font-family:'Arial',sans-serif;overflow:hidden;transition:background-color .3s,color .3s;}.dark{background:#1e1f22;color:white;}.light{background:#ffffff;color:#000}.container{display:flex;justify-content:center;align-items:center;width:100vw;height:100vh;}h1{font-size:6em;}</style></head><body class=\"dark\"><div class=\"container\"><h1>Hee</h1></div></body></html>"""

    const val FILE_CHOSE_TEST: String = """
        <!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文件上传测试</title>
    <style>
        body { padding: 20px; font-family: Arial; }
        input { margin: 10px 0; padding: 10px; font-size: 16px; }
        button { padding: 10px 20px; font-size: 16px; }
    </style>
</head>
<body>
    <h2>文件上传测试</h2>
    
    <!-- 单文件上传 -->
    <div>
        <label>单文件上传：</label>
        <input type="file" id="singleFile" />
    </div>
    
    <!-- 多文件上传 -->
    <div>
        <label>多文件上传：</label>
        <input type="file" id="multipleFiles" multiple />
    </div>
    
    <!-- 指定文件类型 -->
    <div>
        <label>图片上传：</label>
        <input type="file" accept="image/*" />
    </div>
    
    <!-- 测试按钮 -->
    <button onclick="testFileInput()">测试文件输入</button>
    
    <div id="result"></div>
    
    <script>
        function testFileInput() {
            console.log('测试文件输入被点击');
            
            // 创建动态文件输入
            const input = document.createElement('input');
            input.type = 'file';
            input.multiple = true;
            input.accept = '*/*';
            
            input.onchange = function(e) {
                console.log('文件选择完成:', e.target.files);
                document.getElementById('result').innerHTML = 
                    '选择了 ' + e.target.files.length + ' 个文件';
            };
            
            // 触发文件选择
            input.click();
        }
        
        // 监听所有文件输入变化
        document.addEventListener('change', function(e) {
            if (e.target.type === 'file') {
                console.log('文件输入变化:', e.target.files);
            }
        });
        
        console.log('文件上传测试页面已加载');
    </script>
</body>
</html>
        """

    const val NEW_TAB_TEST: String =
        """  <!DOCTYPE html>
            <html>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>新标签页测试</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                        background: rgba(255,255,255,0.1);
                        padding: 30px;
                        border-radius: 20px;
                        backdrop-filter: blur(10px);
                    }
                    .section {
                        margin: 30px 0;
                        padding: 20px;
                        background: rgba(255,255,255,0.1);
                        border-radius: 15px;
                    }
                    h1 { font-size: 2.5em; margin-bottom: 30px; text-align: center; }
                    h3 { margin-bottom: 15px; color: #ffd700; }
                    .link, .button {
                        display: inline-block;
                        margin: 10px;
                        padding: 12px 24px;
                        background: linear-gradient(45deg, #FF6B6B, #FF8E53);
                        color: white;
                        text-decoration: none;
                        border: none;
                        border-radius: 25px;
                        cursor: pointer;
                        font-weight: 600;
                        transition: all 0.3s ease;
                    }
                    .link:hover, .button:hover {
                        background: linear-gradient(45deg, #FF8E53, #FF6B6B);
                        transform: translateY(-2px);
                    }
                    .code {
                        background: rgba(0,0,0,0.3);
                        padding: 10px;
                        border-radius: 5px;
                        font-family: 'Courier New', monospace;
                        font-size: 0.9em;
                        margin: 10px 0;
                    }
                </style>
            </head>
            <body>
                <div class='container'>
                    <h1>🚀 JavaScript新标签页测试</h1>

                    <div class='section'>
                        <h3>1️⃣ HTML target="_blank" 方式</h3>
                        <div class='code'>&lt;a href="url" target="_blank"&gt;链接&lt;/a&gt;</div>
                        <a href='https://www.baidu.com' target='_blank' class='link'>so (target="_blank")</a>
                        <a href='https://www.baidu.com' target='_blank' class='link'>GitHub (target="_blank")</a>
                        <a href='https://www.baidu.com' target='_blank' class='link'>Stack Overflow (target="_blank")</a>
                    </div>

                    <div class='section'>
                        <h3>2️⃣ JavaScript window.open() 方式</h3>
                        <div class='code'>window.open(url, '_blank')</div>
                        <button class='button' onclick="window.open('https://www.baidu.com', '_blank')">
                            YouTube (window.open)
                        </button>
                        <button class='button' onclick="window.open('https://www.wikipedia.org', '_blank', 'width=800,height=600')">
                            Wikipedia (window.open 带参数)
                        </button>
                        <button class='button' onclick="openWithDelay()">
                            延迟打开 (异步window.open)
                        </button>
                    </div>

                    <div class='section'>
                        <h3>3️⃣ 动态创建链接方式</h3>
                        <div class='code'>createElement('a') + click()</div>
                        <button class='button' onclick="openByCreatingLink('https://www.baidu.com')">
                            百度 (动态创建链接)
                        </button>
                        <button class='button' onclick="openByCreatingLink('https://developer.android.com')">
                            Android开发者 (动态创建链接)
                        </button>
                    </div>

                    <div class='section'>
                        <h3>4️⃣ 表单提交方式</h3>
                        <div class='code'>&lt;form target="_blank"&gt;</div>
                        <form method="get" action="https://www.baidu.com/search" target="_blank" style="display:inline;">
                            <input type="text" name="q" value="Android WebView" style="padding:8px; border-radius:5px; border:none; margin-right:10px;">
                            <button type="submit" class="button">Google搜索 (表单target="_blank")</button>
                        </form>
                    </div>

                    <div class='section'>
                        <h3>5️⃣ Android接口直接调用</h3>
                        <div class='code'>Android.openNewTab(url, title)</div>
                        <button class='button' onclick="Android.openNewTab('https://www.baidu.com', 'Reddit')">
                            Reddit (Android接口)
                        </button>
                        <button class='button' onclick="Android.openNewTab('home', '新主页')">
                            新主页 (Android接口)
                        </button>
                    </div>
                </div>

                <script>
                    // 延迟打开函数
                    function openWithDelay() {
                        setTimeout(function() {
                            window.open('https://www.baidu.com', '_blank');
                        }, 1000);
                    }

                    // 动态创建链接并点击
                    function openByCreatingLink(url) {
                        var link = document.createElement('a');
                        link.href = url;
                        link.target = '_blank';
                        link.style.display = 'none';
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                    }

                    // 页面加载完成提示
                    window.onload = function() {
                        console.log('测试页面加载完成，所有新标签页方式都可以测试');
                    };
                </script>
            </body>
            </html>"""

    val ALLOWED_SITES = listOf(
        AllowedSite("Gemini", "gemini.google.com"),
        AllowedSite("Google", "www.google.com"),
        AllowedSite("Kimi", "www.kimi.com"),
        AllowedSite("tianhu", "www.aitianhu.com"),
//        AllowedSite("csdn", "www.csdn.net"),
//        AllowedSite("rimg", "https://filesystem.site/cdn/20250706/m4opHgvU4XH840Jb9IBVwyI3S1WrsQ.jpg"),
    )
    val ALLOWED_PATTERNS = listOf(
        ".*://.*aitianhu.*",
        ".*://.*google\\.com.*",
        ".*://.*kimi\\.com.*",
        ".*://.*github\\.com.*",
        ".*://.*deepseek\\.com.*",
        ".*://.*\\.moonshot\\..*",
        ".*://.*192\\.168\\.1\\..*",
        ".*://.*csdn\\.net.*",
        ".*heeheeaii.*",

        ".*://.*mvnrepository\\.com.*",
        ".*://.*alicdn\\.com.*",
        ".*://.*localhost.*",
        ".*://.*127\\.0\\.0\\.1.*",
        ".*://.*music\\.126.*",
        "^(file|data)://.*"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    init {
        if (useJumpInitPage) {
            INITIAL_HTML = NEW_TAB_TEST
            ALLOWED_SITES.plus(AllowedSite("test", "https://www.baidu.com"))
        }
    }
}
