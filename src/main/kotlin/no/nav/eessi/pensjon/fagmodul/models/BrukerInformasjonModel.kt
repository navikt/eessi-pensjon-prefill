package no.nav.eessi.pensjon.fagmodul.models

import java.util.*

//fra UI til Fagmodul (person info)
data class BrukerInformasjon(
        //bank
        var bankAddress: String?,
        var bankBicSwift: String?,
        var bankCode: String?,
        var bankCountry: BankCountry?,
        var bankIban: String?,
        var bankName: String?,
        var userEmail: String?,
        var userPhone: String?,
        //work
        var retirementCountry: RetirementCountry?,
        var workType: String?,
        var workHourPerWeek: String?,
        var workIncome: String?,
        var workIncomeCurrency: WorkIncomeCurrency?,
        var workPaymentDate: String?,
        var workPaymentFrequency: String?,
        var workEstimatedRetirementDate: Date?,
        var workStartDate: Date?,
        var workEndDate: Date?

)

data class BankCountry(
        var currency: String?,
        var currencyLabel: String?,
        var label: String?,
        var value: String?
)

data class WorkIncomeCurrency(
        var currency: String?,
        var currencyLabel: String?,
        var label: String?,
        var value: String?
)

data class RetirementCountry(
        var currency: String?,
        var currencyLabel: String?,
        var label: String?,
        var value: String?
)