**[v2.1.7] - 2025-03-21**  

**Added**  
- Optional support for PlaceholderAPI on both Bukkit and Velocity platforms. If the PlaceholderAPI plugin is present, external placeholders will be parsed alongside internal ones.  
- Improved internal placeholder handling to avoid conflicts with PlaceholderAPI syntax, ensuring consistent behavior whether or not PlaceholderAPI is installed.  

**Changed**  
- Updated logging messages to clearly indicate whether PlaceholderAPI was detected and whether internal or external placeholder systems are being used.
