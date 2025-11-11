# copycrane

```bash
copycrane -i /home/user/source -o /mnt/backup
```

# Goals

## Always backwards compatible

Any stable API and functionality introduced in a new version of copycrane must be backwards compatible with previous versions. Deprecations must be accompanied by a migration guide which is completely optional.

We're not going to break your backups just because you're using a new version of _copycrane_. This means you can upgrade copycrane at any time. _copycrane_ will **never** update beyond version 1.X.

## Stable & Resilient

_copycrane_ is designed from the ground up to be resilient to failures. It will continue to work even if components of this program fail.

Components and features must always function independently of each other.

## Fast & Lightweight

Any backup operation must be as fast as possible. The more time it takes to perform a backup, the less likely it is that the backup will succeed.

All operations must be performed as efficiently as possible. The more resources they use, the less likely they are to succeed.

## Without unbundled dependencies

_copycrane_ must not require any external dependencies. All dependencies must be bundled with the program, as any external dependencies may be incompatible with this program.

First party external dependencies are also disallowed.