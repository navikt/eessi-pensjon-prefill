package no.nav.eessi.eessifagmodul.models

data class LandspesifikkInfoModel (

        var norge: Norge? = null
)

data class Norge (


        var  ekstraInfoUfore: EkstraInfoUfore? = null
)

//AdditionalInformationForInvalidityPensionClaim
data class EkstraInfoUfore (

        var ekstraInfoBarn: EkstraInfoBarn? = null
)

data class EkstraInfoBarn (

        var adresse: Adresse? = null
)