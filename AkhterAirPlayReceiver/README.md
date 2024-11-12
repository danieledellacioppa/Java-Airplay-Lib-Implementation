# AirplayAndroidReceiver
java airplay 安卓端实现，优化中，站在巨人的肩膀上

## Fixing attempts 

* 1. Port Availability and Binding: The port may not be immediately available due to system-level locking or other applications.
1. Race Conditions or Synchronization Issues: There may be timing issues where parts of the setup or shutdown process are not completing as expected.
2. Unsupported Platform APIs or Hidden Method Access: Logs indicate attempts to access unsupported system APIs (sun.misc.VM.maxDirectMemory and io.netty.channel.epoll.Native.offsetofEpollData) that may interfere with stability.
