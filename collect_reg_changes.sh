cd SporeModder-FX
lastTagCommit=$(git rev-list -n 1 $(git describe --tags --abbrev=0))
git diff -U0 $lastTagCommit $1 | grep '^[+]' | grep -Ev '^(--- a/|\+\+\+ b/)' | cut -c2- 
