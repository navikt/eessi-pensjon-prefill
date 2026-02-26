package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.utils.convertToXMLocal
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory

object PensjonsinformasjonMother {

//    fun pensjoninformasjonForSakstype(saksType: String) =
//            Pensjonsinformasjon().apply {
//                    sakAlder = V1SakAlder().apply {
//                        sakType = saksType
//                        isUttakFor67 = false
//                    }
//                    vedtak = V1Vedtak().apply {
//                        datoFattetVedtak = dummyDate()
//                        virkningstidspunkt = dummyDate()
//                        isBoddArbeidetUtland = true
//                    }
//                    ytelsePerMaanedListe = V1YtelsePerMaanedListe().apply {
//                        ytelsePerMaanedListe.add(V1YtelsePerMaaned().apply {
//                            isMottarMinstePensjonsniva = true
//                            fom = dummyDate()
//                            tom = dummyDate()
//                        })
//                    }
//                    vilkarsvurderingListe = V1VilkarsvurderingListe().apply {
//                        vilkarsvurderingListe.add(V1Vilkarsvurdering().apply {
//                            avslagHovedytelse = "ALDER_PPP"
//                            vilkarsvurderingUforetrygd = V1VilkarsvurderingUforetrygd().apply {
//                                alder = "ALDER"
//                                nedsattInntektsevne = "SANN"
//                            }
//                        })
//                    }
//                    trygdetidListe = trePerioderMed5DagerHver()
//                }

//    fun trePerioderMed5DagerHver() =
//            V1TrygdetidListe().apply {
//                trygdetidListe.add(V1Trygdetid().apply {
//                    fom = 25.daysAgo()
//                    tom = 20.daysAgo()
//                })
//                trygdetidListe.add(V1Trygdetid().apply {
//                    fom = 10.daysAgo()
//                    tom = 5.daysAgo()
//                })
//                trygdetidListe.add(V1Trygdetid().apply {
//                    fom = 0.daysAgo()
//                    tom = 5.daysAhead()
//                })
//            }
}
private fun dummyDate() = DatatypeFactory.newInstance().newXMLGregorianCalendar()
fun Int.daysAgo() = convertToXMLocal(LocalDate.now().minusDays(this.toLong()))
fun Int.daysAhead() = convertToXMLocal(LocalDate.now().plusDays(this.toLong()))
