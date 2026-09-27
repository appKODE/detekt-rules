#!/usr/bin/env bash
# Runs the published jars through the real detekt CLIs on smoke/sample and checks that
#  - detekt-cli 1.23.8 + detekt1 jar and detekt-cli 2.x + detekt2 jar report exactly smoke/expected-findings.txt
#  - the detekt1 jar still runs on the oldest detekt-cli 1.x that 1.4.0 ran on
#  - the 1.4.0 release jar reports the same on detekt-cli 1.23.8 and 1.22.0 for the rules it has (golden behaviour;
#    skip with --no-golden). 1.4.0 is not on Maven Central and not committed here: set GOLDEN_JAR to a local copy of
#    it (checked against GOLDEN_JAR_SHA256); the golden runs are skipped when it is unset (e.g. on GitHub CI)
set -euo pipefail
cd "$(dirname "$0")/.."

DETEKT1_CLI=1.23.8
DETEKT1_OLDEST_CLI=1.22.0
DETEKT2_CLI=$(sed -n 's/^detekt2 *= *"\(.*\)"/\1/p' gradle/libs.versions.toml)
GOLDEN_JAR_VERSION=1.4.0
GOLDEN_JAR=${GOLDEN_JAR:-}
GOLDEN_JAR_SHA256=cc5963354a871d7f2af301a5ccfdb5dfa894ae41cf9c1b786cc9eb0a45c11505
# rules added after 1.4.0, absent from the golden jar
GOLDEN_MISSING_RULES='ComponentFunctionCall|MissingTypeDeclaration'
# findings that changed on purpose since 1.4.0 (behaviour fixes), as "File.kt:line:column RuleId"; left out of both
# sides of the golden comparison
GOLDEN_CHANGED='^$'
GOLDEN_CHANGED+='|^Payload\.kt:18:29 PayloadArgumentName ' # reported twice by 1.4.0 (nested onEach)
GOLDEN_CHANGED+='|^Database\.kt:25:30 BlockingSqlDelightCall ' # reported twice by 1.4.0 (nested suspend function)
GOLDEN_CHANGED+='|^Database\.kt:(30:24|35:8|36:5) BlockingSqlDelightCall ' # 1.4.0: local fun in withContext, Any member, ctor
GOLDEN_CHANGED+='|^UserWiring\.kt:18:3 RouteWiringMethodNaming ' # 1.4.0 skips methods after a nested class
GOLDEN_CHANGED+='|^Flows\.kt:15:(13|49) UseOnStartEmit ' # 1.4.0 reports only the outermost match of a chain
GOLDEN_CHANGED+='|^Surface\.kt:24:19 UseSurfaceModifier ' # 1.4.0: chain nested in a chain, composable nested in a non-composable function
MAVEN=https://repo1.maven.org/maven2
OUT=build/smoke
mkdir -p "$OUT"

fetch() { # <maven path> -> prints local file
  local file="$OUT/${1##*/}"
  [ -f "$file" ] || curl -sfL -o "$file" "$MAVEN/$1"
  echo "$file"
}

# checkstyle XML -> "File.kt:line:column RuleId message", sorted
normalize() {
  sed -n -e 's#.*<file name=".*/\([^/"]*\)".*#file \1#p' \
    -e 's/.*line="\([0-9]*\)" column="\([0-9]*\)" severity="[a-z]*" message="\([^"]*\)" source="detekt\.\([A-Za-z]*\)".*/\1:\2 \4 \3/p' "$1" |
    awk '/^file /{f=$2; next} {print f ":" $0}' | sort -t: -k1,1 -k2,2n -k3,3n
}

run() { # <name> <cli jar> <plugin jar> [extra cli args...]
  local name=$1 cli=$2 plugin=$3; shift 3
  # exit code 2 = findings reported, which is what we want here
  java -jar "$cli" --input smoke/sample --config smoke/all-rules.yml --disable-default-rulesets \
    --plugins "$plugin" --classpath "$stdlib" --jvm-target 11 --report "$@" > "$OUT/$name.log" 2>&1 || [ $? -eq 2 ] \
    || { cat "$OUT/$name.log"; exit 1; }
}

check() { # <name> <checkstyle xml> [expected findings file] [regex of findings to leave out]
  normalize "$2" | grep -vE "${4:-^$}" > "$OUT/$1.findings" || true
  if diff -u "${3:-smoke/expected-findings.txt}" "$OUT/$1.findings"; then echo "OK   $1"; else echo "FAIL $1"; failed=1; fi
}

./gradlew -q :detekt1:jar :detekt2:jar
stdlib=$(fetch org/jetbrains/kotlin/kotlin-stdlib/2.0.21/kotlin-stdlib-2.0.21.jar)
cli1=$(fetch "io/gitlab/arturbosch/detekt/detekt-cli/$DETEKT1_CLI/detekt-cli-$DETEKT1_CLI-all.jar")
cli1_oldest=$(fetch "io/gitlab/arturbosch/detekt/detekt-cli/$DETEKT1_OLDEST_CLI/detekt-cli-$DETEKT1_OLDEST_CLI-all.jar")
cli2=$(fetch "dev/detekt/detekt-cli/$DETEKT2_CLI/detekt-cli-$DETEKT2_CLI-all.jar")
failed=0

run detekt1 "$cli1" detekt1/build/libs/detekt1.jar "xml:$OUT/detekt1.xml"
check "detekt1 (detekt-cli $DETEKT1_CLI)" "$OUT/detekt1.xml"

run detekt1-oldest "$cli1_oldest" detekt1/build/libs/detekt1.jar "xml:$OUT/detekt1-oldest.xml"
check "detekt1 (detekt-cli $DETEKT1_OLDEST_CLI)" "$OUT/detekt1-oldest.xml"

run detekt2 "$cli2" detekt2/build/libs/detekt2.jar "checkstyle:$OUT/detekt2.xml" --analysis-mode full
check "detekt2 (detekt-cli $DETEKT2_CLI)" "$OUT/detekt2.xml"

if [ "${1:-}" != "--no-golden" ] && [ ! -f "$GOLDEN_JAR" ]; then
  echo "SKIP golden $GOLDEN_JAR_VERSION: $GOLDEN_JAR not found (set GOLDEN_JAR)"
elif [ "${1:-}" != "--no-golden" ]; then
  echo "$GOLDEN_JAR_SHA256  $GOLDEN_JAR" | shasum -a 256 -c --quiet
  grep -vE ":[0-9]+:[0-9]+ ($GOLDEN_MISSING_RULES) " smoke/expected-findings.txt | grep -vE "$GOLDEN_CHANGED" \
    > "$OUT/golden-expected.txt" || true
  run golden "$cli1" "$GOLDEN_JAR" "xml:$OUT/golden.xml"
  check "golden $GOLDEN_JAR_VERSION (detekt-cli $DETEKT1_CLI)" "$OUT/golden.xml" "$OUT/golden-expected.txt" "$GOLDEN_CHANGED"
  run golden-oldest "$cli1_oldest" "$GOLDEN_JAR" "xml:$OUT/golden-oldest.xml"
  check "golden $GOLDEN_JAR_VERSION (detekt-cli $DETEKT1_OLDEST_CLI)" "$OUT/golden-oldest.xml" "$OUT/golden-expected.txt" "$GOLDEN_CHANGED"
fi

exit $failed
