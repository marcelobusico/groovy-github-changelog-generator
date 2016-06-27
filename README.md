# groovy-github-changelog-generator
Git &amp; Github Changelog Generator in Groovy

# Requirements
- Your favourite Linux Distro
- Git
- Groovy

# Usage

groovy changelog-generator.groovy GITHUB_REPO_URL TAG_START TAG_END

# Sample Command

- Tested only in Ubuntu Linux.
- You need to set the environment variables: GITHUB_USERNAME and GITHUB_TOKEN.
- You must run the script under a Git working copy folder, in this case this is a clone of https://github.com/marcelobusico/changelog-testing-repo

groovy changelog-generator.groovy https://api.github.com/repos/marcelobusico/changelog-testing-repo v1.0 v2.0.2

# Sample CHANGELOG.md

## Version v2.0.2:

**Merged Pull Requests:**

- Some fix in rel 2.0 PR [\#9](https://github.com/marcelobusico/changelog-testing-repo/pull/9) ([marcelobusico](https://github.com/marcelobusico))
- Release 2.0 - Fix 1 - PR Title [\#7](https://github.com/marcelobusico/changelog-testing-repo/pull/7) ([marcelobusico](https://github.com/marcelobusico))
- Feature C Pull Request Title [\#6](https://github.com/marcelobusico/changelog-testing-repo/pull/6) ([marcelobusico](https://github.com/marcelobusico))
- Merge Back from Release to Master Branch Title [\#5](https://github.com/marcelobusico/changelog-testing-repo/pull/5) ([marcelobusico](https://github.com/marcelobusico))
