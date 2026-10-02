# Team workflow

This project uses a shared integration branch and short-lived feature branches. Do not work directly on `main`.

## Branch roles

- `main`: reviewed, stable production baseline.
- `develop`: integration branch for completed/reviewed features.
- `feature/<short-name>`: one developer's task branch, branched from `develop`.

Use pull requests into `develop`. Test the integration branch before creating a pull request from `develop` to `main` for a production release. Keep `main` protected in GitHub with pull-request review and status checks.

## Current repository setup

This repository has a `develop` integration branch and an `origin` Git remote. Check both before pushing so the team uses the configured repository. Never stage `.env` files, database exports, API keys, signing keys, or generated APKs.

```powershell
git remote -v
git branch --show-current
git status --short
```

If Git does not know your author identity, configure your own name and email locally. Review the exact staged file list before each commit. `.gitignore` excludes local `.env` files, but still verify that no secret or local config is staged.

## Repository hosting

The existing `origin` remote points to the team's GitHub repository. Confirm its URL with `git remote get-url origin`. A maintainer must grant each teammate repository access before they can clone or push.

```powershell
git remote get-url origin
```

Do not add another `origin` unless the current remote was intentionally removed.

## Developer workflow

Start each task from the latest integration branch:

```powershell
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/my-change
```

After making and checking changes on a feature branch:

```powershell
git status --short
git add -- path\to\reviewed-file another\reviewed-file
git diff --cached
git commit -m "Describe change"
git push -u origin feature/my-change
gh pr create --base develop --head feature/my-change --title "Describe change" --body "Summary and verification"
```

Reviewers check the change and its verification notes. Merge the pull request into `develop` only after review and checks pass. Delete the feature branch after merge.

## Release workflow

1. Test `develop` with the agreed verification commands.
2. Create a pull request from `develop` into `main`.
3. Review and merge the release pull request.
4. Build/deploy the backend and admin through their selected hosting providers; produce a mobile APK only after the real HTTPS API is configured.
5. Tag the released commit if the team uses version tags.

## How the second developer gets the project

After the owner pushes the initial `main` and `develop` branches, give the teammate repository access and the real GitHub URL. The teammate clones the repository, checks out `develop`, then creates a feature branch:

```powershell
$Remote = Read-Host "Paste the real GitHub repository URL"
git clone $Remote
$RepoFolder = Read-Host "Enter the repository folder created by git clone"
Set-Location $RepoFolder
git switch develop
git pull --ff-only origin develop
git switch -c feature/my-change
```

Each developer creates their own ignored local environment files from the checked-in `.env.example` templates, installs dependencies with `npm ci`, and uses a local or explicitly shared development MySQL/API. Never share production passwords in chat or commit them. When an admin adds a student, the form also creates that student's login using the entered email and initial password; parent and teacher accounts are created from the account directory.

For a fresh clone, the template copies are:

```powershell
Copy-Item backend\.env.example backend\.env
Copy-Item admin\.env.example admin\.env
Copy-Item mobile\.env.example mobile\.env
```

Each developer fills in their own local database password and random local JWT secret in `backend\.env`; these files remain ignored.
