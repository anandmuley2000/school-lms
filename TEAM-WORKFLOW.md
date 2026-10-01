# Team workflow

This project uses a shared integration branch and short-lived feature branches. Do not work directly on `main`.

## Branch roles

- `main`: reviewed, stable production baseline.
- `develop`: integration branch for completed/reviewed features.
- `feature/<short-name>`: one developer's task branch, branched from `develop`.

Use pull requests into `develop`. Test the integration branch before creating a pull request from `develop` to `main` for a production release. Protect `main` in GitHub with pull-request review and status checks after the repository is created.

## Initial local repository setup

The repository is initialized locally on `main`; no remote is configured and nothing has been pushed. Before making the first commit, set your real author identity locally and inspect the staged file list. Never stage `.env` files, database exports, API keys, signing keys, or generated APKs.

```powershell
cd "D:\student app"
git config --local user.name "Your Name"
git config --local user.email "you@example.com"
git status --short --ignored
git add -A
git status --short
git commit -m "Prepare existing School LMS project"
git switch -c develop
```

The name and email above are examples for commands only; replace them with the contributor's Git author identity. Confirm no secret or local config is staged before committing. `.gitignore` already excludes local `.env` files.

## Create the GitHub repository (when ready)

No GitHub account or remote is connected by this preparation. After creating an empty private repository in GitHub, use its real URL:

```powershell
$Remote = Read-Host "Paste the real GitHub repository URL"
git remote add origin $Remote
git push -u origin main
git push -u origin develop
```

Alternatively, if GitHub CLI is installed and authenticated, create the private repository without pushing automatically, then use the explicit pushes above:

```powershell
$Repo = Read-Host "Enter GitHub owner/repository"
gh repo create $Repo --private --source . --remote origin
git push -u origin main
git push -u origin develop
```

This guide does not run those commands or contact GitHub.

## Developer workflow

Start each task from the latest integration branch:

```powershell
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/my-change
```

After making and checking changes:

```powershell
git status --short
git add -A
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

Each developer creates their own ignored local environment files from the checked-in `.env.example` templates, installs dependencies with `npm ci`, and uses the shared development MySQL/API only if the team has intentionally configured access. Never share production passwords in chat or commit them.

For a fresh clone, the template copies are:

```powershell
Copy-Item backend\.env.example backend\.env
Copy-Item admin\.env.example admin\.env
Copy-Item mobile\.env.example mobile\.env
```

Each developer fills in their own local database password and random local JWT secret in `backend\.env`; these files remain ignored.
