package com.example.utils

data class FeatureExplanation(
    val title: String,
    val whatItIs: String,
    val problemSolved: String,
    val howToUse: String,
    val whatHappens: String
)

enum class FeatureHelpTopic {
    HOME_DASHBOARD,
    SMART_PROFILES,
    PHONE_STATUS,
    ROUTINES,
    RULE_BUILDER,
    ASSISTANT,
    CONTACTS,
    INSIGHTS,
    MORE_HUB,
    FOCUS_SESSIONS,
    ACTIVITY_HISTORY,
    NOTIFICATIONS,
    CONTEXT_AWARENESS,
    SAFETY_CENTER,
    SIMULATION_ENGINE
}

object FeatureExplanations {
    fun get(topic: FeatureHelpTopic, isArabic: Boolean): FeatureExplanation = if (isArabic) {
        when (topic) {
            FeatureHelpTopic.HOME_DASHBOARD -> FeatureExplanation(
                title = "الشاشة الرئيسية",
                whatItIs = "لوحة تحكم هادئة تعرض وضعك الحالي وحالة الهاتف المحاكاة والأتمتة في مكان واحد.",
                problemSolved = "تمنع التشتت وتجمع أهم ما تحتاجه دون إغراقك بتفاصيل معقدة.",
                howToUse = "بدّل الوضع أو الصوت المحاكى أو استخدم الإجراءات السريعة الثلاثة بلمسة واحدة.",
                whatHappens = "يتحدث الوضع الحالي وحالة المحاكاة فوراً ويُسجّل التغيير في سجل النشاط."
            )
            FeatureHelpTopic.SMART_PROFILES -> FeatureExplanation(
                title = "الأوضاع الذكية",
                whatItIs = "مجموعات إعدادات جاهزة (مثل الدراسة والعمل والنوم) تحدد الصوت والتنبيهات المسموحة.",
                problemSolved = "تغنيك عن تعديل الصوت واستثناءات الإشعارات يدوياً في كل مرة.",
                howToUse = "اختر أي وضع لتفعيله فوراً أو أنشئ وضعاً مخصصاً يناسب يومك.",
                whatHappens = "يُطبّق الوضع الصوتي المحاكى وتُحدَّث قواعد السماح لجهات الأولوية."
            )
            FeatureHelpTopic.PHONE_STATUS -> FeatureExplanation(
                title = "حالة الهاتف المحاكاة",
                whatItIs = "مؤشر تجريبي يعرض وضع الصوت الحالي داخل العرض (صوت / اهتزاز / صامت).",
                problemSolved = "يتيح تجربة سلوك كتم الإشعارات بأمان دون تغيير صوت هاتفك الحقيقي.",
                howToUse = "اختر صامت أو اهتزاز أو صوت للمقارنة الفورية مع قواعد جهات الاتصال.",
                whatHappens = "تتغير قرارات السماح أو الحجب داخل المحاكاة فقط دون مساس بإعدادات النظام."
            )
            FeatureHelpTopic.ROUTINES -> FeatureExplanation(
                title = "الروتين الذكي",
                whatItIs = "قواعد تلقائية تربط بين وقت أو سياق معين وإجراءات محددة لهاتفك المحاكى.",
                problemSolved = "تضمن انتقال الهاتف للوضع المناسب تلقائياً دون أن تنسى تفعيله.",
                howToUse = "فعّل الروتين أو عطّله، ابحث في القواعد، أو اضغط إضافة روتين لبناء قاعدة جديدة.",
                whatHappens = "عند تحقق المشغّل والشروط تُنفَّذ الإجراءات مع احترام الاستثناءات والأولوية."
            )
            FeatureHelpTopic.RULE_BUILDER -> FeatureExplanation(
                title = "منشئ الروتين التفاعلي",
                whatItIs = "أداة خطوة بخطوة لبناء قاعدة ذكية عبر: متى ← الشروط ← الإجراءات ← الاستثناءات.",
                problemSolved = "تبسّط كتابة قواعد الأتمتة دون تعقيد برمجي.",
                howToUse = "اتبع الخطوات الخمس، اختبر القاعدة بالمحاكاة، ثم احفظها في قائمتك.",
                whatHappens = "يصبح الروتين جاهزاً للتقييم التلقائي واليدوي مع حماية من التعارض."
            )
            FeatureHelpTopic.ASSISTANT -> FeatureExplanation(
                title = "المساعد الذكي المحلي",
                whatItIs = "مساعد محادثة يعمل محلياً لفهم أوامرك بالعربية أو الإنجليزية واقتراح روتينات.",
                problemSolved = "يوفر وقت إعداد القواعد يدوياً ويشرح لك سبب تغيّر حالة الهاتف.",
                howToUse = "اختر أمراً مقترحاً أو اكتب طلبك، ثم راجع المعاينة وأكّدها قبل الحفظ.",
                whatHappens = "ينشئ المساعد مسودة روتين منظمة أو يبدأ جلسة تركيز أو يعرض نشاطك اليومي."
            )
            FeatureHelpTopic.CONTACTS -> FeatureExplanation(
                title = "جهات الأولوية",
                whatItIs = "تصنيف تجريبي لجهات الاتصال إلى طوارئ، أولوية عالية، وعادي.",
                problemSolved = "يمنع تفويت المكالمات العائلية أو العاجلة أثناء الوضع الصامت.",
                howToUse = "راجع التصنيفات، عدّل أولوية أي جهة، أو اختبر مكالمة/رسالة محاكاة.",
                whatHappens = "تتجاوز جهات الطوارئ الكتم دائماً، بينما تخضع الأولوية العالية لإعدادات الوضع."
            )
            FeatureHelpTopic.INSIGHTS -> FeatureExplanation(
                title = "التحليلات والمؤشرات",
                whatItIs = "ملخص مرئي لوقت التركيز وتنفيذ الروتينات والمقاطعات المحجوبة في العرض.",
                problemSolved = "يكشف لك أوقات الذروة المشتتة ومدى التزامك بجلسات التركيز.",
                howToUse = "تصفّح بطاقات المؤشرات والرسوم البيانية، أو افتح ذكاء الإشعارات للتفاصيل.",
                whatHappens = "تتحدّث الأرقام تلقائياً كلما أنهيت جلسة تركيز أو شغّلت روتيناً."
            )
            FeatureHelpTopic.MORE_HUB -> FeatureExplanation(
                title = "المزيد من الأدوات",
                whatItIs = "مركز منظم يضم الأدوات الإضافية: التركيز، السجل، الإشعارات، السياق، الأمان، والإعدادات.",
                problemSolved = "يحافظ على بساطة الشريط السفلي مع إبقاء كل وحدة متاحة بلمسة واحدة.",
                howToUse = "اختر أي وحدة حسب فئتها للانتقال إليها والعودة بسهولة.",
                whatHappens = "تفتح الوحدة المختارة بنفس تجربة التنقل المتناسقة."
            )
            FeatureHelpTopic.FOCUS_SESSIONS -> FeatureExplanation(
                title = "جلسات التركيز",
                whatItIs = "مؤقت تركيز مرتبط بالأوضاع الذكية يعرض الوقت المتبقي والمنقضي معاً.",
                problemSolved = "يساعدك على إكمال فترات مذاكرة أو عمل عميقة دون تشتيت.",
                howToUse = "اختر المدة والوضع واضغط بدء الجلسة، ويمكنك الإيقاف المؤقت أو محاكاة حجب إشعار.",
                whatHappens = "يُفعَّل الوضع الهادئ وتُسجَّل الجلسة المكتملة في إحصائياتك وتاريخ النشاط."
            )
            FeatureHelpTopic.ACTIVITY_HISTORY -> FeatureExplanation(
                title = "سجل النشاط",
                whatItIs = "خط زمني يميز بوضوح بين أحداث المحاكاة وتحديثات حالة التطبيق.",
                problemSolved = "يجيب عن سؤال: ماذا حدث ومتى ولماذا اتخذ التطبيق هذا القرار؟",
                howToUse = "ابحث أو صفِّ حسب الفئة، واضغط على أي حدث لقراءة شرح القرار الكامل.",
                whatHappens = "تُعرض التفاصيل الكاملة للمُشغّل والنتيجة دون أي غموض."
            )
            FeatureHelpTopic.NOTIFICATIONS -> FeatureExplanation(
                title = "ذكاء الإشعارات",
                whatItIs = "تحليل بيانات تجريبية يوضح مصادر التشتيت وأوقات ذروتها حسب التطبيق والفئة.",
                problemSolved = "يساعدك على معرفة التطبيقات التي تستهلك انتباهك أكثر من غيرها.",
                howToUse = "بدّل بين اليوم وآخر 7 أيام وآخر 30 يوماً لمقارنة الإحصائيات.",
                whatHappens = "يقدم لك توصيات عملية لتقليل الضوضاء في ساعات الذروة."
            )
            FeatureHelpTopic.CONTEXT_AWARENESS -> FeatureExplanation(
                title = "الوعي بالسياق المحاكى",
                whatItIs = "بيئة اختبار لضبط الوقت والموقع والنشاط المحاكى دون استخدام GPS الحقيقي.",
                problemSolved = "يختبر تشغيل الروتينات السياقية قبل الاعتماد عليها.",
                howToUse = "غيّر الساعة أو الموقع أو النشاط المحاكى ثم اضغط اختبار القرار.",
                whatHappens = "يقيّم المحرك القواعد المطابقة فوراً ويعرض النتيجة."
            )
            FeatureHelpTopic.SAFETY_CENTER -> FeatureExplanation(
                title = "مركز الأمان والاسترداد",
                whatItIs = "مجموعة ضوابط لحماية المستخدم: إيقاف شامل، تراجع عن آخر حالة، وفحص تعارضات.",
                problemSolved = "يضمن بقاءك دائماً المتحكم الأول مع إمكانية التراجع الفوري.",
                howToUse = "أوقف الأتمتة مؤقتاً بلمسة، استعد الحالة السابقة، أو اختبر وصول مكالمة الطوارئ.",
                whatHappens = "تتوقف المشغلات التلقائية فوراً مع الحفاظ على جميع قواعدك محفوظة."
            )
            FeatureHelpTopic.SIMULATION_ENGINE -> FeatureExplanation(
                title = "محرك المحاكاة",
                whatItIs = "مختبر تفاعلي يوضح خطوة بخطوة كيف يطابق التطبيق القواعد ويحل التعارضات.",
                problemSolved = "يجعل منطق الأتمتة شفافاً وقابلاً للتجربة قبل تطبيقه.",
                howToUse = "جرّب مشغّل وقت أو موقع أو مكالمة واردة وراقب سجل التقييم بالأسفل.",
                whatHappens = "يعرض المحرك الشروط والاستثناءات والتعارضات والنتيجة النهائية المحاكاة."
            )
        }
    } else {
        when (topic) {
            FeatureHelpTopic.HOME_DASHBOARD -> FeatureExplanation(
                title = "Home Dashboard",
                whatItIs = "A calm overview of your active profile, simulated phone mode, and automation state.",
                problemSolved = "Eliminates clutter by surfacing only the controls and metrics you need at a glance.",
                howToUse = "Switch profiles, adjust simulated sound, or tap one of the 3 quick actions.",
                whatHappens = "Updates your demo state immediately and logs the change in Activity History."
            )
            FeatureHelpTopic.SMART_PROFILES -> FeatureExplanation(
                title = "Smart Profiles",
                whatItIs = "Preset modes (Study, Work, Sleep, Personal) defining sound and interruption rules.",
                problemSolved = "Saves you from toggling sound and contact exceptions manually every time you switch tasks.",
                howToUse = "Tap Activate on any profile, edit its allowed categories, or create a custom profile.",
                whatHappens = "Applies the profile's simulated sound mode and updates priority contact filtering."
            )
            FeatureHelpTopic.PHONE_STATUS -> FeatureExplanation(
                title = "Simulated Phone Status",
                whatItIs = "An in-app demo indicator for Sound, Vibrate, or Silent mode.",
                problemSolved = "Lets you safely test quiet behaviors without altering your real Android audio settings.",
                howToUse = "Pick Sound, Vibrate, or Silent to immediately test how profiles and contacts respond.",
                whatHappens = "Only the app's simulated state changes; real device hardware remains untouched."
            )
            FeatureHelpTopic.ROUTINES -> FeatureExplanation(
                title = "Smart Routines",
                whatItIs = "Automated rules connecting a time or context trigger to simulated phone actions.",
                problemSolved = "Keeps your phone aligned with your daily schedule automatically.",
                howToUse = "Toggle routines on/off, search or filter by status, or tap Add Routine for guided setup.",
                whatHappens = "When a trigger matches and conditions pass, actions execute while honoring exceptions."
            )
            FeatureHelpTopic.RULE_BUILDER -> FeatureExplanation(
                title = "Guided Routine Builder",
                whatItIs = "A 5-step builder following WHEN → CONDITIONS → ACTIONS → EXCEPTIONS → REVIEW.",
                problemSolved = "Makes building reliable automation rules approachable without technical clutter.",
                howToUse = "Move through each step in plain language, test the rule, and save it.",
                whatHappens = "Adds the routine to your active engine with priority and conflict checks ready."
            )
            FeatureHelpTopic.ASSISTANT -> FeatureExplanation(
                title = "Smart Assistant",
                whatItIs = "An offline bilingual assistant that turns plain requests into structured routines or summaries.",
                problemSolved = "Speeds up routine setup and answers why your simulated phone mode changed.",
                howToUse = "Tap a suggestion or type a command, then confirm, edit, or cancel any proposed rule.",
                whatHappens = "Nothing is saved without your explicit confirmation."
            )
            FeatureHelpTopic.CONTACTS -> FeatureExplanation(
                title = "Priority Contacts",
                whatItIs = "Demo contact tiers (Emergency, High Priority, Normal) for interruption filtering.",
                problemSolved = "Prevents missing urgent calls from family or teammates while staying focused.",
                howToUse = "Review each priority tier, edit contact permissions, or expand the interruption tester.",
                whatHappens = "Emergency contacts bypass Silent mode; High Priority and Normal follow profile rules."
            )
            FeatureHelpTopic.INSIGHTS -> FeatureExplanation(
                title = "Insights",
                whatItIs = "Clear KPI cards and charts summarizing focus time, executed routines, and demo alerts.",
                problemSolved = "Reveals when distractions peak and how effectively your routines protect focus.",
                howToUse = "Review focus trends and interruption breakdowns, or open Notification Intelligence for app detail.",
                whatHappens = "KPIs update live as you complete focus sessions and run routines."
            )
            FeatureHelpTopic.MORE_HUB -> FeatureExplanation(
                title = "More Modules",
                whatItIs = "An organized directory of secondary tools grouped by Focus, Activity, Intelligence, Profiles, Protection, and Preferences.",
                problemSolved = "Keeps primary navigation clean with 5 tabs while keeping every capability one tap away.",
                howToUse = "Select any module card to open it, then use the top back arrow to return.",
                whatHappens = "Opens the selected module with full live app state."
            )
            FeatureHelpTopic.FOCUS_SESSIONS -> FeatureExplanation(
                title = "Focus Sessions",
                whatItIs = "A dedicated countdown timer synced with your Smart Profiles showing elapsed and remaining time.",
                problemSolved = "Helps you protect deep work blocks and track how many distractions were filtered.",
                howToUse = "Choose a duration and profile, tap Start Focus Session, and pause or end anytime.",
                whatHappens = "Activates your chosen focus profile and records completed minutes into Insights."
            )
            FeatureHelpTopic.ACTIVITY_HISTORY -> FeatureExplanation(
                title = "Activity History",
                whatItIs = "A searchable timeline separating Simulated actions from internal App State updates.",
                problemSolved = "Explains clearly why a mode switched or a contact was allowed or blocked.",
                howToUse = "Search or filter by category and tap any item to view the full decision trace.",
                whatHappens = "Shows the exact trigger source, outcome, and plain-language explanation."
            )
            FeatureHelpTopic.NOTIFICATIONS -> FeatureExplanation(
                title = "Notification Intelligence",
                whatItIs = "Synthetic demo analytics breaking down alert noise by app, category, and peak hours.",
                problemSolved = "Highlights which apps interrupt you most so you can target routines effectively.",
                howToUse = "Switch between Today, 7 Days, and 30 Days to inspect peaks and recommendations.",
                whatHappens = "Displays tailored focus suggestions based on synthetic demo telemetry."
            )
            FeatureHelpTopic.CONTEXT_AWARENESS -> FeatureExplanation(
                title = "Context Awareness",
                whatItIs = "A simulated environment panel for time, location, and activity without real GPS tracking.",
                problemSolved = "Lets you verify context-based triggers safely indoors or in demos.",
                howToUse = "Pick a simulated time, location, or activity and tap Test Context Decision.",
                whatHappens = "Shows matching routines and evaluates the simulated automation engine immediately."
            )
            FeatureHelpTopic.SAFETY_CENTER -> FeatureExplanation(
                title = "Safety Center",
                whatItIs = "Fail-safe controls including Global Pause, one-tap state rollback, conflict checks, and demo reset.",
                problemSolved = "Ensures you always stay in control and can undo unexpected automation switches.",
                howToUse = "Pause all automation anytime, restore your previous state, or test emergency bypass.",
                whatHappens = "Suspends automatic triggers immediately without deleting any saved routines."
            )
            FeatureHelpTopic.SIMULATION_ENGINE -> FeatureExplanation(
                title = "Simulation Engine",
                whatItIs = "A transparent sandbox showing step-by-step rule matching, conditions, exceptions, and conflicts.",
                problemSolved = "Eliminates black-box automation surprises by letting you inspect every evaluation step.",
                howToUse = "Trigger a clock, location, activity, or incoming call event and read the trace card.",
                whatHappens = "Executes the simulated decision and surfaces conflict resolution prompts when needed."
            )
        }
    }
}
