package com.treevalue.beself.backend

import com.treevalue.beself.config.LangType
import com.treevalue.beself.config.OuterConfig

sealed class Pages {

    enum class DownloadPage {
        Download,
        OpenDownloadDirectory,
        Downloadable,
        DownloadTasks,
        DeleteDownload,
        Pause,
        Resume,
        Retry,
        Remove,
        ClickToDownload,
    }

    // Help Center Page
    enum class HelpPage {
        HelpCenter,
        HelpLang,
        FeedbackEmail,
        CopyTip,
        HelpDescription,
        HelpInfo,
        HelpImg,
    }

    // Function Settings Page
    enum class FunctionPage {
        URLFormatInvalid,
        GetUpdate,
        GetLatestVersion,
        Back,
        FunctionSettings,
        AddAllow,
        AddAllowDescription,
        OpenURL,
        OpenURLDescription,
        HideSite,
        HideSiteDescription,
        BlockSite,
        BlockSiteDescription,
        GrabSite,
        GrabSiteDescription,
        StartPage,
        StartPageDescription,
        OtherFunctions,
        OtherFunctionsDescription,
        UpdatePage,
        Enter
    }

    // Add Site Page
    enum class AddSitePage {
        AddSiteRegex,
        ErrorBackendUnavailable,
        DisplayName,
        DisplayNameExists,
        CheckingName,
        WebsiteURL,
        CheckingSiteRestrictions,
        SiteAddedSuccess,
        SiteAddFailed,
        SiteAddFailedRetry,
        Checking,
        AddSite,
        AddRegex,
        RegularExpression,
        RegexSyntaxError,
        ClickToCollapseHelp,
        ViewRegexExamples,
        RegexExampleTitle,
        MatchSpecificDomain,
        MatchSpecificDomainDesc,
        MatchAllSubdomains,
        MatchAllSubdomainsDesc,
        MatchLocalNetwork,
        MatchLocalNetworkDesc,
        MatchHTTPS,
        MatchHTTPSDesc,
        MatchSpecificPort,
        MatchSpecificPortDesc,
        MatchFileTypes,
        MatchFileTypesDesc,
        RegexNote,
        ManageAddedItems,
        Collapse,
        Expand,
        CustomRegex,
        DeleteSelected,
        DeleteRegex,
        ConfirmDeleteSelected,
        RegexPatternsUndone,
        SelectedRegexDeleted,
        ConfirmDeleteRegex,
        Undone,
        RegexDeleted,
        AddedSites,
        DeleteSite,
        Sites,
        DeleteSiteWarning,
        SelectedSitesDeleted,
        ConfirmDeleteSite,
        DeleteSiteSingleWarning,
        SiteDeleted,
        NoItemsToManage,
        CopiedToClipboard,
        Copy,
        Delete,
        Status,
        Cancel,
        ConfirmDelete,
        RegexHelp,
        RegexAddedSuccess,
        RegexAddFailed,
        RegexAddFailedRetry,
        Website,
        Regex,
        Manage,
        Calculator
    }

    // Open URL Page
    enum class OpenURLPage {
        EnterValidURL,
        NotAllowedToOpen,
        EnterURLOrPath,
        Open,
        CheckingSitePermissions,
        LocalFileDetected,
        SupportsURLsAndFiles,
        SiteAccessRestricted,
        SiteNotAllowed,
        CanOpen,
        LocalFileVerified,
        Accessible,
        SupportedFileFormats,
        SupportedFileFormatsDesc
    }

    // Hide Site Page
    enum class HideSitePage {
        Info,
        Total,
        SitesHidden,
        Count,
        Show,
        Hide
    }

    // Block Site Page
    enum class BlockSitePage {
        BlockDomainOrURL,
        Block,
        Item,
        ItemCount,
        InvalidFormat,
        Details,
        BlockingRules,
        SupportsDomainsAndURLs,
        DomainBlockingDesc,
        URLBlockingDesc,
        SitesToBeBlocked,
        ConfirmBlock,
        Warning,
        BlockWarning,
        ConfirmBlockQuestion,
        Success,
        BlockedSuccessfully,
        DomainURLBlocked,
        OK,
        Failed,
        BlockFailed,
        BlockFailedRetry,
        WillBeBlocked
    }

    // Grab Site Page
    enum class GrabSitePage {
        GrabSite,
        Grab,
        EnterValidURLOrDomain,
        EnterURLToGrab,
        Download,
        OpenDownloadDirectory,
        ClickDownloadButton,
        EnterFileURL,
        DownloadProgress,
        DownloadFailed,
        GrabbingInfo,
        GrabSuccessFound,
        Copied,
        ItemsToClipboard,
        CopyAll,
        Links,
        URLsSelected,
        CopiedDomain,
        CopyDomain,
        CopiedURL,
        CopyURL,
        DownloadComplete,
        Downloadable
    }

    // Start Page Settings
    enum class StartPageSettings {
        StartPageSettings,
        CurrentStartPage,
        DefaultPage,
        RestoreDefaultPage,
        Selected,
        Hidden,
        VerificationFailed,
        Confirm,
        SetStartPage,
        ConfirmSetQuestion,
        AsStartPage,
        ConfirmRestoreDefault
    }

    // Other Functions Page
    enum class OtherFunctionsPage {
        TurnOffVideo,
        TemporarilyEnableVideo,
        Calculator,
        ScheduleManagement,
        VideoPlaybackFunction,
        Video,
        TurnOffVideoQuestion,
        EnableVideoQuestion,
        RemainingTimeToday,
        MustStop,
        TurnOff,
        Enable
    }

    // Schedule Management Page
    enum class SchedulePage {
        ScheduleManagement,
        Back,
        AddSchedule,
        EditSchedule,
        DeleteSchedule,
        ScheduleName,
        StartTime,
        EndTime,
        RepeatMode,
        Note,
        Save,
        Cancel,
        PleaseSelectStartTime,
        PleaseSelectEndTime,
        SetAsStartTime,
        SetAsEndTime,
        InvalidTime,
        PleaseConfirmTime,
        RepeatModeSettings,
        SetTaskToCyclic,
        CyclicTask,
        PleaseCheckTime,
        ConfirmDelete,
        ConfirmDeleteQuestion,
        ScheduleDeleted,
        ScheduleAdded,
        ScheduleEdited,
        PleaseEnterName,
        PleaseEnterValidRange,
        PleaseSelectDate,
        StartTimeCannotBeLater,
        AcceptSuggestion,
        Copy,
        AddScheduleButton,
        Monday,
        Tuesday,
        Wednesday,
        Thursday,
        Friday,
        Saturday,
        Sunday,
        NoSchedules,
        NoteLabel,
        Edit,
        MoveUp,
        MoveDown,
        Delete,
        NoteOptional,
        SetStartTime,
        SetEndTime,
        ScheduleType,
        Normal,
        Cyclic,
        Sequence,
        Once,
        Daily,
        SpecificDays,
        SelectWeekdays,
        Mon,
        Tue,
        Wed,
        Thu,
        Fri,
        Sat,
        Sun,
        CyclicTaskList,
        TotalDuration,
        Minutes,
        ErrorTaskDurationExceeds,
        NoTasksAddBelow,
        AddTask,
        TimeSettings,
        Start,
        End,
        AddCyclicTask,
        TaskName,
        Duration,
        DurationMinutes,
        SelectCopyStartTime,
        SelectCopyTimeDescription,
        SelectTime,
        MonthDay,
    }

    enum class SystemSettingsPage {
        SystemSettings,
        LanguageSettings,
        SelectLanguage,
        Chinese,
        English,
        CurrentLanguage,
        Settings
    }
}

data class Translation(
    val cn: String,
    val en: String,
)

object LangBackend {

    private val translations = buildTranslationMap()

    private fun buildTranslationMap(): Map<Enum<*>, Translation> = mapOf(
        // System Settings Page
        Pages.SystemSettingsPage.SystemSettings to Translation(
            cn = "系统设置",
            en = "System Settings"
        ),
        Pages.SystemSettingsPage.LanguageSettings to Translation(
            cn = "语言设置",
            en = "Language Settings"
        ),
        Pages.SystemSettingsPage.SelectLanguage to Translation(
            cn = "选择语言",
            en = "Select Language"
        ),
        Pages.SystemSettingsPage.Chinese to Translation(
            cn = "中文",
            en = "Chinese"
        ),
        Pages.SystemSettingsPage.English to Translation(
            cn = "English",
            en = "English"
        ),
        Pages.SystemSettingsPage.CurrentLanguage to Translation(
            cn = "当前语言",
            en = "Current Language"
        ),
        Pages.SystemSettingsPage.Settings to Translation(
            cn = "设置",
            en = "Settings"
        ),
        Pages.DownloadPage.Download to Translation(
            cn = "下载",
            en = "Download"
        ),
        Pages.DownloadPage.OpenDownloadDirectory to Translation(
            cn = "打开下载目录",
            en = "Open download directory"
        ),
        Pages.DownloadPage.Downloadable to Translation(
            cn = "可下载",
            en = "Downloadable"
        ),
        Pages.DownloadPage.DownloadTasks to Translation(
            cn = "下载任务",
            en = "Download Tasks"
        ),
        Pages.DownloadPage.DeleteDownload to Translation(
            cn = "删除下载",
            en = "Delete download"
        ),
        Pages.DownloadPage.Pause to Translation(
            cn = "暂停",
            en = "Pause"
        ),
        Pages.DownloadPage.Resume to Translation(
            cn = "继续",
            en = "Resume"
        ),
        Pages.DownloadPage.Retry to Translation(
            cn = "重试",
            en = "Retry"
        ),
        Pages.DownloadPage.Remove to Translation(
            cn = "移除",
            en = "Remove"
        ),
        Pages.DownloadPage.ClickToDownload to Translation(
            cn = "点击下载",
            en = "Click to download"
        ),
        // Help Page
        Pages.HelpPage.HelpCenter to Translation(
            cn = "帮助中心",
            en = "Help Center"
        ),
        Pages.HelpPage.HelpLang to Translation(
            cn = "非常高兴帮到你嘻嘻",
            en = "Very happy to help you"
        ),
        Pages.HelpPage.FeedbackEmail to Translation(
            cn = "反馈邮箱",
            en = "Feedback email"
        ),
        Pages.HelpPage.CopyTip to Translation(
            cn = "邮箱已复制到剪切板",
            en = "The mailbox has been copied to the clipboard"
        ),
        Pages.HelpPage.HelpDescription to Translation(
            cn = """1.需求请阐明具体应用场景和预期结果;
2.问题反馈请附必现条件。""",
            en = """1. Requirements: Please clarify the specific application scenarios and expected results; 
2. Please attach the conditions for feedback on questions."""
        ),
        Pages.HelpPage.HelpInfo to Translation(
            cn = "帮助信息",
            en = "Help Info"
        ),
        Pages.HelpPage.HelpImg to Translation(
            cn = "帮助图片",
            en = "Help Image"
        ),
        // Function Page
        Pages.FunctionPage.URLFormatInvalid to Translation(
            cn = "URL格式无效",
            en = "Invalid URL format"
        ),
        Pages.FunctionPage.GetUpdate to Translation(
            cn = "获取更新",
            en = "Get Update"
        ),
        Pages.FunctionPage.GetLatestVersion to Translation(
            cn = "获取最新版本",
            en = "Get Latest Version"
        ),
        Pages.FunctionPage.Back to Translation(
            cn = "返回",
            en = "Back"
        ),
        Pages.FunctionPage.FunctionSettings to Translation(
            cn = "功能设置",
            en = "Function Settings"
        ),
        Pages.FunctionPage.AddAllow to Translation(
            cn = "添加允许",
            en = "Add Allow"
        ),
        Pages.FunctionPage.AddAllowDescription to Translation(
            cn = "添加允许的网站或正则式",
            en = "Add allowed websites or regex"
        ),
        Pages.FunctionPage.OpenURL to Translation(
            cn = "打开地址",
            en = "Open URL"
        ),
        Pages.FunctionPage.OpenURLDescription to Translation(
            cn = "打开链接或文件",
            en = "Open link or file"
        ),
        Pages.FunctionPage.HideSite to Translation(
            cn = "隐藏网站",
            en = "Hide Site"
        ),
        Pages.FunctionPage.HideSiteDescription to Translation(
            cn = "管理需要隐藏的网站列表",
            en = "Manage hidden site list"
        ),
        Pages.FunctionPage.BlockSite to Translation(
            cn = "屏蔽网站",
            en = "Block Site"
        ),
        Pages.FunctionPage.BlockSiteDescription to Translation(
            cn = "批量屏蔽指定的网站",
            en = "Batch block specified sites"
        ),
        Pages.FunctionPage.GrabSite to Translation(
            cn = "抓取网站",
            en = "Grab Site"
        ),
        Pages.FunctionPage.GrabSiteDescription to Translation(
            cn = "从指定URL抓取网站信息",
            en = "Grab site info from specified URL"
        ),
        Pages.FunctionPage.StartPage to Translation(
            cn = "开始页面",
            en = "Start Page"
        ),
        Pages.FunctionPage.StartPageDescription to Translation(
            cn = "设置浏览器的默认起始页面",
            en = "Set default browser start page"
        ),
        Pages.FunctionPage.OtherFunctions to Translation(
            cn = "其他功能",
            en = "Other Functions"
        ),
        Pages.FunctionPage.OtherFunctionsDescription to Translation(
            cn = "更多实用功能设置",
            en = "More useful function settings"
        ),
        Pages.FunctionPage.UpdatePage to Translation(
            cn = "更新页面",
            en = "Update Page"
        ),
        Pages.FunctionPage.Enter to Translation(
            cn = "进入",
            en = "Enter"
        ),

        // Add Site Page
        Pages.AddSitePage.AddSiteRegex to Translation(
            cn = "添加网站/正则式",
            en = "Add Site/Regex"
        ),
        Pages.AddSitePage.Website to Translation(
            cn = "网站",
            en = "website"
        ),
        Pages.AddSitePage.ErrorBackendUnavailable to Translation(
            cn = "错误：无法访问后端服务",
            en = "Error: Unable to access backend service"
        ),
        Pages.AddSitePage.DisplayName to Translation(
            cn = "显示名称",
            en = "Display Name"
        ),
        Pages.AddSitePage.DisplayNameExists to Translation(
            cn = "该显示名称已存在，请使用其他名称",
            en = "This display name already exists, please use another name"
        ),
        Pages.AddSitePage.CheckingName to Translation(
            cn = "检查名称中...",
            en = "Checking name..."
        ),
        Pages.AddSitePage.WebsiteURL to Translation(
            cn = "网站地址",
            en = "Website URL"
        ),
        Pages.AddSitePage.CheckingSiteRestrictions to Translation(
            cn = "检查网站限制中...",
            en = "Checking site restrictions..."
        ),
        Pages.AddSitePage.SiteAddedSuccess to Translation(
            cn = "网站添加成功",
            en = "Site added successfully"
        ),
        Pages.AddSitePage.SiteAddFailed to Translation(
            cn = "添加网站失败",
            en = "Failed to add site"
        ),
        Pages.AddSitePage.SiteAddFailedRetry to Translation(
            cn = "添加网站失败，请重试",
            en = "Failed to add site, please try again"
        ),
        Pages.AddSitePage.Checking to Translation(
            cn = "检查中...",
            en = "Checking..."
        ),
        Pages.AddSitePage.AddSite to Translation(
            cn = "添加网站",
            en = "Add Site"
        ),
        Pages.AddSitePage.AddRegex to Translation(
            cn = "添加正则式",
            en = "Add Regex"
        ),
        Pages.AddSitePage.RegularExpression to Translation(
            cn = "正则表达式",
            en = "Regular Expression"
        ),
        Pages.AddSitePage.RegexSyntaxError to Translation(
            cn = "正则表达式语法错误",
            en = "Regex syntax error"
        ),
        Pages.AddSitePage.ClickToCollapseHelp to Translation(
            cn = "点击收起帮助",
            en = "Click to collapse help"
        ),
        Pages.AddSitePage.ViewRegexExamples to Translation(
            cn = "查看正则式示例",
            en = "View regex examples"
        ),
        Pages.AddSitePage.RegexExampleTitle to Translation(
            cn = "正则表达式示例：",
            en = "Regular Expression Examples:"
        ),
        Pages.AddSitePage.MatchSpecificDomain to Translation(
            cn = "匹配特定域名",
            en = "Match specific domain"
        ),
        Pages.AddSitePage.MatchSpecificDomainDesc to Translation(
            cn = "匹配 example.com 域名下的所有URL",
            en = "Match all URLs under example.com domain"
        ),
        Pages.AddSitePage.MatchAllSubdomains to Translation(
            cn = "匹配所有子域名",
            en = "Match all subdomains"
        ),
        Pages.AddSitePage.MatchAllSubdomainsDesc to Translation(
            cn = "匹配 *.github.com 的所有子域名",
            en = "Match all subdomains of *.github.com"
        ),
        Pages.AddSitePage.MatchLocalNetwork to Translation(
            cn = "匹配本地网络",
            en = "Match local network"
        ),
        Pages.AddSitePage.MatchLocalNetworkDesc to Translation(
            cn = "匹配 192.168.x.x 网段的所有地址",
            en = "Match all addresses in 192.168.x.x segment"
        ),
        Pages.AddSitePage.MatchHTTPS to Translation(
            cn = "匹配HTTPS协议",
            en = "Match HTTPS protocol"
        ),
        Pages.AddSitePage.MatchHTTPSDesc to Translation(
            cn = "只匹配HTTPS协议的网址",
            en = "Only match HTTPS protocol URLs"
        ),
        Pages.AddSitePage.MatchSpecificPort to Translation(
            cn = "匹配特定端口",
            en = "Match specific port"
        ),
        Pages.AddSitePage.MatchSpecificPortDesc to Translation(
            cn = "匹配8080端口的所有网址",
            en = "Match all URLs with port 8080"
        ),
        Pages.AddSitePage.MatchFileTypes to Translation(
            cn = "匹配文件类型",
            en = "Match file types"
        ),
        Pages.AddSitePage.MatchFileTypesDesc to Translation(
            cn = "匹配特定文件扩展名",
            en = "Match specific file extensions"
        ),
        Pages.AddSitePage.RegexNote to Translation(
            cn = "注意：正则式大小写不敏感，添加前请仔细测试！",
            en = "Note: Regex is case-insensitive, please test carefully before adding!"
        ),
        Pages.AddSitePage.Manage to Translation(
            cn = "管理",
            en = "Manage"
        ),
        Pages.AddSitePage.ManageAddedItems to Translation(
            cn = "管理已添加项目",
            en = "Manage Added Items"
        ),
        Pages.AddSitePage.Collapse to Translation(
            cn = "收起",
            en = "Collapse"
        ),
        Pages.AddSitePage.Expand to Translation(
            cn = "展开",
            en = "Expand"
        ),
        Pages.AddSitePage.CustomRegex to Translation(
            cn = "自定义正则式",
            en = "Custom Regex"
        ),
        Pages.AddSitePage.DeleteSelected to Translation(
            cn = "删除选中项",
            en = "Delete Selected"
        ),
        Pages.AddSitePage.DeleteRegex to Translation(
            cn = "删除正则式",
            en = "Delete Regex"
        ),
        Pages.AddSitePage.ConfirmDeleteSelected to Translation(
            cn = "确定要删除选中的",
            en = "Are you sure you want to delete the selected"
        ),
        Pages.AddSitePage.RegexPatternsUndone to Translation(
            cn = "个正则式吗？此操作不可撤销。",
            en = "regex patterns? This action cannot be undone."
        ),
        Pages.AddSitePage.SelectedRegexDeleted to Translation(
            cn = "已删除选中的正则式",
            en = "Selected regex patterns deleted"
        ),
        Pages.AddSitePage.ConfirmDeleteRegex to Translation(
            cn = "确定要删除正则式",
            en = "Are you sure you want to delete regex"
        ),
        Pages.AddSitePage.Undone to Translation(
            cn = "吗？此操作不可撤销。",
            en = "? This action cannot be undone."
        ),
        Pages.AddSitePage.RegexDeleted to Translation(
            cn = "已删除正则式",
            en = "Regex pattern deleted"
        ),
        Pages.AddSitePage.Regex to Translation(
            cn = "正则式",
            en = "RegExp"
        ),
        Pages.AddSitePage.AddedSites to Translation(
            cn = "已添加网站",
            en = "Added Sites"
        ),
        Pages.AddSitePage.DeleteSite to Translation(
            cn = "删除网站",
            en = "Delete Site"
        ),
        Pages.AddSitePage.Sites to Translation(
            cn = "个网站吗？",
            en = "sites?"
        ),
        Pages.AddSitePage.DeleteSiteWarning to Translation(
            cn = "警告：删除后可能会有时间限制无法再添加这些网站！",
            en = "Warning: After deletion, there may be time restrictions preventing you from adding these sites again!"
        ),
        Pages.AddSitePage.SelectedSitesDeleted to Translation(
            cn = "已删除选中的网站",
            en = "Selected sites deleted"
        ),
        Pages.AddSitePage.ConfirmDeleteSite to Translation(
            cn = "确定要删除网站",
            en = "Are you sure you want to delete site"
        ),
        Pages.AddSitePage.DeleteSiteSingleWarning to Translation(
            cn = "警告：删除后可能会有时间限制无法再添加此网站！",
            en = "Warning: After deletion, there may be time restrictions preventing you from adding this site again!"
        ),
        Pages.AddSitePage.SiteDeleted to Translation(
            cn = "已删除网站",
            en = "Site deleted"
        ),
        Pages.AddSitePage.Calculator to Translation(
            cn = "科学计算器",
            en = "ScientificCalculator"
        ),
        Pages.AddSitePage.NoItemsToManage to Translation(
            cn = "暂无项目可管理",
            en = "No items to manage"
        ),
        Pages.AddSitePage.CopiedToClipboard to Translation(
            cn = "已复制到剪切板",
            en = "Copied to clipboard"
        ),
        Pages.AddSitePage.Copy to Translation(
            cn = "复制",
            en = "Copy"
        ),
        Pages.AddSitePage.Delete to Translation(
            cn = "删除",
            en = "Delete"
        ),
        Pages.AddSitePage.Status to Translation(
            cn = "状态",
            en = "Status"
        ),
        Pages.AddSitePage.Cancel to Translation(
            cn = "取消",
            en = "Cancel"
        ),
        Pages.AddSitePage.ConfirmDelete to Translation(
            cn = "确认删除",
            en = "Confirm Delete"
        ),
        Pages.AddSitePage.RegexHelp to Translation(
            cn = "正则式帮助",
            en = "Regex Help"
        ),
        Pages.AddSitePage.RegexAddedSuccess to Translation(
            cn = "正则式添加成功",
            en = "Regex added successfully"
        ),
        Pages.AddSitePage.RegexAddFailed to Translation(
            cn = "正则式添加失败",
            en = "Failed to add regex"
        ),
        Pages.AddSitePage.RegexAddFailedRetry to Translation(
            cn = "正则式添加失败，请重试",
            en = "Failed to add regex, please try again"
        ),

        // Open URL Page
        Pages.OpenURLPage.EnterValidURL to Translation(
            cn = "请输入有效的网址或文件路径",
            en = "Please enter a valid URL or file path"
        ),
        Pages.OpenURLPage.NotAllowedToOpen to Translation(
            cn = "未允许打开",
            en = "Not allowed to open"
        ),
        Pages.OpenURLPage.EnterURLOrPath to Translation(
            cn = "输入网址或文件路径",
            en = "Enter URL or file path"
        ),
        Pages.OpenURLPage.Open to Translation(
            cn = "打开",
            en = "Open"
        ),
        Pages.OpenURLPage.CheckingSitePermissions to Translation(
            cn = "检查网站权限中...",
            en = "Checking site permissions..."
        ),
        Pages.OpenURLPage.LocalFileDetected to Translation(
            cn = "检测到本地文件，已验证可打开",
            en = "Local file detected, verified and ready to open"
        ),
        Pages.OpenURLPage.SupportsURLsAndFiles to Translation(
            cn = "支持网址、域名或本地文件路径（PDF、图片等）",
            en = "Supports URLs, domains, or local file paths (PDF, images, etc.)"
        ),
        Pages.OpenURLPage.SiteAccessRestricted to Translation(
            cn = "网站访问受限",
            en = "Site access restricted"
        ),
        Pages.OpenURLPage.SiteNotAllowed to Translation(
            cn = "该网站未被允许访问",
            en = "This site is not allowed to access"
        ),
        Pages.OpenURLPage.CanOpen to Translation(
            cn = "可以打开",
            en = "Can open"
        ),
        Pages.OpenURLPage.LocalFileVerified to Translation(
            cn = "本地文件已验证",
            en = "Local file verified"
        ),
        Pages.OpenURLPage.Accessible to Translation(
            cn = "可正常访问",
            en = "Accessible"
        ),
        Pages.OpenURLPage.SupportedFileFormats to Translation(
            cn = "支持的文件格式",
            en = "Supported file formats"
        ),
        Pages.OpenURLPage.SupportedFileFormatsDesc to Translation(
            cn = "• PDF 文档\n• 图片 (JPG, PNG, GIF, SVG 等)\n• 网页 (HTML)\n• 文本 (TXT, JSON, CSV)\n• 视频/音频 (MP4, MP3 等)",
            en = "• PDF documents\n• Images (JPG, PNG, GIF, SVG, etc.)\n• Web pages (HTML)\n• Text files (TXT, JSON, CSV)\n• Video/Audio (MP4, MP3, etc.)"
        ),

        // Hide Site Page
        Pages.HideSitePage.Info to Translation(
            cn = "信息",
            en = "Info"
        ),
        Pages.HideSitePage.Total to Translation(
            cn = "共",
            en = "Total"
        ),
        Pages.HideSitePage.SitesHidden to Translation(
            cn = "个网站，已隐藏",
            en = "sites, hidden"
        ),
        Pages.HideSitePage.Count to Translation(
            cn = "个",
            en = ""
        ),
        Pages.HideSitePage.Show to Translation(
            cn = "显示",
            en = "Show"
        ),
        Pages.HideSitePage.Hide to Translation(
            cn = "隐藏",
            en = "Hide"
        ),

        // Block Site Page
        Pages.BlockSitePage.BlockDomainOrURL to Translation(
            cn = "屏蔽域名或网址",
            en = "Block Domain or URL"
        ),
        Pages.BlockSitePage.Block to Translation(
            cn = "屏蔽",
            en = "Block"
        ),
        Pages.BlockSitePage.Item to Translation(
            cn = "第",
            en = "Item"
        ),
        Pages.BlockSitePage.ItemCount to Translation(
            cn = "项",
            en = ""
        ),
        Pages.BlockSitePage.InvalidFormat to Translation(
            cn = "格式不正确",
            en = "Invalid format"
        ),
        Pages.BlockSitePage.Details to Translation(
            cn = "详细说明",
            en = "Details"
        ),
        Pages.BlockSitePage.BlockingRules to Translation(
            cn = "屏蔽规则说明：",
            en = "Blocking Rules:"
        ),
        Pages.BlockSitePage.SupportsDomainsAndURLs to Translation(
            cn = "支持域名和完整网址，多个请用英文逗号分隔",
            en = "Supports domains and full URLs, separate multiple entries with commas"
        ),
        Pages.BlockSitePage.DomainBlockingDesc to Translation(
            cn = "🔹 域名屏蔽：如 baidu.com\n   该域名下所有网址都无法访问",
            en = "🔹 Domain blocking: e.g., baidu.com\n   All URLs under this domain will be inaccessible"
        ),
        Pages.BlockSitePage.URLBlockingDesc to Translation(
            cn = "🔹 网址屏蔽：如 https://www.google.com/search\n   该网址及其子路径无法访问",
            en = "🔹 URL blocking: e.g., https://www.google.com/search\n   This URL and its sub-paths will be inaccessible"
        ),
        Pages.BlockSitePage.SitesToBeBlocked to Translation(
            cn = "⚠️ 将要屏蔽的网站",
            en = "⚠️ Sites to be blocked"
        ),
        Pages.BlockSitePage.ConfirmBlock to Translation(
            cn = "确认屏蔽",
            en = "Confirm Block"
        ),
        Pages.BlockSitePage.Warning to Translation(
            cn = "警告",
            en = "Warning"
        ),
        Pages.BlockSitePage.BlockWarning to Translation(
            cn = "⚠️ 警告：一旦屏蔽，将永久无法取消！",
            en = "⚠️ Warning: Once blocked, it cannot be undone permanently!"
        ),
        Pages.BlockSitePage.ConfirmBlockQuestion to Translation(
            cn = "确定要屏蔽以下域名/网址吗？",
            en = "Are you sure you want to block the following domains/URLs?"
        ),
        Pages.BlockSitePage.Success to Translation(
            cn = "成功",
            en = "Success"
        ),
        Pages.BlockSitePage.BlockedSuccessfully to Translation(
            cn = "屏蔽成功",
            en = "Blocked successfully"
        ),
        Pages.BlockSitePage.DomainURLBlocked to Translation(
            cn = "域名/网址已成功屏蔽",
            en = "Domain/URL has been blocked successfully"
        ),
        Pages.BlockSitePage.OK to Translation(
            cn = "确定",
            en = "OK"
        ),
        Pages.BlockSitePage.Failed to Translation(
            cn = "失败",
            en = "Failed"
        ),
        Pages.BlockSitePage.BlockFailed to Translation(
            cn = "屏蔽失败",
            en = "Block failed"
        ),
        Pages.BlockSitePage.BlockFailedRetry to Translation(
            cn = "域名/网址屏蔽失败，请重试",
            en = "Failed to block domain/URL, please try again"
        ),
        Pages.BlockSitePage.WillBeBlocked to Translation(
            cn = "将被屏蔽",
            en = "Will be blocked"
        ),

        // Grab Site Page
        Pages.GrabSitePage.GrabSite to Translation(
            cn = "抓取网站",
            en = "Grab Site"
        ),
        Pages.GrabSitePage.Grab to Translation(
            cn = "抓取",
            en = "Grab"
        ),
        Pages.GrabSitePage.EnterValidURLOrDomain to Translation(
            cn = "请输入有效的网址或域名格式",
            en = "Please enter a valid URL or domain format"
        ),
        Pages.GrabSitePage.EnterURLToGrab to Translation(
            cn = "💡 输入完整的网址或域名开始抓取",
            en = "💡 Enter a complete URL or domain to start grabbing"
        ),
        Pages.GrabSitePage.Download to Translation(
            cn = "下载",
            en = "Download"
        ),
        Pages.GrabSitePage.Downloadable to Translation(
            cn = "可下载",
            en = "Downloadable"
        ),
        Pages.GrabSitePage.OpenDownloadDirectory to Translation(
            cn = "打开下载目录",
            en = "Open download directory"
        ),
        Pages.GrabSitePage.ClickDownloadButton to Translation(
            cn = "💡 点击下载按钮开始下载文件",
            en = "💡 Click download button to start downloading file"
        ),
        Pages.GrabSitePage.EnterFileURL to Translation(
            cn = "💡 输入完整的文件地址开始下载",
            en = "💡 Enter complete file URL to start downloading"
        ),
        Pages.GrabSitePage.DownloadProgress to Translation(
            cn = "下载进度",
            en = "Download progress"
        ),
        Pages.GrabSitePage.DownloadFailed to Translation(
            cn = "❌ 下载失败:",
            en = "❌ Download failed:"
        ),
        Pages.GrabSitePage.GrabbingInfo to Translation(
            cn = "正在抓取网站信息...",
            en = "Grabbing website info..."
        ),
        Pages.GrabSitePage.GrabSuccessFound to Translation(
            cn = "抓取成功！共找到",
            en = "Grab successful! Found"
        ),
        Pages.GrabSitePage.Copied to Translation(
            cn = "已复制",
            en = "Copied"
        ),
        Pages.GrabSitePage.ItemsToClipboard to Translation(
            cn = "个项目到剪贴板",
            en = "items to clipboard"
        ),
        Pages.GrabSitePage.CopyAll to Translation(
            cn = "复制全部",
            en = "Copy all"
        ),
        Pages.GrabSitePage.Links to Translation(
            cn = "个链接",
            en = "links"
        ),
        Pages.GrabSitePage.URLsSelected to Translation(
            cn = "个URL已选中",
            en = "URLs selected"
        ),
        Pages.GrabSitePage.CopiedDomain to Translation(
            cn = "已复制域名:",
            en = "Copied domain:"
        ),
        Pages.GrabSitePage.CopyDomain to Translation(
            cn = "复制域名",
            en = "Copy domain"
        ),
        Pages.GrabSitePage.CopiedURL to Translation(
            cn = "已复制URL:",
            en = "Copied URL:"
        ),
        Pages.GrabSitePage.CopyURL to Translation(
            cn = "复制URL",
            en = "Copy URL"
        ),
        Pages.GrabSitePage.DownloadComplete to Translation(
            cn = "下载完成",
            en = "Download complete"
        ),

        // Start Page Settings
        Pages.StartPageSettings.StartPageSettings to Translation(
            cn = "开始页面设置",
            en = "Start Page Settings"
        ),
        Pages.StartPageSettings.CurrentStartPage to Translation(
            cn = "当前开始页面",
            en = "Current Start Page"
        ),
        Pages.StartPageSettings.DefaultPage to Translation(
            cn = "默认页面 (系统内置HTML页面)",
            en = "Default Page (Built-in HTML page)"
        ),
        Pages.StartPageSettings.RestoreDefaultPage to Translation(
            cn = "恢复默认页面",
            en = "Restore Default Page"
        ),
        Pages.StartPageSettings.Selected to Translation(
            cn = "已选中",
            en = "Selected"
        ),
        Pages.StartPageSettings.Hidden to Translation(
            cn = "(已隐藏)",
            en = "(Hidden)"
        ),
        Pages.StartPageSettings.VerificationFailed to Translation(
            cn = "(验证失败)",
            en = "(Verification Failed)"
        ),
        Pages.StartPageSettings.Confirm to Translation(
            cn = "确认",
            en = "Confirm"
        ),
        Pages.StartPageSettings.SetStartPage to Translation(
            cn = "设置开始页面",
            en = "Set Start Page"
        ),
        Pages.StartPageSettings.ConfirmSetQuestion to Translation(
            cn = "确定要将",
            en = "Are you sure you want to set"
        ),
        Pages.StartPageSettings.AsStartPage to Translation(
            cn = "设置为开始页面吗？",
            en = "as the start page?"
        ),
        Pages.StartPageSettings.ConfirmRestoreDefault to Translation(
            cn = "确定要恢复到默认开始页面吗？",
            en = "Are you sure you want to restore to the default start page?"
        ),

        // Other Functions Page
        Pages.SchedulePage.Copy to Translation(
            cn = "复制",
            en = "Copy"
        ),
        Pages.SchedulePage.Monday to Translation(
            cn = "周一",
            en = "Mon"
        ),
        Pages.SchedulePage.Tuesday to Translation(
            cn = "周二",
            en = "Tue"
        ),
        Pages.SchedulePage.Wednesday to Translation(
            cn = "周三",
            en = "Wed"
        ),
        Pages.SchedulePage.Thursday to Translation(
            cn = "周四",
            en = "Thu"
        ),
        Pages.SchedulePage.Friday to Translation(
            cn = "周五",
            en = "Fri"
        ),
        Pages.SchedulePage.Saturday to Translation(
            cn = "周六",
            en = "Sat"
        ),
        Pages.SchedulePage.Sunday to Translation(
            cn = "周日",
            en = "Sun"
        ),
        Pages.SchedulePage.NoSchedules to Translation(
            cn = "暂无日程",
            en = "No schedules"
        ),
        Pages.SchedulePage.NoteLabel to Translation(
            cn = "备注",
            en = "Note"
        ),
        Pages.SchedulePage.Edit to Translation(
            cn = "编辑",
            en = "Edit"
        ),
        Pages.SchedulePage.MoveUp to Translation(
            cn = "上移",
            en = "Move up"
        ),
        Pages.SchedulePage.MoveDown to Translation(
            cn = "下移",
            en = "Move down"
        ),
        Pages.SchedulePage.Delete to Translation(
            cn = "删除",
            en = "Delete"
        ),
        Pages.SchedulePage.NoteOptional to Translation(
            cn = "备注（可选）",
            en = "Note (optional)"
        ),
        Pages.SchedulePage.SetStartTime to Translation(
            cn = "设置开始时间",
            en = "Set start time"
        ),
        Pages.SchedulePage.SetEndTime to Translation(
            cn = "设置结束时间",
            en = "Set end time"
        ),
        Pages.SchedulePage.ScheduleType to Translation(
            cn = "日程类型",
            en = "Schedule Type"
        ),
        Pages.SchedulePage.Normal to Translation(
            cn = "普通",
            en = "Normal"
        ),
        Pages.SchedulePage.Cyclic to Translation(
            cn = "循环",
            en = "Cyclic"
        ),
        Pages.SchedulePage.Sequence to Translation(
            cn = "周期",
            en = "Sequence"
        ),
        Pages.SchedulePage.Once to Translation(
            cn = "一次",
            en = "Once"
        ),
        Pages.SchedulePage.Daily to Translation(
            cn = "每天",
            en = "Daily"
        ),
        Pages.SchedulePage.SpecificDays to Translation(
            cn = "特定日",
            en = "Specific Days"
        ),
        Pages.SchedulePage.SelectWeekdays to Translation(
            cn = "选择星期",
            en = "Select Weekdays"
        ),
        Pages.SchedulePage.Mon to Translation(
            cn = "一",
            en = "Mon"
        ),
        Pages.SchedulePage.Tue to Translation(
            cn = "二",
            en = "Tue"
        ),
        Pages.SchedulePage.Wed to Translation(
            cn = "三",
            en = "Wed"
        ),
        Pages.SchedulePage.Thu to Translation(
            cn = "四",
            en = "Thu"
        ),
        Pages.SchedulePage.Fri to Translation(
            cn = "五",
            en = "Fri"
        ),
        Pages.SchedulePage.Sat to Translation(
            cn = "六",
            en = "Sat"
        ),
        Pages.SchedulePage.Sun to Translation(
            cn = "日",
            en = "Sun"
        ),
        Pages.SchedulePage.CyclicTaskList to Translation(
            cn = "循环任务列表",
            en = "Cyclic Task List"
        ),
        Pages.SchedulePage.TotalDuration to Translation(
            cn = "总时长",
            en = "Total Duration"
        ),
        Pages.SchedulePage.Minutes to Translation(
            cn = "分钟",
            en = "minutes"
        ),
        Pages.SchedulePage.ErrorTaskDurationExceeds to Translation(
            cn = "错误：任务总时长超过日程时长",
            en = "Error: Task duration exceeds schedule duration"
        ),
        Pages.SchedulePage.NoTasksAddBelow to Translation(
            cn = "暂无任务，点击下方添加按钮添加",
            en = "No tasks, click button below to add"
        ),
        Pages.SchedulePage.AddTask to Translation(
            cn = "添加任务",
            en = "Add Task"
        ),
        Pages.SchedulePage.TimeSettings to Translation(
            cn = "时间设置",
            en = "Time Settings"
        ),
        Pages.SchedulePage.Start to Translation(
            cn = "开始",
            en = "Start"
        ),
        Pages.SchedulePage.End to Translation(
            cn = "结束",
            en = "End"
        ),
        Pages.SchedulePage.AddCyclicTask to Translation(
            cn = "添加循环任务",
            en = "Add Cyclic Task"
        ),
        Pages.SchedulePage.TaskName to Translation(
            cn = "任务名称*",
            en = "Task Name*"
        ),
        Pages.SchedulePage.DurationMinutes to Translation(
            cn = "持续时间(分钟)*",
            en = "Duration (minutes)*"
        ),
        Pages.SchedulePage.SelectCopyStartTime to Translation(
            cn = "选择复制开始时间",
            en = "Select Copy Start Time"
        ),
        Pages.SchedulePage.SelectCopyTimeDescription to Translation(
            cn = "选择新的开始时间，将保持原有时间间隔",
            en = "Select new start time, keeping original intervals"
        ),
        Pages.SchedulePage.SelectTime to Translation(
            cn = "选择时间",
            en = "Select Time"
        ),
        Pages.SchedulePage.MonthDay to Translation(
            cn = "月{month}日",  // 可以用占位符处理
            en = "{month}/{day}"
        ),
        Pages.OtherFunctionsPage.TurnOffVideo to Translation(
            cn = "关闭视频",
            en = "Turn Off Video"
        ),
        Pages.OtherFunctionsPage.TemporarilyEnableVideo to Translation(
            cn = "临时打开视频",
            en = "Temporarily Enable Video"
        ),
        Pages.OtherFunctionsPage.Calculator to Translation(
            cn = "计算器",
            en = "Calculator"
        ),
        Pages.OtherFunctionsPage.VideoPlaybackFunction to Translation(
            cn = "视频播放功能",
            en = "Video Playback Function"
        ),
        Pages.OtherFunctionsPage.Video to Translation(
            cn = "视频",
            en = "Video"
        ),
        Pages.OtherFunctionsPage.TurnOffVideoQuestion to Translation(
            cn = "要关闭视频播放功能吗",
            en = "Do you want to turn off the video playback function?"
        ),
        Pages.OtherFunctionsPage.EnableVideoQuestion to Translation(
            cn = "要开启视频播放功能吗",
            en = "Do you want to enable the video playback function?"
        ),
        Pages.OtherFunctionsPage.RemainingTimeToday to Translation(
            cn = "今日剩余时间：",
            en = "Remaining time today: "
        ),
        Pages.OtherFunctionsPage.MustStop to Translation(
            cn = "必须停止了!!!",
            en = "Must stop!!!"
        ),
        Pages.OtherFunctionsPage.TurnOff to Translation(
            cn = "关闭",
            en = "Turn Off"
        ),
        Pages.OtherFunctionsPage.Enable to Translation(
            cn = "开启",
            en = "Enable"
        ),

        // Schedule Page
        Pages.SchedulePage.ScheduleManagement to Translation(
            cn = "日程管理",
            en = "Schedule Management"
        ),
        Pages.SchedulePage.AddSchedule to Translation(
            cn = "添加日程",
            en = "Add Schedule"
        ),
        Pages.SchedulePage.EditSchedule to Translation(
            cn = "编辑日程",
            en = "Edit Schedule"
        ),
        Pages.SchedulePage.DeleteSchedule to Translation(
            cn = "删除日程",
            en = "Delete Schedule"
        ),
        Pages.SchedulePage.ScheduleName to Translation(
            cn = "日程名称",
            en = "Schedule Name"
        ),
        Pages.SchedulePage.StartTime to Translation(
            cn = "开始时间",
            en = "Start Time"
        ),
        Pages.SchedulePage.EndTime to Translation(
            cn = "结束时间",
            en = "End Time"
        ),
        Pages.SchedulePage.RepeatMode to Translation(
            cn = "重复模式",
            en = "Repeat Mode"
        ),
        Pages.SchedulePage.Note to Translation(
            cn = "备注",
            en = "Note"
        ),
        Pages.SchedulePage.Save to Translation(
            cn = "保存",
            en = "Save"
        ),
        Pages.SchedulePage.Cancel to Translation(
            cn = "取消",
            en = "Cancel"
        ),
        Pages.SchedulePage.PleaseSelectStartTime to Translation(
            cn = "请选择开始时间",
            en = "Please select start time"
        ),
        Pages.SchedulePage.PleaseSelectEndTime to Translation(
            cn = "请选择结束时间",
            en = "Please select end time"
        ),
        Pages.SchedulePage.SetAsStartTime to Translation(
            cn = "设置为开始时间",
            en = "Set as Start Time"
        ),
        Pages.SchedulePage.SetAsEndTime to Translation(
            cn = "设置为结束时间",
            en = "Set as End Time"
        ),
        Pages.SchedulePage.InvalidTime to Translation(
            cn = "时间无效",
            en = "Invalid Time"
        ),
        Pages.SchedulePage.PleaseConfirmTime to Translation(
            cn = "请确认时间设置",
            en = "Please confirm the time settings"
        ),
        Pages.SchedulePage.RepeatModeSettings to Translation(
            cn = "重复模式设置",
            en = "Repeat Mode Settings"
        ),
        Pages.SchedulePage.SetTaskToCyclic to Translation(
            cn = "设置任务为循环",
            en = "Set task to Cyclic"
        ),
        Pages.SchedulePage.CyclicTask to Translation(
            cn = "循环任务",
            en = "Cyclic Task"
        ),
        Pages.SchedulePage.PleaseCheckTime to Translation(
            cn = "请检查时间设置",
            en = "Please check the time settings"
        ),
        Pages.SchedulePage.ConfirmDelete to Translation(
            cn = "删除确认",
            en = "Confirm Delete"
        ),
        Pages.SchedulePage.ConfirmDeleteQuestion to Translation(
            cn = "确认删除选中的日程吗？",
            en = "Are you sure you want to delete the selected schedule?"
        ),
        Pages.SchedulePage.ScheduleDeleted to Translation(
            cn = "日程已删除",
            en = "Schedule Deleted"
        ),
        Pages.SchedulePage.ScheduleAdded to Translation(
            cn = "日程已添加",
            en = "Schedule Added"
        ),
        Pages.SchedulePage.ScheduleEdited to Translation(
            cn = "日程编辑成功",
            en = "Schedule Edited Successfully"
        ),
        Pages.SchedulePage.PleaseEnterName to Translation(
            cn = "请输入日程名称",
            en = "Please enter schedule name"
        ),
        Pages.SchedulePage.PleaseEnterValidRange to Translation(
            cn = "请输入有效的时间范围",
            en = "Please enter a valid time range"
        ),
        Pages.SchedulePage.PleaseSelectDate to Translation(
            cn = "请选择日期",
            en = "Please select date"
        ),
        Pages.SchedulePage.StartTimeCannotBeLater to Translation(
            cn = "日程开始时间不能晚于结束时间",
            en = "Start time cannot be later than end time"
        ),
        Pages.SchedulePage.AcceptSuggestion to Translation(
            cn = "按 → 采用建议",
            en = "Press → to accept the suggestion"
        )
    )

    fun getString(key: Enum<*>): String {
        val translation = translations[key] ?: return key.name

        return when (OuterConfig.langType) {
            LangType.CN -> translation.cn
            LangType.EN -> translation.en
            else -> translation.cn
        }
    }
}

fun Enum<*>.getLang(): String = LangBackend.getString(this)
