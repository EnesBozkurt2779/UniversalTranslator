package com.translator.universal.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationMemoryService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Translation Memory - Store and reuse previous translations
    
    data class TMEntry(
        val id: Long = System.currentTimeMillis(),
        val sourceText: String,
        val targetText: String,
        val sourceLang: String,
        val targetLang: String,
        val domain: String = "general",
        val quality: Int = 100, // 0-100
        val usageCount: Int = 0,
        val createdAt: Long = System.currentTimeMillis(),
        val lastUsedAt: Long = System.currentTimeMillis()
    )

    private val tmDatabase = mutableListOf<TMEntry>()

    // Fuzzy matching for TM
    fun findMatch(sourceText: String, sourceLang: String, targetLang: String, minScore: Int = 70): TMEntry? {
        return tmDatabase
            .filter { it.sourceLang == sourceLang && it.targetLang == targetLang }
            .map { entry ->
                val score = calculateSimilarity(sourceText, entry.sourceText)
                Pair(entry, score)
            }
            .filter { it.second >= minScore }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun calculateSimilarity(s1: String, s2: String): Int {
        if (s1 == s2) return 100
        if (s1.isEmpty() || s2.isEmpty()) return 0
        
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        val editDistance = levenshteinDistance(longer, shorter)
        return ((longer.length - editDistance) * 100 / longer.length)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i-1] == s2[j-1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }

    fun addEntry(entry: TMEntry) {
        // Check for duplicates
        val existing = tmDatabase.find { 
            it.sourceText == entry.sourceText && 
            it.targetLang == entry.targetLang 
        }
        
        if (existing != null) {
            existing.usageCount++
            existing.lastUsedAt = System.currentTimeMillis()
        } else {
            tmDatabase.add(entry)
        }
    }

    fun getSuggestions(prefix: String, sourceLang: String, targetLang: String): List<TMEntry> {
        return tmDatabase
            .filter { 
                it.sourceText.startsWith(prefix, ignoreCase = true) &&
                it.sourceLang == sourceLang &&
                it.targetLang == targetLang 
            }
            .sortedByDescending { it.usageCount }
            .take(5)
    }

    fun exportTM(): String {
        return tmDatabase.joinToString("\n") { 
            "${it.sourceText}\t${it.targetText}\t${it.sourceLang}\t${it.targetLang}\t${it.domain}"
        }
    }

    fun importTM(data: String) {
        data.lines().forEach { line ->
            val parts = line.split("\t")
            if (parts.size >= 5) {
                addEntry(TMEntry(
                    sourceText = parts[0],
                    targetText = parts[1],
                    sourceLang = parts[2],
                    targetLang = parts[3],
                    domain = parts.getOrElse(4) { "general" }
                ))
            }
        }
    }
}

@Singleton
class GlossaryService @Inject constructor() {
    // Terminology management for specific domains
    
    data class Glossary(
        val id: String,
        val name: String,
        val description: String,
        val sourceLang: String,
        val targetLang: String,
        val category: String,
        val terms: List<GlossaryTerm>
    )

    data class GlossaryTerm(
        val sourceTerm: String,
        val targetTerm: String,
        val definition: String? = null,
        val context: String? = null,
        val notes: String? = null
    )

    private val glossaries = mutableListOf<Glossary>()

    fun createGlossary(name: String, description: String, sourceLang: String, targetLang: String, category: String): Glossary {
        val glossary = Glossary(
            id = System.currentTimeMillis().toString(),
            name = name,
            description = description,
            sourceLang = sourceLang,
            targetLang = targetLang,
            category = category,
            terms = emptyList()
        )
        glossaries.add(glossary)
        return glossary
    }

    fun addTerm(glossaryId: String, term: GlossaryTerm) {
        val glossary = glossaries.find { it.id == glossaryId }
        glossary?.let {
            val updatedTerms = it.terms.toMutableList()
            updatedTerms.add(term)
            val index = glossaries.indexOf(it)
            glossaries[index] = it.copy(terms = updatedTerms)
        }
    }

    fun translateWithGlossary(text: String, glossaryId: String): String {
        val glossary = glossaries.find { it.id == glossaryId } ?: return text
        var result = text
        
        glossary.terms.forEach { term ->
            result = result.replace(term.sourceTerm, term.targetTerm, ignoreCase = true)
        }
        
        return result
    }

    fun getAllGlossaries(): List<Glossary> = glossaries.toList()

    fun getDefaultGlossaries(): List<Glossary> {
        // Pre-built glossaries for common domains
        return listOf(
            createGlossary("Tıp Terimleri", "Tıbbi terminoloji", "en", "tr", "medical"),
            createGlossary("Hukuk Terimleri", "Yasal terminoloji", "en", "tr", "legal"),
            createGlossary("Teknik Terimler", "Mühendislik terimleri", "en", "tr", "technical"),
            createGlossary("BT Terimleri", "Bilgi teknolojileri", "en", "tr", "it"),
            createGlossary("Finans Terimleri", "Finansal terminoloji", "en", "tr", "finance")
        )
    }
}

@Singleton
class QualityAssuranceService @Inject constructor() {
    // Translation quality checking
    
    data class QualityIssue(
        val type: IssueType,
        val severity: Severity,
        val message: String,
        val position: Int,
        val suggestion: String?
    )

    enum class IssueType {
        SPELLING,
        GRAMMAR,
        PUNCTUATION,
        TERMINOLOGY,
        STYLE,
        CONSISTENCY,
        ACCURACY
    }

    enum class Severity {
        ERROR, WARNING, INFO
    }

    data class QualityReport(
        val score: Int, // 0-100
        val issues: List<QualityIssue>,
        val wordCount: Int,
        val characterCount: Int,
        val readingTime: Int // seconds
    )

    fun checkQuality(source: String, translation: String, glossary: String? = null): QualityReport {
        val issues = mutableListOf<QualityIssue>()
        
        // Check for empty translation
        if (translation.isBlank()) {
            issues.add(QualityIssue(
                type = IssueType.ACCURACY,
                severity = Severity.ERROR,
                message = "Çeviri boş",
                position = 0,
                suggestion = "Lütfen çeviri yapın"
            ))
        }

        // Check for untranslated content
        val untranslatedCount = source.split(" ").count { word ->
            translation.contains(word, ignoreCase = true)
        }
        if (untranslatedCount > source.split(" ").size * 0.3) {
            issues.add(QualityIssue(
                type = IssueType.ACCURACY,
                severity = Severity.WARNING,
                message = "Çeviride çevrilmemiş içerik var",
                position = 0,
                suggestion = null
            ))
        }

        // Check consistency (same words translated same way)
        val sourceWords = source.lowercase().split(" ").filter { it.length > 4 }
        val uniqueTranslations = mutableSetOf<String>()
        sourceWords.forEach { word ->
            if (translation.lowercase().contains(word)) {
                uniqueTranslations.add(word)
            }
        }

        // Check punctuation
        if (!translation.endsWith(".") && !translation.endsWith("?") && !translation.endsWith("!")) {
            issues.add(QualityIssue(
                type = IssueType.PUNCTUATION,
                severity = Severity.INFO,
                message = "Cümle noktalama işareti ile bitmiyor",
                position = translation.length - 1,
                suggestion = translation + "."
            ))
        }

        val score = when {
            issues.any { it.severity == Severity.ERROR } -> 50
            issues.any { it.severity == Severity.WARNING } -> 75
            issues.size < 3 -> 90
            else -> 95
        }

        return QualityReport(
            score = score,
            issues = issues,
            wordCount = translation.split(" ").size,
            characterCount = translation.length,
            readingTime = translation.split(" ").size / 3
        )
    }

    fun compareTranslations(original: String, translated: String, alternative: String): String {
        // Would compare multiple translations and suggest the best
        return translated
    }
}

@Singleton
class CollaborationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Team collaboration features
    
    data class Project(
        val id: String,
        val name: String,
        val description: String,
        val members: List<Member>,
        val sourceLang: String,
        val targetLang: String,
        val createdAt: Long,
        val documents: List<ProjectDocument>
    )

    data class Member(
        val id: String,
        val name: String,
        val email: String,
        val role: MemberRole,
        val joinedAt: Long
    )

    enum class MemberRole {
        OWNER, EDITOR, REVIEWER, VIEWER
    }

    data class ProjectDocument(
        val id: String,
        val name: String,
        val status: DocumentStatus,
        val progress: Int,
        val assignedTo: String?
    )

    enum class DocumentStatus {
        PENDING, IN_PROGRESS, REVIEW, COMPLETED, APPROVED
    }

    private val projects = mutableListOf<Project>()

    fun createProject(name: String, description: String, sourceLang: String, targetLang: String): Project {
        val project = Project(
            id = System.currentTimeMillis().toString(),
            name = name,
            description = description,
            members = emptyList(),
            sourceLang = sourceLang,
            targetLang = targetLang,
            createdAt = System.currentTimeMillis(),
            documents = emptyList()
        )
        projects.add(project)
        return project
    }

    fun getProjects(): List<Project> = projects.toList()

    fun addDocumentToProject(projectId: String, document: ProjectDocument) {
        val project = projects.find { it.id == projectId }
        project?.let {
            val updatedDocs = it.documents.toMutableList()
            updatedDocs.add(document)
            val index = projects.indexOf(it)
            projects[index] = it.copy(documents = updatedDocs)
        }
    }

    fun getSharedProjectLink(projectId: String): String {
        // Would generate shareable link
        return "https://translator.app/project/$projectId"
    }
}

@Singleton
class ExpertDomainService @Inject constructor() {
    // Specialized translation domains
    
    data class Domain(
        val id: String,
        val name: String,
        val description: String,
        val icon: String,
        val terms: List<DomainTerm>
    )

    data class DomainTerm(
        val term: String,
        val definition: String,
        val example: String
    )

    fun getDomains(): List<Domain> {
        return listOf(
            Domain(
                id = "medical",
                name = "Tıp & Sağlık",
                description = "Tıbbi terminoloji, anatomi, farmakoloji",
                icon = "medical_services",
                terms = listOf(
                    DomainTerm("diagnosis", "teşhis", "The diagnosis was confirmed."),
                    DomainTerm("treatment", "tedavi", "The treatment is ongoing."),
                    DomainTerm("symptom", "belirti", "Common symptoms include...")
                )
            ),
            Domain(
                id = "legal",
                name = "Hukuk",
                description = "Yasal terminoloji, sözleşmeler, düzenlemeler",
                icon = "gavel",
                terms = listOf(
                    DomainTerm("contract", "sözleşme", "The contract was signed."),
                    DomainTerm("liability", "sorumluluk", "Limited liability."),
                    DomainTerm("plaintiff", "davacı", "The plaintiff filed a lawsuit.")
                )
            ),
            Domain(
                id = "technical",
                name = "Teknik & Mühendislik",
                description = "Mühendislik terminolojisi, teknik belgeler",
                icon = "engineering",
                terms = listOf(
                    DomainTerm("specification", "şartname", "Technical specifications."),
                    DomainTerm("blueprint", "proje çizimi", "The blueprint was approved."),
                    DomainTerm("prototype", "prototip", "The prototype is ready.")
                )
            ),
            Domain(
                id = "finance",
                name = "Finans & Ekonomi",
                description = "Finansal raporlar, yatırım terminolojisi",
                icon = "account_balance",
                terms = listOf(
                    DomainTerm("equity", "öz sermaye", "Shareholder equity."),
                    DomainTerm("dividend", "temettü", "Annual dividend."),
                    DomainTerm("liability", "borç", "Total liabilities.")
                )
            ),
            Domain(
                id = "it",
                name = "Bilgi Teknolojileri",
                description = "Yazılım, donanım, ağ terminolojisi",
                icon = "computer",
                terms = listOf(
                    DomainTerm("algorithm", "algoritma", "The algorithm processes data."),
                    DomainTerm("database", "veritabanı", "Connect to the database."),
                    DomainTerm("encryption", "şifreleme", "End-to-end encryption.")
                )
            ),
            Domain(
                id = "marketing",
                name = "Pazarlama",
                description = "Reklam, sosyal medya, marka yönetimi",
                icon = "campaign",
                terms = listOf(
                    DomainTerm("conversion", "dönüşüm", "Conversion rate increased."),
                    DomainTerm("engagement", "etkileşim", "User engagement is high."),
                    DomainTerm("ROI", "yatırım getirisi", "Calculate the ROI.")
                )
            ),
            Domain(
                id = "academic",
                name = "Akademik",
                description = "Bilimsel makaleler, tezler, araştırma",
                icon = "school",
                terms = listOf(
                    DomainTerm("hypothesis", "hipotez", "The hypothesis was tested."),
                    DomainTerm("methodology", "metodoloji", "Research methodology."),
                    DomainTerm("correlation", "korelasyon", "Strong correlation observed.")
                )
            ),
            Domain(
                id = "literary",
                name = "Edebiyat",
                description = "Kitaplar, şiirler, edebi eserler",
                icon = "menu_book",
                terms = listOf(
                    DomainTerm("metaphor", "metafor", "The metaphor is powerful."),
                    DomainTerm("alliteration", "aliterasyon", "Alliteration in poetry."),
                    DomainTerm("narrative", "anlatı", "First-person narrative.")
                )
            ),
            Domain(
                id = "culinary",
                name = "Mutfak & Gastronomi",
                description = "Yemek tarifleri, gastronomi",
                icon = "restaurant",
                terms = listOf(
                    DomainTerm("simmer", "kaynatmak", "Simmer for 20 minutes."),
                    DomainTerm("sauté", "kavurmak", "Sauté the vegetables."),
                    DomainTerm("marinate", "marine etmek", "Marinate overnight.")
                )
            ),
            Domain(
                id = "sports",
                name = "Spor",
                description = "Spor terminolojisi, kurallar",
                icon = "sports",
                terms = listOf(
                    DomainTerm("dribble", "dribbling", "Dribble past the defender."),
                    DomainTerm("penalty", "penaltı", "Penalty kick."),
                    DomainTerm("overtime", "uzatma", "Overtime period.")
                )
            )
        )
    }

    fun translateWithDomain(text: String, domainId: String): String {
        // Would apply domain-specific terminology
        return text
    }
}