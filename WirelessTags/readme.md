## Features

The Wireless Sensor Tags integration connects securely to your www.mytaglist.com account securely using OAuth2. Once attached, the SmartApp gets a list of the tags you have in your account. You can then choose which tags you want to create as SmartThings devices. The  SmartApp currently creates the devices as a Motion Device Type or a Water Device Type.

Motion Device Features:

* Temperature 
* Presence (out of range)
* Humidity (shows as zero on tags that don't support humidity)
* Movement (Acceleration) or Open/Close - you choose which mode inside the app
* Set Door Closed position
* Light on/off (using the device switch)
* Beep
* Refresh & Poll
* Signal Strength
* Battery Level

Water devices support (untested):

* Wet/Dry
* Humidity
* Temperature 
* Presence (out of range)
* Refresh & Poll
* Signal Strength
* Battery Level

Other tag types will be created as Motion Devices and should support whatever functionality they have in common with the motion tags. 

The majority of the tuning you can do on www.mytaglist.com will apply while you have the tags enabled as part of ST. Tuning options like responsiveness, sensitivity, and threshold angle may improve responsiveness or fuctionality in ST as well. Note that the ST integration overwrites many of the option in the "URL Calling..." dialog and arms/disarms the tags. 

## Installation

To set up the Wireless Sensor Tags integration, follow these steps:

Set up the SmartApp:
* Create a new SmartApp
* Enter any values in the required * fields
* Click the button "Enable OAuth in Smart App"
* Create the Smart App
* Copy the code from WirelessTagsConnect.groovy over the code of your SmartApp
* Save and Publish For Me

Set up the Device Types:
* Create a new Device Type
* Enter any values in the required * fields
* Create the Device Type
* Copy the code from WirelessTagsMotion.groovy over the code of your Device Type
* Save and Publish For Me
* Repeat these steps for WirelessTagsWater.groovy if you have a Water/Moisture Sensor

Connect to your Wireless Tags account:
* Open the SmartThings application on your iPhone or Android device
* Go to the Convenience apps section and choose Wireless Tags (Connect)
* If you don't see it in the Convenience section, kill the SmartThings application and restart it
* Follow the instructions in the App for the rest of the details

If you add new tags to your web account, go through the Wireless Tags (Connect) app again to add the new devices.

## Update Previous Installation
Update the SmartApp:
* Open your previously created Smart App
* Copy the code from WirelessTagsConnect.groovy over the code of your SmartApp
* Save and Publish For Me

Set up the Device Types:
* Open your previously created Device Type
* Copy the code from WirelessTagsMotion.groovy over the code of your Device Type
* Save and Publish For Me
* Repeat these steps for WirelessTagsWater.groovy if you have a Water/Moisture Sensor

Connect to your Wireless Tags account:
* Open the SmartThings application on your iPhone or Android device
* Go to the Convenience apps section and choose Wireless Tags (Connect)
* Run through the normal setup process even if you don't want to add any new tags. This will update your existing Devices and update the functionality of the SmartApp.