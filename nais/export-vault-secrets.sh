#!/usr/bin/env bash

echo "Sjekker eessi-pensjon-fagmodul srvPassord"
if test -f /var/run/secrets/nais.io/srveessipensjon/password;
then
  echo "Setter eessi-pensjon-fagmodul srvPassord"
    export password=$(cat /var/run/secrets/nais.io/srveessipensjon/password)
fi

echo "Sjekker eessi-pensjon-fagmodul srvUsername"
if test -f /var/run/secrets/nais.io/srveessipensjon/username;
then
    echo "Setter eessi-pensjon-fagmodul srvUsername"
    export username=$(cat /var/run/secrets/nais.io/srveessipensjon/username)
fi