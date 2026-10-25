# wed-rsvp

Quarkus wedding RSVP backend. Build and run its tests with JDK 25 and Maven:

```sh
mvn verify
```

Unit tests mirror the production class's module and package and use the name `<ClassName>Test`.
Integration tests use the suffix `IntegrationTest`, with at most one integration test class per
Java package. Both kinds run as part of `mvn verify`. The authentication boundary and control
packages use separate integration suites to cover enabled and disabled admin bootstrap configuration;
the invitation entity package has its own database integration suite.

Authentication uses [Quarkus SmallRye JWT](https://quarkus.io/version/3.40/guides/security-jwt/)
for signed bearer tokens and role enforcement. The frontend module is reserved for future work;
QR scanning and a login screen can use the API below.

## Authentication API

| Request | JSON body | Result |
| --- | --- | --- |
| `POST /api/auth/login/pin` | `{"invitationCode":"<code>","pin":"0123"}` | Access token for the invitation's user |
| `POST /api/auth/login/credentials` | `{"username":"<username>","password":"<password>"}` | Access token for the matching user |
| `GET /api/auth/me` | — | Current subject, roles, and linked invitation code |

Successful logins return `accessToken`, `tokenType` (`Bearer`), `expiresIn` (900 seconds),
and `roles`. Both login methods authenticate a stored `UserAccount` and copy that account's roles
into the token. The login method does not assign a role: username/password users can be guests,
and a PIN user retains any roles explicitly assigned to that account. Both overloads are named
`login` in the Java API. Send the token in the `Authorization: Bearer <accessToken>`
header. Token responses cannot be cached. Credentials belong in JSON bodies, never URLs.
Use a PIN string to preserve leading zeroes; exactly four ASCII digits are required.

All invitation operations require authentication:

- `GET /api/invitation/{invitation-code}` returns invitation details, replacing the old POST/PIN query API.
- `PUT /api/invitation/{invitation-code}/accept` accepts the invitation.
- `PUT /api/invitation/{invitation-code}/decline` declines it.

Guests can access only the invitation identified by both their token's invitation code and its
linked user account. Admins can access every invitation. Protected endpoints use
`@RolesAllowed({"guest", "admin"})`, with an additional ownership check for invitations.
An account without either role can authenticate but cannot use these endpoints.
Incorrect credentials, unknown accounts, and locked
accounts return the same `401` response. Invalid request data returns `400`; insufficient access returns `403`.

Five consecutive failed logins lock that invitation's PIN login or that user's password login for 15 minutes. Successful
login resets the counter. Counters persist in the database, and row locks serialize concurrent
attempts. Password failure counts are per user; an unknown username does not lock another account.

Tokens expire after 15 minutes, with five seconds of clock tolerance. There is no refresh token
or server-side logout/revocation list; clients log out by discarding their token. A password/PIN
change does not revoke an already issued token. Keep browser tokens in memory and use HTTPS.

## Admin and signing key configuration

The optional `ADMIN_USERNAME` and `ADMIN_PASSWORD_HASH` settings provision an initial user with the
`admin` role if that username does not already exist. Existing accounts retain their stored password
and roles across restarts, even if bootstrap configuration changes. Password login works for all
provisioned users, including guests, and does not require these settings after provisioning.
There is no default password or public registration endpoint. Supply a bcrypt hash, never a
plaintext password. For example, `htpasswd -nBC 12 admin` prompts for a password; the portion after
`admin:` is the hash. Supported hashes use bcrypt cost 12–16 and the `$2a$`, `$2b$`, or `$2y$` prefix.
Passwords are limited to 72 UTF-8 bytes to avoid bcrypt truncation.

Development and tests automatically receive an ephemeral RSA key pair from Quarkus. Production
requires externally managed RSA keys (at least 2048 bits). Generate a pair outside the repository:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out private-key.pem
openssl pkey -in private-key.pem -pubout -out public-key.pem
chmod 600 private-key.pem
```

Set these environment variables for the running application:

```sh
export ADMIN_USERNAME='your-admin-name'
export ADMIN_PASSWORD_HASH='<bcrypt-hash>'
export JWT_PRIVATE_KEY_LOCATION='/run/secrets/private-key.pem'
export JWT_PUBLIC_KEY_LOCATION='/run/secrets/public-key.pem'
```

Mount the key files at those paths; keep private keys out of source control. JWT verification
requires RS256, issuer `wed-rsvp`, and audience `wed-rsvp-api`. Use the same key pair across restarts;
replacing it invalidates existing tokens. Put the application behind HTTPS and apply request limits
to `/api/auth/*` at the reverse proxy to protect against floods across unknown invitation codes.

## Invitation provisioning and existing data

Invitation creation remains outside the HTTP API. Generate unpredictable invitation codes with
at least 128 random bits (for example, 24 `SecureRandom` bytes encoded as unpadded Base64 URL).
The code is a secret alongside the PIN, which has only 10,000 possibilities. Do not put the PIN
in the invitation URL or reuse invitation codes.

Create a `UserAccount`, explicitly assign its `roles` (for example `roles.add("guest")`), persist it,
and link it with `Invitation.account`. Each account has at most one invitation. PIN-only accounts
can omit `username` and `passwordHash`. Accounts that also log in with credentials need a unique
username and a bcrypt password hash. Roles are never accepted from login requests.

Set `Invitation.pinHash` to `BcryptUtil.bcryptHash("0123", 12)` when provisioning an invitation.
Only the hash is persisted; it maps to the existing database column named `pin`. The schema update
adds user accounts, their roles, an invitation/account association, and failed-attempt fields.

Existing plaintext PINs are deliberately rejected. Before using existing invitations, assign new
four-digit PINs and replace their `pin` column values with bcrypt hashes of cost 12–16. Link each
invitation to a provisioned user account with the intended roles; unlinked invitations cannot log in.
The old singleton admin login state is no longer used. There is no
automatic plaintext migration or six-digit PIN compatibility. Tests use an isolated in-memory H2
database and do not change the local application database.

## Future endpoints

Unannotated REST endpoints are denied by default. Protect admin endpoints with
`@RolesAllowed("admin")`. Invitation endpoints use `@RolesAllowed({"guest", "admin"})` together with
`@AllowedResponder` on the API interface methods. The latter binds a Jakarta REST request filter that adds ownership
enforcement to role authorization; it does not replace `@RolesAllowed`.
Boundary methods contain no explicit access calls. The service also verifies the invitation's
linked account ID inside its transaction to prevent access after an invitation code is reused.
Only intentionally public endpoints should use `@PermitAll`.

Declare `@AllowedResponder` only on the API interface methods; Jakarta REST inherits this name
binding for the implementing endpoint. Keep REST method annotations on the interface so overrides
do not suppress inherited metadata. The ownership filter is registered with the same annotation.

Role annotations are visible on the API and repeated on the concrete endpoints for Quarkus runtime
enforcement. The authentication API declares public logins with `@PermitAll` and the current-session endpoint with
`@RolesAllowed({"guest", "admin"})`.
The OpenAPI schema includes the bearer authentication scheme and login contracts.
