#!/bin/sh
if [ -f /shared_fold/baza.fbk ] && [ ! -f /shared_fold/baza.fdb ]; then
  gbak -c -v -user sysdba -password masterkey /shared_fold/baza.fbk /shared_fold/baza.fdb
fi
chmod 666 /shared_fold/baza.fdb
