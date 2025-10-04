# Creating a new release

## Prepare

### Update version

1. Choose a semver-compatible version that reflects the unreleased changes (M.m.p).
2. Update the version in [release/version.txt].

### Update changelog

Put a new heading below the Unreleased heading:

```md
## [M.m.p] - yyyy-mm-dd
```

Below the new heading, add a link to the comparison of the last and current version tag.

```md
[M.m.p]: https://github.com/infolektuell/gradle-jextract/compare/vlM.lm.lp...vM.m.p
```

Update the link for the Unreleased heading to compare the unreleased commits with the current release.

```md
[unreleased]: https://github.com/infolektuell/gradle-jextract/compare/vM.m.p...HEAD
```

### Update release notes

1. Copy the content of the current release section from the changelog to [release/changes.md].
2. This will be displayed on Gradle plugin portal. Remove subheadings and try to shorten the notes to be as compact as possible.

## Commit and push release

Stage modified files:

```sh
git add .
```

Create the release commit:

```sh
git commit -m "Release vM.m.p"
```

Create a signed annotated tag:

```sh
git tag -sa vM.m.p
```

1. In [release/version.txt], change the version to `M.m.p-SNAPSHOT`.
2. `git add . && git commit -m "Postrelease vM.m.p-SNAPSHOT"`

Push the release commits and tag to GitHub. The plugin will be published for the new tag.

```sh
git push && git push --tags
```
