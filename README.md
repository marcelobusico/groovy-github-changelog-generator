# groovy-github-changelog-generator
Git &amp; Github Changelog Generator in Groovy

## Requirements
- Your favourite Linux Distro
- Git
- Groovy

## Usage

groovy ChangelogGenerator.groovy TAG_START TAG_END [--create-github-release]

## Sample Command

- Tested only in Ubuntu Linux.
- You need to set the environment variables: GITHUB_USERNAME and GITHUB_TOKEN and optionally GITHUB_API.
- Github repository is inferred using the first remote available of your git working copy.
- You must run the script under a Git working copy folder, in this case this is a clone of https://github.com/marcelobusico/changelog-testing-repo
- Optionally you can add at the end of the script the modifier --create-github-release to create a new release in Github using specified tag and generated changelog.
- If you get a 404 Not found error when creating a github release, probably you need to grant "repo" permission to your personal access token in Github settings.

groovy PATH_TO_GENERATOR_SCRIPT/ChangelogGenerator.groovy v1.0 v2.0.2 --create-github-release

## Sample Generated Github Release

https://github.com/marcelobusico/changelog-testing-repo/releases/tag/v2.0.2

# Sample CHANGELOG.md

## [v2.0.2](https://github.com/marcelobusico/changelog-testing-repo/tree/v2.0.2) (2016-06-23)
[Full Changelog](https://github.com/marcelobusico/changelog-testing-repo/compare/v1.0...v2.0.2)

**Bug fixes:**

- Some fix in rel 2.0 PR [\#9](https://github.com/marcelobusico/changelog-testing-repo/pull/9) ([marcelobusico](https://github.com/marcelobusico))
- Release 2.0 - Fix 1 - PR Title [\#7](https://github.com/marcelobusico/changelog-testing-repo/pull/7) ([marcelobusico](https://github.com/marcelobusico))

**Enhancements:**

- Feature C Pull Request Title [\#6](https://github.com/marcelobusico/changelog-testing-repo/pull/6) ([marcelobusico](https://github.com/marcelobusico))

**Merged Pull Requests:**

- Merge Back from Release to Master Branch Title [\#5](https://github.com/marcelobusico/changelog-testing-repo/pull/5) ([marcelobusico](https://github.com/marcelobusico))
