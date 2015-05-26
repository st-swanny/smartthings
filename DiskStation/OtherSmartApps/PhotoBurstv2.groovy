/**
 *  Photo Burst When...
 *
 *  Author: SmartThings, modified by Dave Swanson
 *
 *  Date: 2015-05-25
 */

definition(
    name: "Photo Burst When v2...",
    namespace: "swanny",
    author: "swanny",
    description: "Take a burst of photos and send a push notification when...",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png"
)

preferences {
	section("Choose one or more, when..."){
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
	}
	section("Take a burst of pictures") {
		input "camera", "capability.imageCapture"
        input "presetName", "string", title: "Optional preset to go to", required: false
        input "presetDelay", "number", title: "Seconds until preset is done", defaultValue:0
		input "burstCount", "number", title: "How many? (default 5)", defaultValue:5
	}
	section("Then send this message in a push notification"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("And as text message to this number (optional)"){
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone Number", required: false
        }
	}

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(contact, "contact.open", sendMessage)
	subscribe(acceleration, "acceleration.active", sendMessage)
	subscribe(motion, "motion.active", sendMessage)
	subscribe(mySwitch, "switch.on", sendMessage)
	subscribe(arrivalPresence, "presence.present", sendMessage)
	subscribe(departurePresence, "presence.not present", sendMessage)
}

def sendMessage(evt) {
	log.debug "$evt.name: $evt.value, $messageText"

	if ((presetName != null) && (presetName != "")) { 
    	log.trace "go to " + presetName
		camera.presetGoName(presetName)
    }

	def takeDelay
	(1..((burstCount ?: 5))).each {
    	takeDelay = (((presetDelay ?: 0)*1000) + (500 * it) - 450)
        log.trace "delay = " + takeDelay
		camera.take(delay: takeDelay) // using 450 so there is always a tiny delay
	}

    if (location.contactBookEnabled) {
        sendNotificationToContacts(messageText, recipients)
    }
    else {
    	if (messageText) {
        	sendPush(messageText)
        }
        if (phone) {
            sendSms(phone, messageText)
        }
    }
}