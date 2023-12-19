# FilesMusic

FilesMusic is a simple file explorer and music player made on a whim originating from an android
mid-term project in university

> **To my m8s in uni**: Refer to beginner heading at the bottom

## Introducing

- file explorer directories navigation using fragment manager stack
    - RecyclerView with different view types (directory/file/audio file)
- viewpager2 tablayout
    - fragment-activity-fragment communication using fragment tag `f*` hack
- Glide v4 usage for smooth scrolling
    - custom ModelLoader using MediaMetadataRetriever to load album art from audio file
- weird directory-level album art selector
- kotlin coroutines or suspend or whatever they are called usage for IO bound works
- **spaghetti code**

### disclaimer

- Nothing here is best practice (probably aside from Bing/Bard/Chat GPT generated code or
  suspiciously beautiful code
  that follows best practice (probably from stackoverflow))

## Things I want to improve or TODO (not in any particular order)

- viewbinding & databinding instead of findViewById
  and `bruh.setOnClickListener{ onAbcXyzListener() }`
    - Or Jetpack Compose
- file explorer navigation without using fragment stack instead save history
- use navcontroller instead of fragment find by tag hack
- ViewModel or LiveData or whatever (idk)
- ExoPlayer instead of MediaPlayer
- Make it a Service or smth and allowing outside control (lock screen/notification/earphone click)
- better activity/fragment lifecycle handling
- use interfaces instead of passing callbacks around
- javadoc or kotlin-doc
- string resources everywhere
- logging
- android 13 (scoped storage stuff)
- yes, tests

## Development

_All contributions are appreciated_

## Installation

It runs on my machine

## License

- My code: ~~No license~~, most codebase is not **mine** anyway
    - `MIT may protect you from being sued if bug in your code is the cause of their rocket explosion on launch.`
        - https://www.reddit.com/r/github/comments/hvp4k6/comment/fyuxhis/
    - Thus, MIT
- Not my code: Their licenses then

## Unrelated

- gitignore file is from https://github.com/github/gitignore/blob/main/Android.gitignore

### Beginner (or To my m8s in uni)

- learn to use github pls fr fr
- how contribute:
    - fork
    - change code
    - push to fork
    - create pull request
