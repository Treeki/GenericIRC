GenericIRC
==========

Hi! This is my latest project, an IRC client for Android.

It's not the most complete. It kind of works. You can compile it if you figure
out how to set up the project (I still haven't quite managed to grok Android
project management...)

And I need a better name for it. Definitely.

There's a ton of stuff to do, and.. I've already managed to get bored of this,
for the most part. Go me! Sigh.

Pull requests welcome, if you can deal with my bad code :p

*Treeki, May 2013*

# Features

- Doesn't lag horribly!
- Smooth scrolling that does Android proud, thanks to ViewPager
- (hopefully those two will still be true once all the features are added...)
- The latest and greatest UI paradigm, Navigation Drawer, for buffer switching
- Handles multiple servers in a sane way
- Adheres to the Holo theme on ICS and newer
- Compatible with Gingerbread and up
- Supports Samsung multi-window (only tested on the phone version, not tablet)

# Todo

- Get a decent icon for this thing
- Handling of most of the IRC protocol
- CTCP
- Handle typed commands
- State tracking of active/inactive channel buffers
- Add unread count to pre-ICS drawer list item layout
- UI for joining channels
- UI for managing channels
- Close buffers
- Queries/PMs
- UI for channel users list
- Tablet-specific UI with persistent buffer list
- Reconnecting
- Notifications
- Prettier messages with colours
- Theme settings
- and other settings (global nick/username/realname, etc)

# Dependencies

- Android Support Library v4, v7
- ActionBarSherlock
- [BoD/android-switch-backport](https://github.com/BoD/android-switch-backport)
- [greenrobot/EventBus](https://github.com/greenrobot/EventBus)

