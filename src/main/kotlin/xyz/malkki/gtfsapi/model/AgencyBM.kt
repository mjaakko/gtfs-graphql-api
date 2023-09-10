package xyz.malkki.gtfsapi.model

data class AgencyBM(
    val agencyId: String?,
    val name: String,
    val url: String,
    val timezone: String,
    val lang: String?,
    val phone: String?,
    val fareUrl: String?,
    val email: String?
)
