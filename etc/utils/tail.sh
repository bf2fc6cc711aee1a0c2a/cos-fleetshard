#!/bin/bash

stern -l app.kubernetes.io/name="$1" --output=raw --tail=1