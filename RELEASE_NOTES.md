## Metro Player 2.3

**Install over your current version — no need to uninstall.** Every build is signed with the same key.

### New
- **Custom "note" GIF.** Settings → *GIF instead of the note* → pick your own GIF or image. It is shown in the lyrics during intros and instrumental breaks. One tap restores the default.
- **New cover animation.** Tap the cover on the Now Playing screen: the card flips around its vertical axis and the lyrics appear on the back. The lyrics now glide smoothly — the active line moves to the center while the neighbouring lines shrink and fade.

- **Optional buttons under the cover.** Settings → *buttons under the cover* hides shuffle / repeat / favorite on Now Playing (the "···" menu stays).

- **Default format.** Settings → *default format in lists* (e.g. `.flac`): lists open with that format already selected when it is present.
- **Better lyric animations.** Words light up one by one on the active line, lines scale with a soft spring and fade by distance on both the cover panel and the Lyrics tab.

### Fixed
- List rows no longer fly in again when you scroll back up.
- Tapping a lyric line now seeks to the right place when a sync offset is set (it ignored the offset before).
- The queue now shows the real play order when shuffle is on (it used to show the original order).
- GIFs share one image loader — less memory use and fewer stutters.
- Lyrics were sometimes fetched twice for the same track at the same time.
- New music appears in the library when you return to the app, no restart needed.
