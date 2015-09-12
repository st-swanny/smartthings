/**
 *  Synology Diskstation Camera Control
 *
 *  Copyright 2014 swanny
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Synology Diskstation Camera Control",
    namespace: "swanny",
    author: "swanny",
    description: "Control the cameras based on presence",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Who?") {
        input "selectedSensors", "capability.presenceSensor", title: "Presense sensors?", multiple: true
	}
    section ("Cameras to adjust?") {
    	input "selectedCameras", "capability.imageCapture", title: "Cameras?", multiple: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	state.home = true
	subscribe(selectedSensors, "presence", presence)
}

def presence(evt)
{
	if (state.home == false) {    	
        if (evt.value == "present") {
        	selectedCameras.each { camera ->
            	log.trace "go home"
        		camera.home()
            }
            state.home = true
        }
    } else {
    	def allGone = true
    	selectedSensors.each { sensor ->
        	if (sensor.currentValue("presence") == "present") {
            	allGone = false
            }
        }
        
        log.trace "all gone = $allGone"
        
    	if (allGone == true) {
        	selectedCameras.each { camera ->
            	log.trace "go middle"
        		camera.presetGoName("middle")
            }
            state.home = false
        }
    }
}