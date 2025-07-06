#!/bin/bash
gsec -user sysdba -password masterkey <<EOF
add newuser -pw newpassword
quit
EOF
