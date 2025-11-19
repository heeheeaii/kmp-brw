package com.treevalue.beself.backend

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.treevalue.beself.UrlReceiver
import com.treevalue.beself.config.BrowserConfig
import com.treevalue.beself.config.BrowserConfig.ALLOWED_PATTERNS
import com.treevalue.beself.data.TabInfo
import com.treevalue.beself.net.SiteInfo
import com.treevalue.beself.net.SiteStatus
import com.treevalue.beself.persistence.BrowserState
import com.treevalue.beself.persistence.RecordManager
import com.treevalue.beself.persistence.PersistenceManager
import com.treevalue.beself.persistence.PersistentSiteInfo
import com.treevalue.beself.persistence.PersistentTabInfo
import com.treevalue.beself.ui.DownloadIndicatorState
import com.treevalue.beself.bus.DownloadEvent
import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.EventId
import com.treevalue.beself.bus.TabEvent
import com.treevalue.beself.net.DownloadStatus
import com.treevalue.beself.net.DownloadTask
import com.treevalue.beself.net.FileUrlDetector
import com.treevalue.beself.net.NetworkCrawler
import com.treevalue.beself.net.RealDownloadManager
import com.treevalue.beself.net.getHostnameFromUrl
import com.treevalue.beself.net.isValidDomain
import com.treevalue.beself.net.isValidHostname
import com.treevalue.beself.net.isValidUrl
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import com.treevalue.beself.util.dw
import com.treevalue.beself.web.LoadingState
import com.treevalue.beself.web.ServiceProvider
import com.treevalue.beself.web.WebViewController
import com.treevalue.beself.web.WebViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


data class ConfirmDialogState(
    val title: String, val message: String, val onConfirm: () -> Unit, val onCancel: () -> Unit,
)

class InterceptRequestBackend private constructor(
    private val scope: CoroutineScope,
) : ServiceProvider {
    companion object {
        @Volatile
        private var INSTANCE: InterceptRequestBackend? = null

        fun getInstance(scope: CoroutineScope): InterceptRequestBackend {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InterceptRequestBackend(scope).also { INSTANCE = it }
            }
        }

        fun getInstance(): InterceptRequestBackend? {
            return INSTANCE
        }
    }

    private val _customHomeText = mutableStateOf("Hee")
    val customHomeText = _customHomeText

    // 添加持久化管理器
    private val persistenceManager = PersistenceManager()

    // UI 状态
    private val _forceDark = mutableStateOf(false)
    val forceDark = _forceDark

    private val _sidebarVisible = mutableStateOf(true)
    val sidebarVisible = _sidebarVisible

    private val _showPop = mutableStateOf(false)
    val showPop = _showPop

    // Tab 相关状态
    private val _tabs = mutableStateListOf<TabInfo>()
    val tabs = _tabs

    private val _activeTabIndex = mutableIntStateOf(0)
    val activeTabIndex = _activeTabIndex

    private val _tabStateMap = mutableStateMapOf<String, Pair<WebViewState, WebViewController>>()

    // 当前激活的 Tab 和 Navigator
    private val _activeNavigator = mutableStateOf<WebViewController?>(null)
    val activeNavigator = _activeNavigator

    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _dynamicSites = mutableStateListOf<SiteInfo>()
    val dynamicSites = _dynamicSites

    // 网站验证任务管理
    private val verificationJobs = mutableMapOf<String, Job>()

    // 消息提示
    private val _toastMessage = mutableStateOf<String?>(null)
    val toastMessage = _toastMessage

    // 确认对话框
    private val _confirmDialog = mutableStateOf<ConfirmDialogState?>(null)
    val confirmDialog = _confirmDialog

    private val _excludedSiteIds = mutableStateListOf<String>()
    val excludedSiteIds = _excludedSiteIds

    // 隐藏网站的状态管理
    private val _hiddenSiteIds = mutableStateListOf<String>()
    val hiddenSiteIds = _hiddenSiteIds

    private val recordManager = RecordManager()

    private val _startPageSiteId = mutableStateOf<String?>(null)
    val startPageSiteId = _startPageSiteId

    val searchState = SearchState()

    private val _blockedSites = mutableStateListOf<String>()
    val blockedSites = _blockedSites

    private val _availableDownloads = mutableStateListOf<Pair<String, String>>() // (url, filename)
    val availableDownloads: List<Pair<String, String>> = _availableDownloads

    // 添加下载边栏状态
    private val _downloadSidebarVisible = mutableStateOf(false)
    val downloadSidebarVisible = _downloadSidebarVisible

    private val realDownloadManager = RealDownloadManager(scope)
    val downloadTasks: List<DownloadTask> = realDownloadManager.downloadTasks

    private val _downloadIndicatorState = mutableStateOf(DownloadIndicatorState.NONE)
    val downloadIndicatorState = _downloadIndicatorState

    // 添加自定义正则式状态
    private val _customRegexPatterns = mutableStateListOf<String>()
    val customRegexPatterns: List<String> = _customRegexPatterns

    private val _hasViewedDownloadSidebar = mutableStateOf(false)
    val hasViewedDownloadSidebar = _hasViewedDownloadSidebar

    private val networkCrawler = NetworkCrawler()

    private val _featureSettings = mutableStateOf(FeatureSettings())
    val featureSettings = _featureSettings

    private val featureLimits = FeatureLimits()

    private var videoTimerJob: Job? = null

    // 视频计时器速度控制
    private var videoTimerSpeed = 1.0f
    private var videoDuration = 0L

    override fun isStillVideoEnable(): Boolean {
        return _featureSettings.value.videoEnabled && getRemainingVideoTimeToday() > 0
    }

    /**
     * 设置视频计时器速度
     * @param speed 速度倍率，1.0为正常速度，0为暂停，5为5倍速
     */
    override fun setVideoLimiterSpeed(speed: Boolean) {
        if (!featureSettings.value.videoEnabled) {
            return
        }
        videoTimerSpeed = getVideoLimiterSpeed()
    }

    /**
     * 检查当前网站是否在正常网站过滤器中
     */
    private fun getVideoLimiterSpeed(): Float {
        val activeTabInfo = getActiveTabInfo()
        val currentUrl = activeTabInfo?.initialUrl ?: return 0.0f
        val hostname = getHostnameFromUrl(currentUrl)

        val isNormalSpeed = featureLimits.longVideoTimeSites.any { filter ->
            hostname.contains(filter, ignoreCase = true)
        }
        return if (isNormalSpeed) {
            1.0f
        } else {
            2.0f
        }
    }

    fun getRemainingVideoTimeToday(): Long {
        val settings = _featureSettings.value

        // 检查是否距离上次重置已过24小时
        val now = System.currentTimeMillis()
        val lastResetTime = settings.videoUsageResetTime
        val has24HoursPassed = (now - lastResetTime) >= (24 * 60 * 60 * 1000L)

        // 如果已过24小时，返回完整限制时间
        if (has24HoursPassed) {
            return featureLimits.videoLimit
        }

        return maxOf(0L, featureLimits.videoLimit - settings.videoUsageToday)
    }

    fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }

    fun enableVideo(): Boolean {
        val remainingTime = getRemainingVideoTimeToday()
        if (remainingTime <= 0) {
            showToast("今日视频时间已用完")
            return false
        }

        val currentTime = System.currentTimeMillis()
        val currentDate = getCurrentDateString()

        // 如果是新的一天，重置使用时间
        val now = System.currentTimeMillis()
        val lastResetTime = _featureSettings.value.videoUsageResetTime ?: 0L
        val resetUsage = (now - lastResetTime) >= (24 * 60 * 60 * 1000L)

        _featureSettings.value = _featureSettings.value.copy(
            videoEnabled = true,
            videoSessionStartTime = now,
            lastUsageDate = currentDate,
            videoUsageToday = if (resetUsage) 0L else _featureSettings.value.videoUsageToday,
            videoUsageResetTime = if (resetUsage) now else _featureSettings.value.videoUsageResetTime ?: now
        )
        startVideoTimer()
        saveState()

        val limitMinutes = featureLimits.videoLimit / (60 * 1000)
        showToast("视频功能已开启，今日限制${limitMinutes}分钟")
        _activeNavigator.value?.reload()
        toggleForceDark()
        return true
    }

    fun disableVideo() {
        stopVideoTimer()

        if (videoDuration > 0) {
            val newUsageTime = _featureSettings.value.videoUsageToday + videoDuration

            _featureSettings.value = _featureSettings.value.copy(
                videoEnabled = false, videoSessionStartTime = null, videoUsageToday = newUsageTime
            )
        } else {
            _featureSettings.value = _featureSettings.value.copy(
                videoEnabled = false, videoSessionStartTime = null
            )
        }

        saveState()

        showToast("视频已关闭")
        _activeNavigator.value?.reload()
    }

    fun switchVideoEnable(): Boolean {
        return if (_featureSettings.value.videoEnabled) {
            disableVideo()
            false
        } else {
            enableVideo()
        }
    }

    private fun startVideoTimer() {
        stopVideoTimer()
        videoDuration = 0L

        videoTimerJob = scope.launch {
            while (_featureSettings.value.videoEnabled) {
                delay(1000)

                if (videoTimerSpeed > 0) {
                    videoDuration += (1000 * videoTimerSpeed).toLong()

                    val totalUsedTime = _featureSettings.value.videoUsageToday + videoDuration
                    val limit = featureLimits.videoLimit

                    if (totalUsedTime >= limit) {
                        scope.launch {
                            disableVideo()
                            showToast("视频时间已用完")
                        }
                        break
                    }
                }
                // 如果速度为0，则只延迟但不增加时间
            }
        }
    }

    private fun stopVideoTimer() {
        videoTimerJob?.cancel()
        videoTimerJob = null
    }

    fun checkUrlForDownload(url: String) {
        if (FileUrlDetector.isDownloadableUrl(url)) {
            val filename = FileUrlDetector.extractFilename(url)
            handleDownloadUrlIntercept(url, filename)
        }
    }

    fun startDownload(url: String, filename: String) {
        // 从可下载列表中移除
        _availableDownloads.removeAll { it.first == url }

        // 创建下载任务并立即添加到 realDownloadManager
        val downloadTask = DownloadTask(
            id = "download_${System.currentTimeMillis()}",
            url = url,
            fileName = filename,
            status = DownloadStatus.DOWNLOADING,
            progress = 0f
        )

        // 先添加任务到下载管理器
        if (!realDownloadManager.addDownloadTask(downloadTask)) {
            return
        }
        updateDownloadIndicatorState()
        showToast("开始下载: $filename")

        // 启动下载
        scope.launch {
            try {
                networkCrawler.downloadFile(url) { progress, errorMsg ->
                    if (errorMsg != null) {
                        // 下载失败
                        realDownloadManager.updateDownloadStatus(downloadTask.id, DownloadStatus.FAILED, progress)
                        updateDownloadIndicatorState()
                        showToast("下载失败: $errorMsg")
                    } else {
                        // 更新进度
                        if (progress >= 1f) {
                            // 下载完成
                            realDownloadManager.updateDownloadStatus(downloadTask.id, DownloadStatus.COMPLETED, 1f)
                            updateDownloadIndicatorState()
                            showToast("下载完成: $filename")
                        } else {
                            // 更新进度
                            realDownloadManager.updateDownloadStatus(
                                downloadTask.id, DownloadStatus.DOWNLOADING, progress
                            )
                            updateDownloadIndicatorState()
                        }
                    }
                }

            } catch (e: Exception) {
                realDownloadManager.updateDownloadStatus(downloadTask.id, DownloadStatus.FAILED, 0f)
                updateDownloadIndicatorState()
                showToast("下载失败: ${e.message}")
            }
        }
    }

    private fun updateDownloadIndicatorState() {
        val hasDownloading = downloadTasks.any { it.status == DownloadStatus.DOWNLOADING }
        val hasAvailable = _availableDownloads.isNotEmpty()
        val hasCompleted = downloadTasks.any { it.status == DownloadStatus.COMPLETED }

        _downloadIndicatorState.value = when {
            hasDownloading -> DownloadIndicatorState.DOWNLOADING
            // 只有在下载栏关闭时才显示红点
            !_downloadSidebarVisible.value && !_hasViewedDownloadSidebar.value && (hasAvailable || hasCompleted) -> DownloadIndicatorState.HAS_NEW
            else -> DownloadIndicatorState.NONE
        }
    }

    fun removeAvailableDownload(url: String) {
        _availableDownloads.removeAll { it.first == url }
        updateDownloadIndicatorState()
    }

    private fun addAvailableDownload(url: String, filename: String) {
        val download = Pair(url, filename)
        if (!_availableDownloads.contains(download)) {
            _availableDownloads.add(download)
            _hasViewedDownloadSidebar.value = false
            updateDownloadIndicatorState()
        }
    }

    fun toggleDownloadSidebar() {
        _downloadSidebarVisible.value = !_downloadSidebarVisible.value
        if (_downloadSidebarVisible.value) {
            _sidebarVisible.value = false
            _hasViewedDownloadSidebar.value = true
        } else {
            clearUnstartedDownloads()
        }
        updateDownloadIndicatorState()
    }

    private fun clearUnstartedDownloads() {
        _availableDownloads.clear()
    }

    // 删除下载任务
    fun deleteDownloadTask(task: DownloadTask) {
        realDownloadManager.deleteDownload(task.id)
        updateDownloadIndicatorState()
    }

    // 暂停下载任务
    fun pauseDownloadTask(task: DownloadTask) {
        realDownloadManager.pauseDownload(task.id)
        updateDownloadIndicatorState()
    }

    // 恢复/重试下载任务
    fun resumeDownloadTask(task: DownloadTask) {
        realDownloadManager.resumeDownload(task.id)
        updateDownloadIndicatorState()
    }

    init {
        initializeData()
        EventBus.registerHandler<TabEvent.RequestNewTab>(EventId.NewTab) { event ->
            handleNewTabRequest(event.url, event.title)
        }
        EventBus.registerHandler<DownloadEvent>(EventId.Download) { event ->
            when (event) {
                is DownloadEvent.DownloadAvailable -> handleDownloadUrlIntercept(event.url, event.filename)
                is DownloadEvent.AutoStartDownload -> startDownload(event.url, event.filename)
                is DownloadEvent.DownloadStarted -> updateDownloadIndicatorState()
                is DownloadEvent.DownloadCompleted -> updateDownloadIndicatorState()
                is DownloadEvent.DownloadFailed -> updateDownloadIndicatorState()
                is DownloadEvent.DownloadProgress -> {}
            }
        }
    }

    private fun handleDownloadUrlIntercept(url: String, filename: String) {
        scope.launch {
            addAvailableDownload(url, filename)
            updateDownloadIndicatorState()
        }
    }

    private fun handleNewTabRequest(url: String, title: String? = null) {
        scope.launch {
            if (isUrlAllowed(url) || FileUrlDetector.isDownloadableUrl(url)) {
                val finalTitle = title?.takeIf { it.isNotBlank() } ?: getHostnameFromUrl(url)
                addNewTab(url, finalTitle)
            }
        }
    }


    fun removeCustomRegexPattern(pattern: String): Boolean {
        return if (_customRegexPatterns.contains(pattern)) {
            _customRegexPatterns.remove(pattern)
            saveState()
            true
        } else {
            false
        }
    }

    private fun initializeData() {
        scope.launch {
            // 首先尝试恢复保存的状态
            val savedState = persistenceManager.loadBrowserState()
            if (savedState != null) {
                restoreFromSavedState(savedState)
                _isInitialized.value = true


                // 监听新的 URL
                UrlReceiver.urlHotFlow.collect { newUrl ->
                    newUrl?.let {
                        handleNewTabRequest(newUrl.url)
                    }
                }
                return@launch
            }

            // 如果没有保存的状态，执行原有的初始化逻辑
            val isInit = UrlReceiver.canExecOnce()
            if (isInit) {
                val newUrl = UrlReceiver.getAndClearUrl()
                if (!newUrl.isNullOrEmpty()) {
                    val newTab = TabInfo(
                        initialUrl = newUrl, title = mutableStateOf(getHostnameFromUrl(newUrl))
                    )
                    _tabs.add(newTab)
                    _isInitialized.value = true
                    return@launch
                }
            }

            if (_tabs.isEmpty()) {
                createInitialTab()
            }

            _isInitialized.value = true

            // 监听新的 URL
            UrlReceiver.urlHotFlow.collect { newUrl ->
                newUrl?.let {
                    handleNewTabRequest(newUrl.url)
                }
            }
        }
    }

    fun isNameDuplicate(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false

        // 检查静态站点中是否有相同名称
        val existsInStatic = BrowserConfig.ALLOWED_SITES.any { site ->
            val siteId = "static_${site.label}_${site.host}"
            !_excludedSiteIds.contains(siteId) &&  // 排除已删除的
                    site.label.equals(trimmedName, ignoreCase = true)
        }

        if (existsInStatic) return true

        // 检查动态站点中是否有相同名称（排除已删除的站点）
        val existsInDynamic = _dynamicSites.any { siteInfo ->
            !_excludedSiteIds.contains(siteInfo.id) && siteInfo.label.equals(trimmedName, ignoreCase = true)
        }

        return existsInDynamic
    }

    private fun restoreFromSavedState(savedState: BrowserState) {
        _forceDark.value = savedState.forceDark
        _sidebarVisible.value = savedState.sidebarVisible
        _customHomeText.value = savedState.customHomeText

        // 恢复标签页
        _tabs.clear()
        if (savedState.tabs.isNotEmpty()) {
            savedState.tabs.forEach { persistentTab ->
                val tabInfo = TabInfo(
                    id = persistentTab.id,
                    initialUrl = persistentTab.initialUrl,
                    initialHtml = persistentTab.initialHtml,
                    title = mutableStateOf(persistentTab.title),
                )
                _tabs.add(tabInfo)
            }
        } else {
            _tabs.add(TabInfo(initialHtml = BrowserConfig.INITIAL_HTML))
        }

        _activeTabIndex.intValue = savedState.activeTabIndex.coerceIn(0, _tabs.size - 1)

        _dynamicSites.clear()
        savedState.dynamicSites.forEach { persistentSite ->
            val siteInfo = SiteInfo(
                id = persistentSite.id,
                label = persistentSite.label,
                host = persistentSite.host,
                status = persistentSite.status,
                originalUrl = persistentSite.originalUrl
            )
            _dynamicSites.add(siteInfo)
        }

        _excludedSiteIds.clear()
        _excludedSiteIds.addAll(savedState.excludedSiteIds)

        _hiddenSiteIds.clear()
        _hiddenSiteIds.addAll(savedState.hiddenSiteIds)
        _startPageSiteId.value = savedState.startPageSiteId

        _blockedSites.clear()
        _blockedSites.addAll(savedState.blockedSites)

        _customRegexPatterns.clear()
        _customRegexPatterns.addAll(savedState.customRegexPatterns)

        _featureSettings.value = savedState.featureSettings

        if (_featureSettings.value.videoEnabled) {
            startVideoTimer()
        }
    }

    // 获取当前状态用于保存
    private fun getCurrentState(): BrowserState {
        val persistentTabs = _tabs.map { tabInfo ->
            PersistentTabInfo(
                id = tabInfo.id,
                initialUrl = tabInfo.initialUrl,
                initialHtml = tabInfo.initialHtml,
                title = tabInfo.title.value,
            )
        }

        val persistentSites = _dynamicSites.map { siteInfo ->
            PersistentSiteInfo(
                id = siteInfo.id,
                label = siteInfo.label,
                host = siteInfo.host,
                status = siteInfo.status,
                originalUrl = siteInfo.originalUrl
            )
        }

        return BrowserState(
            tabs = persistentTabs,
            activeTabIndex = _activeTabIndex.intValue,
            dynamicSites = persistentSites,
            excludedSiteIds = _excludedSiteIds.toList(),
            hiddenSiteIds = _hiddenSiteIds.toList(),
            forceDark = _forceDark.value,
            sidebarVisible = _sidebarVisible.value,
            startPageSiteId = _startPageSiteId.value,
            blockedSites = _blockedSites.toList(),
            customRegexPatterns = _customRegexPatterns.toList(),
            featureSettings = _featureSettings.value,
            customHomeText = _customHomeText.value,
        )
    }

    fun setCustomHomeText(text: String) {
        _customHomeText.value = text
        saveState()
    }

    fun getCustomHomeText(): String {
        return _customHomeText.value
    }

    fun addCustomRegexPattern(pattern: String): Boolean {
        val trimmedPattern = pattern.trim()
        if (trimmedPattern.isNotEmpty() && !_customRegexPatterns.contains(trimmedPattern)) {
            try {
                // 验证正则表达式是否有效
                Regex(trimmedPattern, RegexOption.IGNORE_CASE)
                _customRegexPatterns.add(trimmedPattern)
                saveState()
                return true
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    fun blockSites(inputs: List<String>) {
        var blockedCount = 0
        inputs.forEach { input ->
            val trimmedInput = input.trim()
            if (trimmedInput.isNotEmpty() && !_blockedSites.contains(trimmedInput)) {
                _blockedSites.add(trimmedInput)
                blockedCount++
            }
        }
        if (blockedCount > 0) {
            saveState()
        }
    }

    override fun isUrlAllowed(url: String): Boolean {
        if (!isValidUrl(url)) return false
        if (isUrlBlocked(url)) return false

        val hostname = getHostnameFromUrl(url)

        val isCanAdd = runBlocking { recordManager.canAddSiteQuick(hostname) }
        if (!isCanAdd) {
            return false
        }

        // 检查动态网站
        val inDynamicSites = _dynamicSites.any { siteInfo ->
            !_excludedSiteIds.contains(siteInfo.id) && !_hiddenSiteIds.contains(siteInfo.id) && !isSiteInfoBlocked(
                siteInfo
            ) && siteInfo.status == SiteStatus.COMPLETED && hostname.equals(siteInfo.host, ignoreCase = true)
        }

        if (inDynamicSites) {
            return true
        }
        if (_customRegexPatterns.any { pattern ->
                try {
                    Regex(pattern, RegexOption.IGNORE_CASE).matches(url)
                } catch (e: Exception) {
                    false
                }
            }) return true
        // 检查静态网站
        val inStaticSites = BrowserConfig.ALLOWED_SITES.any { config ->
            hostname.equals(config.host, ignoreCase = true)
        }
        if (inStaticSites) return true
        if (ALLOWED_PATTERNS.any { it.matches(url) }) return true

        return false
    }

    override fun isUrlBlocked(url: String): Boolean {
        val hostname = getHostnameFromUrl(url)

        return _blockedSites.any { blockedItem ->
            when {
                isValidUrl(blockedItem) -> {
                    url.startsWith(blockedItem, ignoreCase = true)
                }

                isValidDomain(blockedItem) || isValidHostname(blockedItem) -> {
                    hostname.equals(blockedItem, ignoreCase = true)
                }

                else -> false
            }
        }
    }

    override fun isDarkMode(): Boolean {
        return forceDark.value
    }

    fun isSiteInfoBlocked(siteInfo: SiteInfo): Boolean {
        val siteUrl = "https://${siteInfo.host}"
        return isUrlBlocked(siteUrl) || isUrlBlocked(siteInfo.host)
    }

    fun setStartPageSetting(siteId: String?) {
        _startPageSiteId.value = siteId
        saveState()
    }

    fun getStartPageSetting(): String? {
        return _startPageSiteId.value
    }

    private fun createInitialTab() {
        _tabs.add(createDefaultTab())
    }

    // 保存当前状态
    fun saveState() {
        scope.launch {
            try {
                val currentState = getCurrentState()
                persistenceManager.requestPermissionAndSave(currentState)
            } catch (e: Exception) {

            }
        }
    }

    fun toggleForceDark() {
        _forceDark.value = !_forceDark.value
        saveState()
    }

    fun toggleSidebar() {
        _sidebarVisible.value = !_sidebarVisible.value
        if (_sidebarVisible.value) {
            _downloadSidebarVisible.value = false
        }
        saveState()
    }

    fun setShowPop(show: Boolean) {
        _showPop.value = show
    }

    // Tab 操作方法
    fun selectTab(index: Int) {
        try {
            if (index >= 0 && index < _tabs.size) {
                _activeTabIndex.intValue = index
                saveState()
                KLogger.dd { "切换到标签页: $index" }
            } else {
                KLogger.dw { "无效的标签页索引: $index, 当前标签页数量: ${_tabs.size}" }
            }
        } catch (e: Exception) {
            KLogger.de { "切换标签页失败: ${e.message}" }
        }
    }

    fun closeAllTabs() {
        try {
            _tabs.forEach { tab ->
                EventBus.publish(TabEvent.TabClosed(tab.id))
            }
            _tabStateMap.clear()
            _tabs.clear()
            _activeTabIndex.intValue = 0
            _activeNavigator.value = null
            createInitialTab()
            saveState()
        } catch (e: Exception) {

        }
    }

    fun closeTab(tabInfo: TabInfo) {
        try {
            val currentIndex = _tabs.indexOf(tabInfo)
            if (currentIndex < 0) {
                KLogger.dw { "要关闭的标签页不存在: ${tabInfo.title.value}" }
                return
            }

            EventBus.publish(TabEvent.TabClosed(tabInfo.id))

            // 清理状态
            _tabStateMap.remove(tabInfo.id)
            _tabs.removeAt(currentIndex)

            // 安全地调整活动标签页索引
            when {
                _tabs.isEmpty() -> {
                    _activeTabIndex.intValue = 0
                    createInitialTab()
                }

                _activeTabIndex.intValue >= _tabs.size -> {
                    _activeTabIndex.intValue = maxOf(0, _tabs.size - 1)
                }

                _activeTabIndex.intValue > currentIndex -> {
                    _activeTabIndex.intValue = _activeTabIndex.intValue - 1
                }
            }

            saveState()
            KLogger.dd { "成功关闭标签页: ${tabInfo.title.value}, 剩余: ${_tabs.size}" }
        } catch (e: Exception) {
            KLogger.de { "关闭标签页失败: ${e.message}" }
        }
    }

    fun addNewTab(url: String? = null, label: String? = null) {
        scope.launch {
            try {
                val newTab = if (url != null) {
                    TabInfo(
                        initialUrl = url, title = mutableStateOf(label ?: getHostnameFromUrl(url))
                    )
                } else {
                    createDefaultTab()
                }

                _tabs.add(newTab)

                // 安全地更新活动标签页索引
                val newIndex = _tabs.size - 1
                if (newIndex >= 0 && newIndex < _tabs.size) {
                    _activeTabIndex.intValue = newIndex
                }
                saveState()
                KLogger.dd { "成功添加新标签页: $url, 当前标签页数量: ${_tabs.size}" }
            } catch (e: Exception) {
                KLogger.de { "添加新标签页失败: ${e.message}" }
            }
        }
    }

    private fun createDefaultTab(): TabInfo {
        if (_startPageSiteId.value != null) {
            val startSite = getAllSitesIncludeHidden().find { it.id == _startPageSiteId.value }
            if (startSite != null) {
                return TabInfo(
                    initialUrl = startSite.originalUrl ?: "https://${startSite.host}",
                    title = mutableStateOf(startSite.label)
                )
            }
        }
        return TabInfo(
            initialHtml = BrowserConfig.getInitialHTML(_customHomeText.value),
            title = mutableStateOf("Home")
        )
    }

    fun addSiteFromSidebar(label: String, urlOrHost: String) {
        if (isValidUrl(urlOrHost)) {
            addNewTab(urlOrHost, label)
        } else if (isValidDomain(urlOrHost)) {
            addNewTab("https://$urlOrHost", label)
        } else {
            showToast("error: $label<->$urlOrHost")
        }
    }

    // WebView 状态管理
    fun cacheTabState(tabId: String, state: WebViewState, navigator: WebViewController) {
        _tabStateMap[tabId] = state to navigator
    }

    fun getTabState(tabId: String): Pair<WebViewState, WebViewController>? {
        return _tabStateMap[tabId]
    }

    fun setActiveNavigator(navigator: WebViewController?) {
        _activeNavigator.value = navigator
    }

    // 获取当前激活的 Tab 信息
    fun getActiveTabInfo(): TabInfo? {
        return try {
            val index = _activeTabIndex.intValue
            if (index >= 0 && index < _tabs.size) {
                _tabs[index]
            } else {
                KLogger.dw { "活动标签页索引越界: $index, 标签页数量: ${_tabs.size}" }
                // 尝试修复索引
                if (_tabs.isNotEmpty()) {
                    val correctedIndex = 0.coerceAtMost(_tabs.size - 1)
                    _activeTabIndex.intValue = correctedIndex
                    _tabs[correctedIndex]
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            KLogger.de { "获取活动标签页失败: ${e.message}" }
            null
        }
    }

    fun getActiveTabState(): Pair<WebViewState, WebViewController>? {
        val activeTabInfo = getActiveTabInfo()
        return activeTabInfo?.id?.let { _tabStateMap[it] }
    }

    fun addSite(label: String, url: String): Boolean {
        val host = getHostnameFromUrl(url)

        // 生成唯一 ID
        val siteId = "${label}_${host}_${System.currentTimeMillis()}"

        val siteInfo = SiteInfo(
            id = siteId, label = label, host = host, status = SiteStatus.PENDING, originalUrl = url  // 保存完整URL
        )

        _dynamicSites.add(siteInfo)
        saveState()

        showToast("正在添加网站: $label")

        // 创建新 Tab 来验证网站
        val newTab = TabInfo(
            initialUrl = url, title = mutableStateOf(label)
        )
        _tabs.add(newTab)
        _activeTabIndex.intValue = _tabs.lastIndex
        saveState()

        // 启动验证任务
        startSiteVerification(siteInfo.id, newTab.id)
        return true
    }

    // 启动网站验证任务
    private fun startSiteVerification(siteId: String, tabId: String) {
        val job = scope.launch {
            val maxRetries = 3
            var currentRetry = 0
            var isSuccess = false
            val startTime = System.currentTimeMillis()

            while (currentRetry < maxRetries && !isSuccess) {
                delay(2000) // 增加等待时间

                val tabState = _tabStateMap[tabId]
                if (tabState != null) {
                    val (state, navigator) = tabState
                    when (state.loadingState) {
                        is LoadingState.Finished -> {
                            val url = state.lastLoadedUrl

                            // 更宽松的错误检查逻辑
                            val hasCriticalError = state.errorsForCurrentRequest.any { error ->
                                val description = error.description

                                // 只检查真正的网络错误，忽略一些常见的非关键错误
                                when {
                                    // 跳过ORB错误（这通常不影响页面正常显示）
                                    description.contains("ERR_BLOCKED_BY_ORB", ignoreCase = true) -> false
                                    // 跳过一些资源加载错误
                                    description.contains("ERR_BLOCKED_BY_CLIENT", ignoreCase = true) -> false
                                    description.contains("ERR_ABORTED", ignoreCase = true) -> false
                                    // 检查真正的网络连接错误
                                    description.contains("ERR_NETWORK_CHANGED", ignoreCase = true) -> true
                                    description.contains("ERR_INTERNET_DISCONNECTED", ignoreCase = true) -> true
                                    description.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) -> true
                                    description.contains("ERR_CONNECTION_REFUSED", ignoreCase = true) -> true
                                    description.contains("ERR_CONNECTION_TIMED_OUT", ignoreCase = true) -> true
                                    description.contains("ERR_CONNECTION_RESET", ignoreCase = true) -> true
                                    // 检查HTTP错误（但404可能是页面的一部分，不一定是错误）
                                    error.code in listOf(-105, -106, -109, -118) -> true // 具体的网络错误代码
                                    else -> false
                                }
                            }

                            // 简化成功判断条件
                            val isValidUrl = url != null && (url.startsWith("https://") || url.startsWith("http://"))

                            // 检查是否是明显的错误页面（更精确的检查）
                            val isErrorPage = url?.let { currentUrl ->
                                // 检查URL是否包含明显的错误标识
                                val errorPatterns = listOf(
                                    "/404", "/error", "/not-found", "error=", "errorcode="
                                )
                                errorPatterns.any { pattern -> currentUrl.contains(pattern, ignoreCase = true) }
                            } ?: false

                            // 新的成功判断逻辑
                            if (isValidUrl && !hasCriticalError && !isErrorPage) {

                                isSuccess = true
                                updateSiteStatus(siteId, SiteStatus.COMPLETED)
                                saveState()
                                showToast("网站添加成功: ${_dynamicSites.find { it.id == siteId }?.label}")
                            } else {

                                currentRetry++
                                if (currentRetry < maxRetries) {
                                    showToast("网站验证中，正在重试...")
                                    navigator.reload()
                                }
                            }
                        }

                        is LoadingState.Loading, LoadingState.Initializing -> {
                            // 检查是否已经等待太久
                            val elapsed = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
                            if (elapsed > 8000) {

                                if (state.lastLoadedUrl != null && isValidUrl(state.lastLoadedUrl!!)) {
                                    isSuccess = true
                                    updateSiteStatus(siteId, SiteStatus.COMPLETED)
                                    saveState()
                                    showToast("网站添加成功")
                                }
                                break
                            } else {

                                delay(1000)
                            }
                        }

                        else -> {

                            currentRetry++
                            if (currentRetry < maxRetries) {
                                showToast("加载失败，正在重试...")
                                navigator.reload()
                            }
                        }
                    }
                } else {

                    currentRetry++
                    delay(1000)
                }
            }

            if (!isSuccess) {
                val siteInfo = _dynamicSites.find { it.id == siteId }
                if (siteInfo != null) {

                    updateSiteStatus(siteId, SiteStatus.FAILED)
                    saveState()
                    showToast("网站添加失败：无法访问该网址")
                }
            }
        }

        verificationJobs[siteId] = job
    }

    // 区分失败网站和正常网站
    fun requestDeleteSite(siteInfo: SiteInfo) {
        scope.launch {
            if (siteInfo.status == SiteStatus.FAILED) {
                // 失败网站的删除逻辑保持不变
                _confirmDialog.value = ConfirmDialogState(title = "删除失败网站",
                    message = "确定要删除失败的网站 \"${siteInfo.label}\" 吗？\n\n此操作不会影响未来添加该网站。",
                    onConfirm = {
                        scope.launch { deleteFailedSite(siteInfo) }
                        _confirmDialog.value = null
                    },
                    onCancel = {
                        _confirmDialog.value = null
                        _showPop.value = false
                    })
                _showPop.value = true
                return@launch
            }

            // 获取当前删除记录（删除前的状态）
            val currentRecords = recordManager.loadDeleteRecords()
            val currentRecord = currentRecords.records.find { it.hostOrId == siteInfo.host }
            val currentDeleteCount = currentRecord?.deleteCount ?: 0

            // 预测删除后的限制（删除次数+1）
            val futureDeleteCount = currentDeleteCount + 1
            val (title, message) = when (futureDeleteCount) {
                1 -> {
                    val timeInfo = if (currentDeleteCount > 0) {
                        val remainingTime =
                            getRemainingTimeString(currentRecord!!.lastDeleteTime, 30 * 24 * 60 * 60 * 1000L)
                        "\n\n当前已删除 ${currentDeleteCount} 次，$remainingTime"
                    } else ""

                    "删除网站" to "确定要删除 \"${siteInfo.label}\" 吗？\n\n⚠️ 警告：删除后一个月内无法再添加此网站！$timeInfo"
                }

                2 -> {
                    val timeInfo = if (currentDeleteCount > 0) {
                        val remainingTime =
                            getRemainingTimeString(currentRecord!!.lastDeleteTime, 6 * 30 * 24 * 60 * 60 * 1000L)
                        "\n\n当前已删除 ${currentDeleteCount} 次，$remainingTime"
                    } else ""

                    "删除网站" to "确定要删除 \"${siteInfo.label}\" 吗？\n\n⚠️ 警告：删除后半年内无法再添加此网站！$timeInfo"
                }

                else -> {
                    val timeInfo = if (currentDeleteCount > 0) {
                        "\n\n当前已删除 ${currentDeleteCount} 次"
                    } else ""

                    "永久删除网站" to "确定要删除 \"${siteInfo.label}\" 吗？\n\n🚨 严重警告：删除后将永远无法再添加此网站！$timeInfo"
                }
            }

            _confirmDialog.value = ConfirmDialogState(title = title, message = message, onConfirm = {
                scope.launch { deleteSite(siteInfo) }
                _confirmDialog.value = null
            }, onCancel = {
                _confirmDialog.value = null
                _showPop.value = false
            })
            _showPop.value = true
        }
    }

    private fun getRemainingTimeString(lastDeleteTime: Long, restrictionPeriod: Long): String {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastDeleteTime
        val remaining = restrictionPeriod - elapsed

        return if (remaining > 0) {
            val days = remaining / (24 * 60 * 60 * 1000)
            val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            "剩余限制时间：${days}天${hours}小时"
        } else {
            "限制时间已过"
        }
    }

    private suspend fun deleteSite(siteInfo: SiteInfo) {
        try {
            // 记录删除操作
            val deleteRecord = recordManager.recordDelete(siteInfo.host)

            // 取消验证任务
            verificationJobs[siteInfo.id]?.cancel()
            verificationJobs.remove(siteInfo.id)

            // 检查是否为动态网站
            val isDynamicSite = _dynamicSites.any { it.id == siteInfo.id }

            if (isDynamicSite) {
                // 从动态网站列表中删除
                _dynamicSites.removeAll { it.id == siteInfo.id }
            } else {
                // 如果是静态网站，添加到排除列表
                if (!_excludedSiteIds.contains(siteInfo.id)) {
                    _excludedSiteIds.add(siteInfo.id)
                }
            }

            if (_startPageSiteId.value == siteInfo.id) {
                _startPageSiteId.value = null
            }

            saveState()

            // 根据删除次数显示不同的提示
            val message = when (deleteRecord.deleteCount) {
                1 -> "已删除网站: ${siteInfo.label}（一个月内无法再添加）"
                2 -> "已删除网站: ${siteInfo.label}（半年内无法再添加）"
                else -> "已删除网站: ${siteInfo.label}（永远无法再添加）"
            }

            showToast(message)

        } catch (e: Exception) {

            showToast("删除网站失败，请重试")
        }
    }

    suspend fun canAddSiteWithDetails(url: String): Pair<Boolean, String> {
        val host = getHostnameFromUrl(url)

        if (isUrlBlocked(url)) {
            return Pair(false, "此网站已被屏蔽，永久无法添加")
        }

        if (!recordManager.canAddSite(host)) {
            val restrictionInfo = recordManager.getDeleteRestriction(host)
            return Pair(false, restrictionInfo.message)
        }

        // 修改重复检查逻辑：检查完整URL而不是只检查hostname
        val existsInDynamic = _dynamicSites.any {
            it.originalUrl?.equals(
                url, ignoreCase = true
            ) == true || (it.originalUrl == null && it.host == host) // 兼容旧数据
        }
        val existsInStatic = BrowserConfig.ALLOWED_SITES.any {
            val siteId = "static_${it.label}_${it.host}"
            val staticUrl = "https://${it.host}"
            !_excludedSiteIds.contains(siteId) && staticUrl.equals(url, ignoreCase = true)
        }

        if (existsInDynamic || existsInStatic) {
            return Pair(false, "该网站已经存在，无法重复添加")
        }
        return Pair(true, "")
    }

    fun getAllSites(): List<SiteInfo> {
        val staticSites = BrowserConfig.ALLOWED_SITES.mapNotNull { config ->
            val siteInfo = SiteInfo(
                id = "static_${config.label}_${config.host}",
                label = config.label,
                host = config.host,
                status = SiteStatus.COMPLETED
            )
            // 过滤掉被排除、被隐藏和被屏蔽的网站
            if (!_excludedSiteIds.contains(siteInfo.id) && !_hiddenSiteIds.contains(siteInfo.id) && !isSiteInfoBlocked(
                    siteInfo
                )
            ) {
                siteInfo
            } else {
                null
            }
        }

        // 过滤掉被排除、被隐藏和被屏蔽的动态网站
        val filteredDynamicSites = _dynamicSites.filter {
            !_excludedSiteIds.contains(it.id) && !_hiddenSiteIds.contains(it.id) && !isSiteInfoBlocked(it)
        }

        return staticSites + filteredDynamicSites
    }

    fun getAllSitesIncludeHidden(): List<SiteInfo> {
        val staticSites = BrowserConfig.ALLOWED_SITES.mapNotNull { config ->
            val siteInfo = SiteInfo(
                id = "static_${config.label}_${config.host}",
                label = config.label,
                host = config.host,
                status = SiteStatus.COMPLETED
            )
            // 只过滤掉被删除的网站，保留隐藏的网站
            if (!_excludedSiteIds.contains(siteInfo.id)) siteInfo else null
        }

        val filteredDynamicSites = _dynamicSites.filter { !_excludedSiteIds.contains(it.id) }

        return staticSites + filteredDynamicSites
    }

    fun isSiteHidden(siteId: String): Boolean {
        return _hiddenSiteIds.contains(siteId)
    }

    fun toggleSiteVisibility(siteId: String) {
        if (_hiddenSiteIds.contains(siteId)) {
            _hiddenSiteIds.remove(siteId)
        } else {
            _hiddenSiteIds.add(siteId)
        }
        saveState()
    }

    private fun updateSiteStatus(siteId: String, status: SiteStatus) {
        val index = _dynamicSites.indexOfFirst { it.id == siteId }
        if (index != -1) {
            val oldSite = _dynamicSites[index]
            _dynamicSites[index] = oldSite.copy(status = status)
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
        _showPop.value = true

        scope.launch {
            delay(3000)
            if (_toastMessage.value == message) {
                clearToast()
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
        _showPop.value = false
    }

    fun forceSaveState() {
        try {
            val currentState = getCurrentState()
            runBlocking {
                persistenceManager.saveBrowserState(currentState)
            }
        } catch (e: Exception) {

        }
    }

    fun cleanup() {
        // 在清理前保存最终状态
        forceSaveState()

        stopVideoTimer()

        scope.launch {
            EventBus.unregisterHandler(EventId.NewTab)
        }

        // 取消所有验证任务
        verificationJobs.values.forEach { it.cancel() }
        verificationJobs.clear()

        // 清理所有状态
        _tabStateMap.clear()
        _tabs.clear()
        _dynamicSites.clear()
        _excludedSiteIds.clear()
    }

    // 检查当前激活的标签页是否对应一个失败的网站
    fun getActiveTabFailedSite(): SiteInfo? {
        val activeTab = getActiveTabInfo() ?: return null
        val activeUrl = activeTab.initialUrl ?: return null
        val activeHost = getHostnameFromUrl(activeUrl)

        return _dynamicSites.find { site ->
            site.status == SiteStatus.FAILED && site.host == activeHost
        }
    }

    // 重新尝试验证失败的网站
    fun retryFailedSite(siteInfo: SiteInfo) {
        val activeTab = getActiveTabInfo() ?: return

        // 更新网站状态为 PENDING
        updateSiteStatus(siteInfo.id, SiteStatus.PENDING)
        saveState()

        showToast("重新尝试验证网站: ${siteInfo.label}")

        // 重新启动验证任务
        startSiteVerification(siteInfo.id, activeTab.id)
    }

    // 删除失败的网站（不触发删除限制）
    private fun deleteFailedSite(siteInfo: SiteInfo) {
        try {
            // 注意：这里不调用 deleteRecordManager.recordDelete，因为失败的网站不应该触发删除限制

            // 取消验证任务
            verificationJobs[siteInfo.id]?.cancel()
            verificationJobs.remove(siteInfo.id)

            // 从动态网站列表中删除（失败的网站肯定是动态网站）
            _dynamicSites.removeAll { it.id == siteInfo.id }

            saveState()
            showToast("已删除失败的网站: ${siteInfo.label}")

        } catch (e: Exception) {

            showToast("删除网站失败，请重试")
        }
    }
}
