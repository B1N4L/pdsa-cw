#!/usr/bin/env bash
# Netlify runs this before each build (see netlify.toml): exit 0 skips the build, exit 1 builds.
# Every deploy uses Netlify free-plan credits, so only the live site is built, and only when
# something the website uses has changed since the last build.

# no deploy previews or branch deploys
[ "${CONTEXT:-}" = "production" ] || exit 0

# first build, or a build without cache ("Clear cache and deploy"): always build
if [ -z "${CACHED_COMMIT_REF:-}" ] || [ "$CACHED_COMMIT_REF" = "${COMMIT_REF:-}" ]; then
    exit 1
fi

# otherwise build only if the website, its installers or this setup changed
git diff --quiet "$CACHED_COMMIT_REF" "$COMMIT_REF" -- website/ AMS/packaging/ netlify.toml
