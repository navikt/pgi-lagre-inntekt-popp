# pgi-lagre-inntekt-popp
Henter inntekter fra kafka topic ```privat-pgi-inntekt```, og gjør deretter et REST kall mot POPP for å lagre inntekten.

For å se hvordan inntektene publiseres til ```privat-pgi-inntekt```, se følgende github repo: [pgi-les-inntekt-skatt](https://github.com/navikt/pgi-les-inntekt-skatt/)

Dokumentasjon lagringstjeneste i POPP: [TODO: Swagger?]()

#### Bygge lokalt
For å bygge lokalt, så må man ha satt environment variablene GITHUB_ACTOR og GITHUB_TOKEN.
Generer nytt token her: https://github.com/settings/tokens. Husk å KUN gi den følgende tilgangen:

```read:packages Download packages from github package registry```.

Med tokenet generert så har jeg satt det opp slik i .zshrc/.bashrc
```
export GITHUB_ACTOR="username"
# Read only token for downloading github packages
export GITHUB_TOKEN="token"
```

#### Metrikker
Grafana dashboards brukes for å f.eks. monitorere minne, cpu-bruk og andre metrikker.
Se [pgi-lagre-inntekt-popp grafana dasboard](https://grafana.adeo.no/) TODO: Fiks link

#### Logging
[Kibana](https://logs.adeo.no/app/kibana) benyttes til logging. Søk på f.eks. ```application:pgi-les-inntekt-skatt AND envclass:q``` for logginnslag fra preprod.

#### Kontakt
Kontakt Team Samhandling dersom du har noen spørsmål. Vi finnes blant annet på Slack, i kanalen [#samhandling_pensjonsområdet](https://nav-it.slack.com/archives/CQ08JC3UG)

