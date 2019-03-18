EESSI Pensjon Fagmodul
======================

 -=<>=- Electronic Exchange of Social Security Information  -=<>=-


# Utvikling

## Komme i gang

```
./gradlew assemble
```

legge systembruker 'srveessipensjon' og passord inn som systemverdier:
SRVEESSI_PENSJON_USERNAME
SRVEESSI_PENSJON_PASSWORD_T


## Oppdatere avhengigheter

Sjekke om man har utdaterte avhengigheter (forsøker å unngå milestones og beta-versjoner):

```
./gradlew dependencyUpdates
```

Enkelte interne artifakter har ikke komplette metadata i repo.adeo.no - noe som gir en advarsel:

```
 Failed to determine the latest version for the following dependencies (use --info for details):
 - no.nav.pensjon:pensjonsinformasjon-xsd
 - no.nav.tjenester:nav-person-v3-tjenestespesifikasjon
```

Dersom du er supertrygg på testene kan du forsøke en oppdatering av alle avhengighetene:

```
./gradlew useLatestVersions && ./gradlew useLatestVersionsCheck
```

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #eessi-pensjonpub.
