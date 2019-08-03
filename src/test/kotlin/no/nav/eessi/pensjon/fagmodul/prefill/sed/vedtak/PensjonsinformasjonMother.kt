package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import no.nav.eessi.pensjon.utils.convertToXMLocal
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sakalder.V1SakAlder
import no.nav.pensjon.v1.trygdetid.V1Trygdetid
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vedtak.V1Vedtak
import no.nav.pensjon.v1.vilkarsvurdering.V1Vilkarsvurdering
import no.nav.pensjon.v1.vilkarsvurderingliste.V1VilkarsvurderingListe
import no.nav.pensjon.v1.vilkarsvurderinguforetrygd.V1VilkarsvurderingUforetrygd
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import no.nav.pensjon.v1.ytelsepermaanedliste.V1YtelsePerMaanedListe
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory

object PensjonsinformasjonMother {

    fun pensjoninformasjonForSakstype(saksType: String) =
            Pensjonsinformasjon().apply {
                    sakAlder = V1SakAlder().apply {
                        sakType = saksType
                        isUttakFor67 = false
                    }
                    vedtak = V1Vedtak().apply {
                        datoFattetVedtak = dummyDate()
                        virkningstidspunkt = dummyDate()
                        isBoddArbeidetUtland = true
                    }
                    ytelsePerMaanedListe = V1YtelsePerMaanedListe().apply {
                        ytelsePerMaanedListe.add(V1YtelsePerMaaned().apply {
                            isMottarMinstePensjonsniva = true
                            fom = dummyDate()
                            tom = dummyDate()
                        })
                    }
                    vilkarsvurderingListe = V1VilkarsvurderingListe().apply {
                        vilkarsvurderingListe.add(V1Vilkarsvurdering().apply {
                            avslagHovedytelse = "ALDER_PPP"
                            vilkarsvurderingUforetrygd = V1VilkarsvurderingUforetrygd().apply {
                                alder = "ALDER"
                                nedsattInntektsevne = "SANN"
                            }
                        })
                    }
                    trygdetidListe = trePerioderMed5DagerHver()
                }

    fun trePerioderMed5DagerHver() =
            V1TrygdetidListe().apply {
                trygdetidListe.add(V1Trygdetid().apply {
                    fom = 25.daysAgo()
                    tom = 20.daysAgo()
                })
                trygdetidListe.add(V1Trygdetid().apply {
                    fom = 10.daysAgo()
                    tom = 5.daysAgo()
                })
                trygdetidListe.add(V1Trygdetid().apply {
                    fom = 0.daysAgo()
                    tom = 5.daysAhead()
                })
            }
}
private fun dummyDate() = DatatypeFactory.newInstance().newXMLGregorianCalendar()
fun Int.daysAgo() = convertToXMLocal(LocalDate.now().minusDays(this.toLong()))
fun Int.daysAhead() = convertToXMLocal(LocalDate.now().plusDays(this.toLong()))
