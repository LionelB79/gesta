#!/bin/bash
{
  echo "=== ARBORESCENCE ==="
  find . -type f \( -name "*.java" -o -name "*.yaml" -o -name "*.yml" -o -name "pom.xml" \) \
    -not -path "*/target/*" -not -path "*/node_modules/*" | sort
  echo
  find . -type f \( -name "*.java" -o -name "*.yaml" -o -name "*.yml" -o -name "pom.xml" \) \
    -not -path "*/target/*" -not -path "*/node_modules/*" | sort | while read -r f; do
      echo "=== $f ==="
      cat "$f"
      echo
  done
} > contexte.txt
echo "contexte.txt généré"
