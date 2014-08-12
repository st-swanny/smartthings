
## Features

Connect to Synology Diskstation / Surveillance Station on your local network and create camera devices for each camera in the Survellance Station application.

Camera devices support the following capabilities:
- Image Capture
   Image capture uses the standard take command
- Motion Detection
   Motion detection status lasts for a user specified number of minutes past the last motion sent by the Diskstation
- Record 
   Using the device button or switch on/off commands to start and stop recording 
- Refresh
   Use the refresh button / command when you change presets or patrols

The follow commands are also supported from SmartApps:
- "left", "right", "up", and "down"
- "zoomIn" and "zoomOut"
- "home"
- "presetGoName", ["string"]
- "patrolGoName", ["string"]
- "autoTakeOn" and "autoTakeOff"

## Installation

To set up the your Synology Diskstation cameras from Surveillance Station as SmartThings devices, follow these steps:

Set up the SmartApp:
1. Create a new SmartApp
2. Enter any values in the required * fields
3. Click the button "Enable OAuth in Smart App"
4. Create the Smart App
5. Copy the code from DiskstationConnect.groovy over the code of your SmartApp
6. Save and Publish For Me

Set up the Device Type:
1. Create a new Device Type
2. Enter any values in the required * fields
3. Create the Device Type
4. Copy the code from DiskstationCamera.groovy over the code of your Device Type
5. Save and Publish For Me

Connect to your DiskStation:
1. Open your SmartThings application on your iPhone or Android device
2. Go to My Apps and Choose Diskstation (Connect)
3. If you don't see it in My Apps, kill the SmartThings application and restart it
4. Follow the instructions in the App for the rest of the details

If you add new cameras to your system, go through the Diskstation (Connect) app again to add the new cameras. If you already set up motion detection, you do not need to do it again.

