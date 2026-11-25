## Metro Player 2.6.1

**Install over your current version, no need to uninstall.** Every build is signed with the same key.

### Fixed in 2.6.1
- **Now Playing:** the lyrics line at the bottom no longer overlaps the buttons on smaller screens. The cover now takes the remaining space and the lyrics line has its own fixed area.
- **Fast scrolling:** long track lists scroll smoother, because each row now does much less work.
- **GIF instead of the note** is now also shown during the intro, long instrumental gaps and at the end of a song (not only on empty lyric lines), and on the "all tracks" tile.

### 2.6 · New
- **Equalizer.** Settings → *Equalizer*: five bands with sliders and twelve ready-made presets (Bass, Treble, V-shape, Rock, Pop, Hip-hop, Electronic, Jazz, Classical, Acoustic, Vocal, Flat). It works on all music in the app.

### 2.6 · Fixed
- The lyrics line under the player buttons no longer jumps or flickers: it has a fixed height and fades smoothly from line to line.
- The custom/default GIF is now shown instead of the "♪" note everywhere in the lyrics, including the neighbouring lines and the Lyrics tab.

### 2.4
> Exactly 25 days ago, on this very day, the first version of Metro Player was released. In those 25 days the app has grown into something that has given me an incredible experience.
>
> I am sincerely proud of my project and of the community.
>
> From today, the MetroPlayer source code is fully open. I hope this decision will be the start of new projects.

- **Two lyric animations to choose from.** Settings → *Lyrics* → *text animation*: the new one (lines glide to the center, words light up one by one) or the classic one (the block of lines slides in).

### Also new since 2.1
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
