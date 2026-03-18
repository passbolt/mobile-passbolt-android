# Required Backend Resources for Resource Tests

This document describes the resources that need to exist on the backend for the
resource instrumentation tests in `app/src/androidTest/.../scenarios/`.

All resources must be accessible by the test user with **owner permissions**.

---

## Quick Setup Checklist

Create the following resources on the backend for the test user.
Unless noted otherwise, every resource uses these field values:

| Field | Value |
|---|---|
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |

The test user also needs the ability to **create** and **delete** resources.

---

## Resources to Create

### 1. `Simple password - v4`

| Field | Value |
|---|---|
| Name | `Simple password - v4` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Description (metadata) | `This test description is unencrypted this time` |
| Shared with | At least 1 other user |

> **Note:** This resource type does NOT have a secret note field.

Used by: ResourcesDetailsTest, DeleteResourcePopupTest, SharedWithSubsectionTest

---

### 2. `Password with description - v4`

| Field | Value |
|---|---|
| Name | `Password with description - v4` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Note (secret) | `This is a Note which is secret` |
| Shared with | At least 1 other user |

> **Note:** This resource type does NOT have a metadata description field.

Used by: ResourcesDetailsTest, ResourcesNoteTest, DeleteResourcePopupTest, SharedWithSubsectionTest

---

### 3. `Password, Description and TOTP - v4`

| Field | Value |
|---|---|
| Name | `Password, Description and TOTP - v4` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Note (secret) | `This is a Note which is secret` |
| Shared with | At least 1 other user |

> **Note:** This resource type does NOT have a metadata description field.

Used by: ResourcesDetailsTest, ResourcesNoteTest, DeleteResourcePopupTest, SharedWithSubsectionTest

---

### 4. `Simple Password (Deprecated)`

| Field | Value |
|---|---|
| Name | `Simple Password (Deprecated)` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Shared with | At least 1 other user |

Used by: ResourcesDetailsTest, DeleteResourcePopupTest, SharedWithSubsectionTest

---

### 5. `Default resource type`

| Field | Value |
|---|---|
| Name | `Default resource type` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Shared with | At least 1 other user |

Used by: ResourcesDetailsTest, DeleteResourcePopupTest, SharedWithSubsectionTest

---

### 6. `Default resource with TOTP`

| Field | Value |
|---|---|
| Name | `Default resource with TOTP` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Shared with | At least 1 other user |

Used by: ResourcesDetailsTest, DeleteResourcePopupTest, SharedWithSubsectionTest

---

### 7. `Password with description and long note`

| Field | Value |
|---|---|
| Name | `Password with description and long note` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Note (secret) | Long text — see [Long Note Content](#long-note-content) below |

Used by: ResourcesNoteTest (currently disabled)

---

### 8. `Password, Description and TOTP with long note`

| Field | Value |
|---|---|
| Name | `Password, Description and TOTP with long note` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Note (secret) | Long text — see [Long Note Content](#long-note-content) below |

Used by: ResourcesNoteTest (currently disabled)

---

### 9. `Expired`

| Field | Value |
|---|---|
| Name | `Expired` |
| URI | `https://www.passbolt.com` |
| Username | `BettyAutomate` |
| Password | `TestPassword123!` |
| Expiry date | A date in the past (e.g. yesterday) |

> **Note:** This resource must have an expiry date set to a past date so it shows as expired.
> The server must have automatic expiry enabled (7 days).

Used by: UpdateExpiryTest

---

## Folders

### 1. `Shared without permission to add`

| Property | Value |
|---|---|
| Name | `Shared without permission to add` |
| Test user permission | **Read only** (no write/create) |
| Contents | At least 1 subfolder and 1 resource |

> **Note:** The test user must NOT have write permission on this folder.
> Another user should own the folder and share it with the test user as read-only.

Used by: FolderWithoutWritePermissionTest

## Long Note Content

Resources #7 and #8 must have the following text as their secret note:

> Free and open-source software (FOSS) is software available under a license that grants users the right to use, modify, and distribute the software -- modified or not -- to everyone. FOSS is an inclusive umbrella term encompassing free software and open-source software.[a][1] The rights guaranteed by FOSS originate from the "Four Essential Freedoms" of The Free Software Definition and the criteria of The Open Source Definition.[4][6] All FOSS can have publicly available source code, but not all source-available software is FOSS. FOSS is the opposite of proprietary software, which is licensed restrictively or has undisclosed source code.[4]
>
> The historical precursor to FOSS was the hobbyist and academic public domain software ecosystem of the 1960s to 1980s. Free and open-source operating systems such as Linux distributions and descendants of BSD are widely used, powering millions of servers, desktops, smartphones, and other devices.[9][10] Free-software licenses and open-source licenses have been adopted by many software packages. Reasons for using FOSS include decreased software costs, increased security against malware, stability, privacy, opportunities for educational usage, and giving users more control over their own hardware.
>
> The free software movement and the open-source software movement are online social movements behind widespread production, adoption and promotion of FOSS, with the former preferring to use the equivalent term free/libre and open-source software (FLOSS). FOSS is supported by a loosely associated movement of multiple organizations, foundations, communities and individuals who share basic philosophical perspectives and collaborate practically, but may diverge in detail questions.

---

## Test Details

### ResourcesDetailsTest (`details/`) — Active

Opens each resource and verifies that copying fields (URI, Username, Password,
Description, Note) to the clipboard produces the expected values.

| Resource | What is verified |
|---|---|
| `Simple password - v4` | Copy URI, Username, Password, Metadata Description |
| `Password with description - v4` | Copy URI, Username, Password, Note |
| `Password, Description and TOTP - v4` | Copy URI, Username, Password, Note |
| `Simple Password (Deprecated)` | Copy URI, Username, Password |
| `Default resource type` | Copy URI, Username, Password |
| `Default resource with TOTP` | Copy URI, Username, Password |

### ResourcesNoteTest (`details/note/`) — Disabled

Opens the resource detail view and checks the displayed note content matches
the expected value (short note or long FOSS excerpt).

| Resource | Expected note |
|---|---|
| `Password with description - v4` | `This is a Note which is secret` |
| `Password with description and long note` | FOSS Wikipedia excerpt (see [Long Note Content](#long-note-content)) |
| `Password, Description and TOTP - v4` | `This is a Note which is secret` |
| `Password, Description and TOTP with long note` | FOSS Wikipedia excerpt (see [Long Note Content](#long-note-content)) |

### DeleteResourcePopupTest (`deleteresourcepopup/`) — Active

Opens the action menu for each resource and verifies the delete confirmation
dialog appears and that cancel returns to the home screen.

Uses all 6 main resources (#1–#6).

### UpdateExpiryTest (`resourcesedition/updateexpiry/`) — Active

| Test method | Required resources |
|---|---|
| `updateExpiryOfAResourceWhenSecretHasChanged` | Creates resource at runtime; server must have automatic expiry (7 days) |
| `doNotUpdateExpiryOfAResourceWhenAllItemsExceptPasswordHasChanged` | `Expired` (#9) — must have past expiry date |

### SetExpiryTest (`resourcescreation/setexpiry/`) — Active

Creates a resource at runtime and verifies expiry is set to 7 days.
Server must have automatic expiry enabled (7 days).

### DeleteResourcesTest (`deleteresource/`) — Partially active

This test does **not** require a pre-existing resource. It creates a resource named
`To be deleted - Password with description <timestamp>` at runtime, then deletes it.
The test user just needs permission to create and delete resources.

| Test method | Status |
|---|---|
| `onThePasswordRemovalPopupICanClickTheDeleteButton` | Deprecated (active) — creates resource, then deletes it |
| `onThePasswordRemovalPopupICanDeleteTheResourceAndItDisappearsFromTheList` | Disabled (`@Ignore`) |
| `afterDeletionICanSeeConfirmationPopUpWhenV5ResourcesAreEnabled` | Disabled (`@Ignore`) |

### SharedWithSubsectionTest (`details/sharewithsubsection/`) — Disabled

Opens the resource detail view and verifies the "Shared with" section is visible.
Each resource must have at least one other user in its share list.

Uses all 6 main resources (#1–#6).