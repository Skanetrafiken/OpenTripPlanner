# Branches

We have two main branches:

- Our master branch `resesok-otp` that is used for synchronizing the community OTP code with our own infrastructure code. We don't apply hotfixes to the master branch, instead we want to fix these in the upstream OTP community repo.
- The release branch named after the current major version eg `resesok-3.x`, where hotfixes and configuration changes are applied.

# Sync with OTP community repo

When we want to fetch the upstream changes from OTP community we:

- Cherry pick any infrastructure changes from the current release branch into the `resesok-otp` branch.
- Merge the changes from the OTP master branch (currently named `dev-2.x`) into `resesok-otp`.
- From `resesok-otp`, create a new release branch called `resesok-{n+1}.x` (eg `resesok-4.x`).
- Go through the hotfixes from the last release branch and see if they have been fixed upstream or if we need to carry them over to the new release branch.
- Use the new release branch from now on.

# Release Policy

- Branch off from the current major release branch, eg `resesok-3.x`. Your release branch should be named `release/X.X`. For example `release/3.7`.
- Update `pom.xml` with new version and commit.
- Push branch.
- Run the `resesok-otp-post-merge` pipeline manually for your branch to publish and deploy to DEV.

![Release Flow](/.images/release_flow.png)
