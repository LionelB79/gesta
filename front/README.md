# Gesta — front

Application React 19 + TypeScript (Vite), servie par le back-end Spring Boot en production.

## Commandes

```bash
npm ci          # installer les dépendances (versions exactes du package-lock.json)
npm run dev     # serveur de développement : http://localhost:5173, /api redirigé vers localhost:8080
npm run lint    # vérification oxlint
npm run build   # vérification TypeScript, puis build de production dans dist/
```

Le back-end doit tourner sur le port 8080 en mode développement (voir le README à la racine).
En production, `dist/` est copié dans le jar par le build Maven : aucune commande front n'est nécessaire.

## Organisation

- `src/app/` : router
- `src/shared/` : client HTTP, gestion des erreurs, authentification
- `src/features/` : un dossier par fonctionnalité (`auth/`, `bookings/`)

Règles : `features` peut importer `shared`, jamais l'inverse ; deux `features` ne s'importent pas entre elles.