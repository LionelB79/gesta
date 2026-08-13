#!/usr/bin/env bash
# ---------------------------------------------------------------
#  bash dump.sh
# ---------------------------------------------------------------
set -euo pipefail

OUTPUT="${1:-projet_complet.txt}"
MAX_SIZE_KB=300   # on saute les fichiers plus gros (dumps SQL, mocks, etc.)

# Extensions embarquées (back + front + conf)
EXT_REGEX='\.(java|kt|xml|yml|yaml|properties|sql|ts|tsx|js|jsx|mjs|cjs|css|scss|html|json|md|py|sh|feature)$|(^|/)(Dockerfile|Makefile|\.gitlab-ci\.yml)$'

# Dossiers / fichiers toujours exclus
EXCLUDE_REGEX='(^|/)(\.git|node_modules|target|build|dist|out|coverage|\.next|\.idea|\.vscode|\.mvn|__pycache__|\.venv|venv)/|(package-lock\.json|yarn\.lock|pnpm-lock\.yaml|\.env.*)$'

# --- Récupération de la liste des fichiers -----------------------
# En dépôt git : trackés + non trackés, en respectant le .gitignore
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  RAW_LIST=$(git ls-files --cached --others --exclude-standard)
  SOURCE="git ls-files -co (.gitignore respecté)"
else
  RAW_LIST=$(find . -type f | sed 's|^\./||')
  SOURCE="find"
fi

FILES=$(printf '%s\n' "$RAW_LIST" \
  | grep -Ev "$EXCLUDE_REGEX" \
  | grep -E  "$EXT_REGEX" \
  | grep -v  "^${OUTPUT}$" \
  | grep -v  "^$(basename "$0")$" \
  | sort || true)

if [ -z "$FILES" ]; then
  echo "Aucun fichier trouvé. Vérifie que tu es bien à la racine du projet." >&2
  exit 1
fi

# --- Écriture ---------------------------------------------------
{
  echo "================================================================================"
  echo "PROJET : $(basename "$PWD")"
  echo "Généré le : $(date '+%d/%m/%Y %H:%M')  |  Source : $SOURCE"
  echo "================================================================================"
  echo
  echo "ARBORESCENCE"
  echo "--------------------------------------------------------------------------------"
  printf '%s\n' "$FILES"
  echo
} > "$OUTPUT"

COUNT=0
SKIPPED=0
while IFS= read -r f; do
  [ -f "$f" ] || continue

  # on saute le binaire
  if ! grep -Iq . "$f" 2>/dev/null; then
    SKIPPED=$((SKIPPED + 1)); continue
  fi

  size_kb=$(du -k "$f" | cut -f1)
  if [ "$size_kb" -gt "$MAX_SIZE_KB" ]; then
    {
      echo
      echo "================================================================================"
      echo "FICHIER : $f  [IGNORÉ — ${size_kb} Ko > ${MAX_SIZE_KB} Ko]"
      echo "================================================================================"
    } >> "$OUTPUT"
    SKIPPED=$((SKIPPED + 1)); continue
  fi

  {
    echo
    echo "================================================================================"
    echo "FICHIER : $f"
    echo "================================================================================"
    cat "$f"
    echo
  } >> "$OUTPUT"
  COUNT=$((COUNT + 1))
done <<< "$FILES"

# --- Bilan ------------------------------------------------------
CHARS=$(wc -c < "$OUTPUT" | tr -d ' ')
echo "OK -> $OUTPUT"
echo "   $COUNT fichiers inclus, $SKIPPED ignorés"
echo "   $(du -h "$OUTPUT" | cut -f1) — ~$((CHARS / 4)) tokens estimés"