# CallScope

CallScope is a local-first Android app for understanding patterns in your call history.

It summarizes your on-device call log into review queues, ranked contacts, and trend views so you can see who you talk with most, who you miss, and how your call activity changes over time.

## Features

* Review missed, screened, blocked, and reconnected calls
* Rank contacts by call time, call count, missed calls, and recency
* Explore call-time trends and incoming/outgoing balance
* Use built-in sample data until call-log access is granted

## Privacy

CallScope reads call log and contacts data only on your device. It has no cloud account, no remote sync, no analytics SDK, and no network permission.

The app requests:

* `READ_CALL_LOG` to analyze local call records
* `READ_CONTACTS` to show contact names for those records

If call-log access is not granted, CallScope shows sample data instead.

## Build

Build the debug APK:

```sh
./gradlew assembleDebug
```

Run tests:

```sh
./gradlew test
```

Build the release APK:

```sh
./gradlew assembleRelease
```

## License

CallScope is licensed under the GNU General Public License version 3. See [COPYING](COPYING).
