package tv.garden.global.webapp

import java.util.Locale

object LanguageManager {
    private val lang = Locale.getDefault().language
    fun isTurkish(): Boolean = lang == "tr"

    private val languageToCountries = mapOf(
        "tr" to listOf("TR"),
        "de" to listOf("DE", "AT", "CH"),
        "fr" to listOf("FR", "BE", "CA"),
        "es" to listOf("ES", "MX", "AR", "CO", "CL"),
        "ru" to listOf("RU", "BY", "KZ"),
        "pt" to listOf("PT", "BR"),
        "ar" to listOf("SA", "AE", "EG", "QA", "KW", "IQ"),
        "zh" to listOf("CN", "TW", "HK"),
        "en" to listOf("US", "GB")
    )

    fun getLocalCountryCodes(): List<String> {
        val currentLang = Locale.getDefault().language.lowercase(Locale.ROOT)
        val mapped = languageToCountries[currentLang]
        if (!mapped.isNullOrEmpty()) return mapped
        val currentCountry = Locale.getDefault().country.uppercase(Locale.ROOT)
        return if (currentCountry.isNotBlank()) listOf(currentCountry) else emptyList()
    }

    private fun getText(
        tr: String, es: String, fr: String, de: String,
        ru: String, pt: String, ar: String, zh: String, def: String
    ): String = when (lang) {
        "tr" -> tr; "es" -> es; "fr" -> fr; "de" -> de
        "ru" -> ru; "pt" -> pt; "ar" -> ar; "zh" -> zh; else -> def
    }

    fun getTranslatedCategory(groupRaw: String): String {
        return when (groupRaw.lowercase(Locale.ROOT).trim()) {
            "general", "ulusal", "national", "genel" -> getText(
                "Ulusal / Genel", "Nacional / General", "Général", "Allgemein",
                "Общий", "Geral", "عام", "综合", "General"
            )
            "news", "haberler", "haber" -> getText(
                "Haberler", "Noticias", "Actualités", "Nachrichten",
                "Новости", "Notícias", "أخبار", "新闻", "News"
            )
            "sports", "spor", "sport" -> getText(
                "Spor", "Deportes", "Sports", "Sport",
                "Спорт", "Esportes", "رياضة", "体育", "Sports"
            )
            "kids", "çocuk", "cocuk" -> getText(
                "Çocuk", "Infantil", "Enfants", "Kinder",
                "Детские", "Infantil", "أطفال", "儿童", "Kids"
            )
            "movies", "sinema", "film" -> getText(
                "Sinema / Film", "Películas", "Films", "Filme",
                "Кино", "Filmes", "أفلام", "电影", "Movies"
            )
            "documentary", "belgesel", "doc" -> getText(
                "Belgesel", "Documentales", "Documentaires", "Dokumentarfilm",
                "Документальный", "Documentários", "وثائقي", "纪录片", "Documentary"
            )
            "music", "müzik", "muzik" -> getText(
                "Müzik", "Música", "Musique", "Musik",
                "Музыка", "Música", "موسيقى", "音乐", "Music"
            )
            "religious", "dini", "islamic" -> getText(
                "Dini", "Religioso", "Religieux", "Religiös",
                "Религиозный", "Religioso", "ديني", "宗教", "Religious"
            )
            "entertainment", "eğlence", "eglence" -> getText(
                "Eğlence", "Entretenimiento", "Divertissement", "Unterhaltung",
                "Развлечения", "Entretenimento", "ترفيه", "娱乐", "Entertainment"
            )
            "lifestyle", "yaşam", "yemek", "cooking" -> getText(
                "Yaşam / Yemek", "Estilo de Vida", "Style de vie", "Lifestyle",
                "Стиль жизни", "Estilo de Vida", "أسلوب الحياة", "生活方式", "Lifestyle"
            )
            "education" -> getText(
                "Eğitim", "Educación", "Éducation", "Bildung",
                "Образование", "Educação", "تعليم", "教育", "Education"
            )
            "culture", "kültür", "kultur" -> getText(
                "Kültür", "Cultura", "Culture", "Kultur",
                "Культура", "Cultura", "ثقافة", "文化", "Culture"
            )
            "series", "dizi" -> getText(
                "Dizi", "Series", "Séries", "Serien",
                "Сериалы", "Séries", "مسلسلات", "剧集", "Series"
            )
            else -> groupRaw
        }
    }

    val privacyTitle = getText(
        "Gizlilik ve Rıza Yönetimi", "Privacidad y Consentimiento",
        "Confidentialité et Consentement", "Datenschutz & Einwilligung",
        "Конфиденциальность", "Privacidade e Consentimento",
        "الخصوصية والموافقة", "隐私与同意", "Privacy & Consent"
    )
    val privacyBody = getText(
        "Bu uygulama, kişiselleştirilmiş veya kişiselleştirilmemiş reklamlar sunmak için cihaz tanımlayıcılarını kullanabilir.\n\n• Kişiselleştirilmiş: İlgi alanlarınıza uygun reklamlar gösterilir.\n• Kişiselleştirilmemiş: Genel reklamlar gösterilir.\n\nİstediğiniz zaman Ayarlar menüsünden rıza tercihinizi değiştirebilirsiniz.",
        "Esta aplicación puede usar identificadores de dispositivo para anuncios personalizados o no personalizados.\n\nPuede cambiar su preferencia en Configuración.",
        "Cette application peut utiliser des identifiants d'appareil pour des publicités personnalisées ou non.\n\nVous pouvez modifier vos préférences dans les paramètres.",
        "Diese App kann Gerätekennungen für personalisierte oder nicht personalisierte Werbung verwenden.\n\nSie können Ihre Einwilligung in den Einstellungen ändern.",
        "Это приложение может использовать идентификаторы устройства для персонализированной или неперсонализированной рекламы.\n\nВы можете изменить настройки в меню.",
        "Este app pode usar identificadores de dispositivo para anúncios personalizados ou não.\n\nVocê pode alterar sua preferência em Configurações.",
        "قد يستخدم هذا التطبيق معرفات الجهاز للإعلانات المخصصة أو غير المخصصة.\n\nيمكنك تغيير تفضيلاتك في الإعدادات.",
        "此应用程序可能使用设备标识符来投放个性化或非个性化广告。\n\n您可以在设置中更改偏好。",
        "This app may use device identifiers for personalized or non-personalized ads.\n\n• Personalized: Ads tailored to your interests.\n• Non-personalized: General ads.\n\nYou can change your consent preference anytime in Settings."
    )
    val btnAcceptPersonalized = getText(
        "Kişiselleştirilmiş Kabul", "Aceptar Personalizado", "Accepter Personnalisé",
        "Personalisiert Akzeptieren", "Принять Персонализированные", "Aceitar Personalizado",
        "قبول مخصص", "接受个性化", "Accept Personalized"
    )
    val btnAcceptNonPersonalized = getText(
        "Yalnızca Genel Reklamlar", "Solo Anuncios Generales", "Annonces Générales Uniquement",
        "Nur Allgemeine Werbung", "Только Общие", "Apenas Anúncios Gerais",
        "إعلانات عامة فقط", "仅一般广告", "Non-Personalized Only"
    )
    val searchHint = getText(
        "Kanal Ara...", "Buscar...", "Chercher...", "Suche...",
        "Поиск...", "Procurar...", "بحث...", "搜索...", "Search Channels..."
    )
    val countrySearchHint = getText(
        "Ülke / bölge ara...", "Buscar país...", "Rechercher un pays...", "Land suchen...",
        "Поиск страны...", "Buscar país...", "بحث عن بلد...", "搜索国家...", "Search country..."
    )
    val selectChannel = getText(
        "Kanal Seçimi Bekleniyor...",
        "Esperando selección...",
        "En attente...",
        "Warten...",
        "Ожидание...",
        "Aguardando...",
        "في انتظار الاختيار...",
        "等待选择...",
        "Waiting for Selection..."
    )
    val errorMsg = getText(
        "⚠️ Yayın hatası — listedeki yeriniz korunuyor",
        "⚠️ Error — se mantiene su posición",
        "⚠️ Erreur — position conservée",
        "⚠️ Fehler — Position bleibt",
        "⚠️ Ошибка — позиция сохранена",
        "⚠️ Erro — posição mantida",
        "⚠️ خطأ — يتم الاحتفاظ بموضعك",
        "⚠️ 错误 — 保留当前位置",
        "⚠️ Stream error — keeping your place"
    )
    val loading = getText(
        "Liste Hazırlanıyor... (%d)", "Preparando lista... (%d)", "Préparation... (%d)",
        "Vorbereitung... (%d)", "Загрузка... (%d)", "Preparando... (%d)",
        "جار التحميل... (%d)", "正在加载... (%d)", "Preparing List... (%d)"
    )
    val slowNetwork = getText(
        "Bağlantı yavaş görünüyor, lütfen bekleyin...",
        "La conexión parece lenta, espere...",
        "Connexion lente, veuillez patienter...",
        "Verbindung langsam, bitte warten...",
        "Соединение медленное, подождите...",
        "Conexão lenta, aguarde...",
        "الاتصال بطيء، يرجى الانتظار...",
        "网络较慢，请稍候...",
        "Connection looks slow, please wait..."
    )
    val networkError = getText(
        "İnternet bağlantısı yok. Lütfen kontrol edin.",
        "Sin conexión a Internet.",
        "Pas de connexion Internet.",
        "Keine Internetverbindung.",
        "Нет подключения к Интернету.",
        "Sem conexão com a Internet.",
        "لا يوجد اتصال بالإنترنت.",
        "没有互联网连接。",
        "No internet connection. Please check your network."
    )
    val retryBtn = getText(
        "Tekrar Dene", "Reintentar", "Réessayer", "Wiederholen",
        "Повторить", "Tentar Novamente", "إعادة المحاولة", "重试", "Retry"
    )
    val fetchError = getText(
        "Kanal listesi yüklenemedi.", "No se pudo cargar la lista.",
        "Impossible de charger la liste.", "Kanalliste konnte nicht geladen werden.",
        "Не удалось загрузить список.", "Não foi possível carregar a lista.",
        "تعذر تحميل القائمة.", "无法加载频道列表。", "Failed to load channel list."
    )
    val menuAll = getText("Tümü", "Todos", "Tout", "Alle", "Все", "Tudo", "الكل", "全部", "All")
    val menuGenres = getText(
        "Ülkeler/Bölgeler", "Países/Regiones", "Pays/Régions", "Länder/Regionen",
        "Страны/Регионы", "Países/Regiões", "البلدان / المناطق", "国家/地区", "Countries"
    )
    val menuFavs = getText(
        "Favoriler", "Favoritos", "Favoris", "Favoriten",
        "Избранное", "Favoritos", "المفضلة", "最爱", "Favorites"
    )
    val menuHistory = getText(
        "Geçmiş", "Historial", "Historique", "Verlauf",
        "История", "Histórico", "تاريخ", "历史", "History"
    )
    val close = getText(
        "Kapat", "Cerrar", "Fermer", "Schließen",
        "Закрыть", "Fechar", "إغلاق", "关闭", "Close"
    )
    val settingsTitle = getText(
        "Ayarlar", "Ajustes", "Paramètres", "Einstellungen",
        "Настройки", "Configurações", "الإعدادات", "设置", "Settings"
    )
    val clearCache = getText(
        "Önbelleği Temizle", "Limpiar caché", "Vider le cache", "Cache leeren",
        "Очистить кэш", "Limpar cache", "مسح ذاكرة التخزين المؤقت", "清除缓存", "Clear Cache"
    )
    val cacheCleared = getText(
        "Önbellek temizlendi! (%d KB silindi)", "Caché limpiada! (%d KB eliminados)",
        "Cache vidé! (%d KB supprimés)", "Cache geleert! (%d KB gelöscht)",
        "Кэш очищен! (%d KB удалено)", "Cache limpo! (%d KB removidos)",
        "تم مسح ذاكرة التخزين المؤقت! (%d KB)", "缓存已清除！(%d KB)", "Cache cleared! (%d KB freed)"
    )
    val timerTitle = getText(
        "Uyku Zamanlayıcısı (Dk)",
        "Temporizador (Min)",
        "Minuterie (Min)",
        "Sleep Timer (Min)",
        "Таймер сна (мин)",
        "Temporizador (Min)",
        "مؤقت النوم (دقيقة)",
        "睡眠定时器 (分钟)",
        "Sleep Timer (Min)"
    )
    val setTimer = getText(
        "Zamanı Kur", "Establecer", "Régler", "Setzen",
        "Установить", "Definir", "تعيين", "设置", "Set Timer"
    )
    val consentManage = getText(
        "Rıza Tercihini Yönet", "Gestionar Consentimiento", "Gérer le Consentement",
        "Einwilligung Verwalten", "Управление Согласием", "Gerenciar Consentimento",
        "إدارة الموافقة", "管理同意", "Manage Consent"
    )
    val consentStatus = getText(
        "Mevcut Rıza: %s", "Consentimiento: %s", "Consentement: %s", "Einwilligung: %s",
        "Согласие: %s", "Consentimento: %s", "الموافقة: %s", "同意: %s", "Current Consent: %s"
    )
    val consentPersonalized = getText(
        "Kişiselleştirilmiş", "Personalizado", "Personnalisé", "Personalisiert",
        "Персонализированные", "Personalizado", "مخصص", "个性化", "Personalized"
    )
    val consentNonPersonalized = getText(
        "Kişiselleştirilmemiş", "No Personalizado", "Non Personnalisé", "Nicht Personalisiert",
        "Неперсонализированные", "Não Personalizado", "غير مخصص", "非个性化", "Non-Personalized"
    )
    val consentNone = getText(
        "Rıza Verilmedi", "Sin Consentimiento", "Pas de Consentement", "Keine Einwilligung",
        "Нет Согласия", "Sem Consentimento", "لا موافقة", "未同意", "No Consent"
    )
    val revokeConsent = getText(
        "Rızayı Geri Çek", "Revocar Consentimiento", "Révoquer le Consentement",
        "Einwilligung Widerrufen", "Отозвать Согласие", "Revogar Consentimento",
        "سحب الموافقة", "撤回同意", "Revoke Consent"
    )
    val privacyPolicy = getText(
        "Gizlilik Politikası", "Política de Privacidad", "Politique de Confidentialité",
        "Datenschutzrichtlinie", "Политика Конфиденциальности", "Política de Privacidade",
        "سياسة الخصوصية", "隐私政策", "Privacy Policy"
    )
    val qualityLive = getText(
        "Canlı", "En Vivo", "En Direct", "Live",
        "Прямой", "Ao Vivo", "مباشر", "直播", "Live"
    )
    val international = getText(
        "Uluslararası", "Internacional", "International", "International",
        "Международные", "Internacional", "دولي", "国际", "International"
    )
    val europe = getText(
        "Avrupa", "Europa", "Europe", "Europa",
        "Европа", "Europa", "أوروبا", "欧洲", "Europe"
    )
    val kosovo = getText(
        "Kosova", "Kosovo", "Kosovo", "Kosovo",
        "Косово", "Kosovo", "كوسوفو", "科索沃", "Kosovo"
    )
    val continueWatching = getText(
        "Kaldığın yerden devam", "Continuar viendo", "Reprendre", "Weiterschauen",
        "Продолжить", "Continuar", "متابعة المشاهدة", "继续观看", "Continue watching"
    )
    val hiddenBroken = getText(
        "Gizlenen bozuk yayın: %d", "Streams ocultos: %d", "Flux masqués : %d",
        "Ausgeblendete Streams: %d", "Скрыто каналов: %d", "Streams ocultos: %d",
        "البث المخفي: %d", "已隐藏损坏频道：%d", "Hidden broken streams: %d"
    )
    val restoreBroken = getText(
        "Gizlenen kanalları geri al", "Restaurar ocultos", "Restaurer les masqués",
        "Ausgeblendete wiederherstellen", "Показать скрытые", "Restaurar ocultos",
        "استعادة المخفي", "恢复隐藏频道", "Restore hidden channels"
    )
    val emptyList = getText(
        "Bu listede kanal yok", "No hay canales", "Aucun canal", "Keine Sender",
        "Нет каналов", "Sem canais", "لا توجد قنوات", "没有频道", "No channels here"
    )
    val noEpg = getText(
        "Program bilgisi yok", "Sin guía", "Pas de programme", "Kein Programm",
        "Нет программы", "Sem programação", "لا يوجد برنامج", "暂无节目信息", "No program info"
    )
    val shareApp = getText(
        "NovaStream - Canlı TV uygulamasını dene: ",
        "Prueba NovaStream: ",
        "Essayez NovaStream : ",
        "NovaStream ausprobieren: ",
        "Попробуйте NovaStream: ",
        "Experimente o NovaStream: ",
        "جرّب NovaStream: ",
        "试试 NovaStream：",
        "Try NovaStream Live TV: "
    )
    val castUnavailable = getText(
        "Cast kullanılamıyor", "Cast no disponible", "Cast indisponible",
        "Cast nicht verfügbar", "Cast недоступен", "Cast indisponível",
        "البث غير متاح", "无法投屏", "Cast not available"
    )
    val shareUnavailable = getText(
        "Paylaşım kullanılamıyor", "No se puede compartir", "Partage indisponible",
        "Teilen nicht verfügbar", "Не удалось поделиться", "Compartilhamento indisponível",
        "المشاركة غير متاحة", "无法分享", "Sharing unavailable"
    )
    val nowPlaying = getText(
        "Oynatılıyor", "Reproduciendo", "Lecture", "Wird wiedergegeben",
        "Сейчас", "Reproduzindo", "قيد التشغيل", "正在播放", "Now playing"
    )
}
