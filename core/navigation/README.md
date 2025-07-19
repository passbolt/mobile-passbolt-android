## Navigating between activities

Project migration to Jetpack Compose is in progress. No new activities should be created — use single activity principle.

Previously, the [Quadrant](https://github.com/gaelmarhic/Quadrant) plugin was used for navigation between existing activities. However, due to compatibility issues with newer Gradle versions (specifically task dependency configuration bugs), the plugin has been removed. <[Reported bug](https://github.com/gaelmarhic/Quadrant/issues/19#issuecomment-2211802556)>

The plugin-generated constants have been retained in the `constants` package for now. These will be fully removed once the migration is complete.

**Important**: If you rename or move any existing activity, make sure to update the corresponding entries in the `constants` package. Relevant notes have been added directly in each activity as reminders.
