from datetime import datetime

# List of timestamps of onQuitting messages
timestamps = [
    "2024-07-10 13:19:39.800",
    "2024-07-10 13:21:49.843",
    "2024-07-10 13:23:59.827",
    "2024-07-10 13:26:09.800",
    "2024-07-10 13:28:19.819",
    "2024-07-10 13:30:29.811",
    "2024-07-10 13:32:39.805",
    "2024-07-10 13:34:49.783"
]

# Convert timestamps to datetime objects
datetimes = [datetime.strptime(ts, "%Y-%m-%d %H:%M:%S.%f") for ts in timestamps]

# Calculate time differences in seconds
time_differences = [(datetimes[i] - datetimes[i - 1]).total_seconds() for i in range(1, len(datetimes))]

# Calculate average time difference in seconds
average_difference_seconds = sum(time_differences) / len(time_differences)

# Convert average time difference to minutes and seconds
average_minutes = int(average_difference_seconds // 60)
average_seconds = average_difference_seconds % 60

average_minutes, average_seconds
