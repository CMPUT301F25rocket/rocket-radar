#!/usr/bin/env bash

for diagram in *.mmd; do
    [ -e "${diagram}" ] || continue
    # https://stackoverflow.com/a/125340 
    # I did not know this was a thing.
    name="${diagram%.mmd}"
    printf "Now generating ${name}.svg\n"
    mmdc -i "${diagram}" -o "$1/uml-${name}.svg"
done
