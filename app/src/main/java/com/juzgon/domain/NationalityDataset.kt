@file:Suppress("MaxLineLength")

package com.juzgon.domain

data class NationalityOption(
    val code: String,
    val nationality: String,
    val country: String,
    val flagEmoji: String,
    val aliases: List<String> = emptyList(),
)

object NationalityDataset {
    val all: List<NationalityOption> =
        buildList {
            add(NationalityOption("AF", "Afghan", "Afghanistan", "\uD83C\uDDE6\uD83C\uDDEB"))
            add(NationalityOption("AL", "Albanian", "Albania", "\uD83C\uDDE6\uD83C\uDDF1"))
            add(NationalityOption("DZ", "Algerian", "Algeria", "\uD83C\uDDE9\uD83C\uDDFF"))
            add(NationalityOption("AR", "Argentine", "Argentina", "\uD83C\uDDE6\uD83C\uDDF7", listOf("Argentinian")))
            add(NationalityOption("AU", "Australian", "Australia", "\uD83C\uDDE6\uD83C\uDDFA", listOf("Aussie")))
            add(NationalityOption("AT", "Austrian", "Austria", "\uD83C\uDDE6\uD83C\uDDF9"))
            add(NationalityOption("BD", "Bangladeshi", "Bangladesh", "\uD83C\uDDE7\uD83C\uDDE9"))
            add(NationalityOption("BE", "Belgian", "Belgium", "\uD83C\uDDE7\uD83C\uDDEA"))
            add(NationalityOption("BO", "Bolivian", "Bolivia", "\uD83C\uDDE7\uD83C\uDDF4"))
            add(NationalityOption("BR", "Brazilian", "Brazil", "\uD83C\uDDE7\uD83C\uDDF7"))
            add(NationalityOption("BG", "Bulgarian", "Bulgaria", "\uD83C\uDDE7\uD83C\uDDEC"))
            add(NationalityOption("KH", "Cambodian", "Cambodia", "\uD83C\uDDF0\uD83C\uDDED"))
            add(NationalityOption("CM", "Cameroonian", "Cameroon", "\uD83C\uDDE8\uD83C\uDDF2"))
            add(NationalityOption("CA", "Canadian", "Canada", "\uD83C\uDDE8\uD83C\uDDE6"))
            add(NationalityOption("CL", "Chilean", "Chile", "\uD83C\uDDE8\uD83C\uDDF1"))
            add(NationalityOption("CN", "Chinese", "China", "\uD83C\uDDE8\uD83C\uDDF3"))
            add(NationalityOption("CO", "Colombian", "Colombia", "\uD83C\uDDE8\uD83C\uDDF4"))
            add(NationalityOption("CR", "Costa Rican", "Costa Rica", "\uD83C\uDDE8\uD83C\uDDF7"))
            add(NationalityOption("HR", "Croatian", "Croatia", "\uD83C\uDDED\uD83C\uDDF7"))
            add(NationalityOption("CU", "Cuban", "Cuba", "\uD83C\uDDE8\uD83C\uDDFA"))
            add(NationalityOption("CZ", "Czech", "Czech Republic", "\uD83C\uDDE8\uD83C\uDDFF", listOf("Czechia")))
            add(NationalityOption("DK", "Danish", "Denmark", "\uD83C\uDDE9\uD83C\uDDF0"))
            add(NationalityOption("DO", "Dominican", "Dominican Republic", "\uD83C\uDDE9\uD83C\uDDF4"))
            add(NationalityOption("EC", "Ecuadorian", "Ecuador", "\uD83C\uDDEA\uD83C\uDDE8"))
            add(NationalityOption("EG", "Egyptian", "Egypt", "\uD83C\uDDEA\uD83C\uDDEC"))
            add(NationalityOption("SV", "Salvadoran", "El Salvador", "\uD83C\uDDF8\uD83C\uDDFB"))
            add(
                NationalityOption(
                    "GB",
                    "British",
                    "United Kingdom",
                    "\uD83C\uDDEC\uD83C\uDDE7",
                    listOf("English", "UK", "Scottish", "Welsh"),
                ),
            )
            add(NationalityOption("EE", "Estonian", "Estonia", "\uD83C\uDDEA\uD83C\uDDEA"))
            add(NationalityOption("ET", "Ethiopian", "Ethiopia", "\uD83C\uDDEA\uD83C\uDDF9"))
            add(NationalityOption("FI", "Finnish", "Finland", "\uD83C\uDDEB\uD83C\uDDEE"))
            add(NationalityOption("FR", "French", "France", "\uD83C\uDDEB\uD83C\uDDF7"))
            add(NationalityOption("DE", "German", "Germany", "\uD83C\uDDE9\uD83C\uDDEA"))
            add(NationalityOption("GH", "Ghanaian", "Ghana", "\uD83C\uDDEC\uD83C\uDDED"))
            add(NationalityOption("GR", "Greek", "Greece", "\uD83C\uDDEC\uD83C\uDDF7"))
            add(NationalityOption("GT", "Guatemalan", "Guatemala", "\uD83C\uDDEC\uD83C\uDDF9"))
            add(NationalityOption("HN", "Honduran", "Honduras", "\uD83C\uDDED\uD83C\uDDF3"))
            add(NationalityOption("HK", "Hong Kongese", "Hong Kong", "\uD83C\uDDED\uD83C\uDDF0"))
            add(NationalityOption("HU", "Hungarian", "Hungary", "\uD83C\uDDED\uD83C\uDDFA"))
            add(NationalityOption("IS", "Icelandic", "Iceland", "\uD83C\uDDEE\uD83C\uDDF8"))
            add(NationalityOption("IN", "Indian", "India", "\uD83C\uDDEE\uD83C\uDDF3"))
            add(NationalityOption("ID", "Indonesian", "Indonesia", "\uD83C\uDDEE\uD83C\uDDE9"))
            add(NationalityOption("IR", "Iranian", "Iran", "\uD83C\uDDEE\uD83C\uDDF7", listOf("Persian")))
            add(NationalityOption("IQ", "Iraqi", "Iraq", "\uD83C\uDDEE\uD83C\uDDF6"))
            add(NationalityOption("IE", "Irish", "Ireland", "\uD83C\uDDEE\uD83C\uDDEA"))
            add(NationalityOption("IL", "Israeli", "Israel", "\uD83C\uDDEE\uD83C\uDDF1"))
            add(NationalityOption("IT", "Italian", "Italy", "\uD83C\uDDEE\uD83C\uDDF9"))
            add(NationalityOption("CI", "Ivorian", "Ivory Coast", "\uD83C\uDDE8\uD83C\uDDEE", listOf("Cote d'Ivoire")))
            add(NationalityOption("JM", "Jamaican", "Jamaica", "\uD83C\uDDEF\uD83C\uDDF2"))
            add(NationalityOption("JP", "Japanese", "Japan", "\uD83C\uDDEF\uD83C\uDDF5"))
            add(NationalityOption("JO", "Jordanian", "Jordan", "\uD83C\uDDEF\uD83C\uDDF4"))
            add(NationalityOption("KZ", "Kazakh", "Kazakhstan", "\uD83C\uDDF0\uD83C\uDDFF"))
            add(NationalityOption("KE", "Kenyan", "Kenya", "\uD83C\uDDF0\uD83C\uDDEA"))
            add(NationalityOption("KR", "South Korean", "South Korea", "\uD83C\uDDF0\uD83C\uDDF7", listOf("Korean")))
            add(NationalityOption("KW", "Kuwaiti", "Kuwait", "\uD83C\uDDF0\uD83C\uDDFC"))
            add(NationalityOption("LV", "Latvian", "Latvia", "\uD83C\uDDF1\uD83C\uDDFB"))
            add(NationalityOption("LB", "Lebanese", "Lebanon", "\uD83C\uDDF1\uD83C\uDDE7"))
            add(NationalityOption("LT", "Lithuanian", "Lithuania", "\uD83C\uDDF1\uD83C\uDDF9"))
            add(NationalityOption("MY", "Malaysian", "Malaysia", "\uD83C\uDDF2\uD83C\uDDFE"))
            add(NationalityOption("MX", "Mexican", "Mexico", "\uD83C\uDDF2\uD83C\uDDFD"))
            add(NationalityOption("MA", "Moroccan", "Morocco", "\uD83C\uDDF2\uD83C\uDDE6"))
            add(NationalityOption("MM", "Burmese", "Myanmar", "\uD83C\uDDF2\uD83C\uDDF2", listOf("Burma")))
            add(NationalityOption("NP", "Nepalese", "Nepal", "\uD83C\uDDF3\uD83C\uDDF5", listOf("Nepali")))
            add(NationalityOption("NL", "Dutch", "Netherlands", "\uD83C\uDDF3\uD83C\uDDF1", listOf("Holland")))
            add(NationalityOption("NZ", "New Zealander", "New Zealand", "\uD83C\uDDF3\uD83C\uDDFF", listOf("Kiwi")))
            add(NationalityOption("NI", "Nicaraguan", "Nicaragua", "\uD83C\uDDF3\uD83C\uDDEE"))
            add(NationalityOption("NG", "Nigerian", "Nigeria", "\uD83C\uDDF3\uD83C\uDDEC"))
            add(NationalityOption("NO", "Norwegian", "Norway", "\uD83C\uDDF3\uD83C\uDDF4"))
            add(NationalityOption("PK", "Pakistani", "Pakistan", "\uD83C\uDDF5\uD83C\uDDF0"))
            add(NationalityOption("PA", "Panamanian", "Panama", "\uD83C\uDDF5\uD83C\uDDE6"))
            add(NationalityOption("PY", "Paraguayan", "Paraguay", "\uD83C\uDDF5\uD83C\uDDFE"))
            add(NationalityOption("PE", "Peruvian", "Peru", "\uD83C\uDDF5\uD83C\uDDEA"))
            add(NationalityOption("PH", "Filipino", "Philippines", "\uD83C\uDDF5\uD83C\uDDED"))
            add(NationalityOption("PL", "Polish", "Poland", "\uD83C\uDDF5\uD83C\uDDF1"))
            add(NationalityOption("PT", "Portuguese", "Portugal", "\uD83C\uDDF5\uD83C\uDDF9"))
            add(NationalityOption("PR", "Puerto Rican", "Puerto Rico", "\uD83C\uDDF5\uD83C\uDDF7"))
            add(NationalityOption("QA", "Qatari", "Qatar", "\uD83C\uDDF6\uD83C\uDDE6"))
            add(NationalityOption("RO", "Romanian", "Romania", "\uD83C\uDDF7\uD83C\uDDF4"))
            add(NationalityOption("RU", "Russian", "Russia", "\uD83C\uDDF7\uD83C\uDDFA"))
            add(NationalityOption("SA", "Saudi", "Saudi Arabia", "\uD83C\uDDF8\uD83C\uDDE6", listOf("Saudi Arabian")))
            add(NationalityOption("SN", "Senegalese", "Senegal", "\uD83C\uDDF8\uD83C\uDDF3"))
            add(NationalityOption("RS", "Serbian", "Serbia", "\uD83C\uDDF7\uD83C\uDDF8"))
            add(NationalityOption("SG", "Singaporean", "Singapore", "\uD83C\uDDF8\uD83C\uDDEC"))
            add(NationalityOption("SK", "Slovak", "Slovakia", "\uD83C\uDDF8\uD83C\uDDF0"))
            add(NationalityOption("SI", "Slovenian", "Slovenia", "\uD83C\uDDF8\uD83C\uDDEE"))
            add(NationalityOption("ZA", "South African", "South Africa", "\uD83C\uDDFF\uD83C\uDDE6"))
            add(NationalityOption("ES", "Spanish", "Spain", "\uD83C\uDDEA\uD83C\uDDF8"))
            add(NationalityOption("LK", "Sri Lankan", "Sri Lanka", "\uD83C\uDDF1\uD83C\uDDF0"))
            add(NationalityOption("SD", "Sudanese", "Sudan", "\uD83C\uDDF8\uD83C\uDDE9"))
            add(NationalityOption("SE", "Swedish", "Sweden", "\uD83C\uDDF8\uD83C\uDDEA"))
            add(NationalityOption("CH", "Swiss", "Switzerland", "\uD83C\uDDE8\uD83C\uDDED"))
            add(NationalityOption("SY", "Syrian", "Syria", "\uD83C\uDDF8\uD83C\uDDFE"))
            add(NationalityOption("TW", "Taiwanese", "Taiwan", "\uD83C\uDDF9\uD83C\uDDFC"))
            add(NationalityOption("TZ", "Tanzanian", "Tanzania", "\uD83C\uDDF9\uD83C\uDDFF"))
            add(NationalityOption("TH", "Thai", "Thailand", "\uD83C\uDDF9\uD83C\uDDED"))
            add(NationalityOption("TT", "Trinidadian", "Trinidad and Tobago", "\uD83C\uDDF9\uD83C\uDDF9"))
            add(NationalityOption("TN", "Tunisian", "Tunisia", "\uD83C\uDDF9\uD83C\uDDF3"))
            add(NationalityOption("TR", "Turkish", "Turkey", "\uD83C\uDDF9\uD83C\uDDF7", listOf("Turkiye")))
            add(NationalityOption("UA", "Ukrainian", "Ukraine", "\uD83C\uDDFA\uD83C\uDDE6"))
            add(NationalityOption("AE", "Emirati", "United Arab Emirates", "\uD83C\uDDE6\uD83C\uDDEA", listOf("UAE")))
            add(NationalityOption("US", "American", "United States", "\uD83C\uDDFA\uD83C\uDDF8", listOf("USA", "United States of America")))
            add(NationalityOption("UY", "Uruguayan", "Uruguay", "\uD83C\uDDFA\uD83C\uDDFE"))
            add(NationalityOption("UZ", "Uzbek", "Uzbekistan", "\uD83C\uDDFA\uD83C\uDDFF"))
            add(NationalityOption("VE", "Venezuelan", "Venezuela", "\uD83C\uDDFB\uD83C\uDDEA"))
            add(NationalityOption("VN", "Vietnamese", "Vietnam", "\uD83C\uDDFB\uD83C\uDDF3"))
            add(NationalityOption("YE", "Yemeni", "Yemen", "\uD83C\uDDFE\uD83C\uDDEA"))
            add(NationalityOption("ZM", "Zambian", "Zambia", "\uD83C\uDDFF\uD83C\uDDF2"))
            add(NationalityOption("ZW", "Zimbabwean", "Zimbabwe", "\uD83C\uDDFF\uD83C\uDDFC"))
        }

    private val byCode: Map<String, NationalityOption> =
        all.associateBy { it.code.uppercase() }

    fun findByCode(code: String): NationalityOption? = byCode[code.uppercase()]

    fun search(query: String): List<NationalityOption> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return all
        val lower = trimmed.lowercase()
        return all.filter { option ->
            option.nationality.lowercase().startsWith(lower) ||
                option.country.lowercase().startsWith(lower) ||
                option.aliases.any { alias -> alias.lowercase().startsWith(lower) }
        }
    }
}
