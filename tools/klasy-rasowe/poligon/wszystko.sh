#!/bin/sh
echo "=== exp"; node /s/exp-zabicie.js 2>&1 | grep -E "PASS|FAIL"
echo "=== spec"; node /s/spec-e2e.js 2>&1 | grep -E "^(FAIL|WYNIK)|widać skutek"
echo "=== sygnatury"; node /s/p6-e2e.js 2>&1 | grep -E "^(FAIL|WYNIK)"
