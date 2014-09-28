/**
 *  Wireless Tag Water
 *
 *  Copyright 2014 Dave Swanson (swanny)
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
metadata {
	definition (name: "Wireless Tag Water", namespace: "swanny", author: "swanny") {
		capability "Water Sensor"
        capability "Presence Sensor"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Signal Strength"
		capability "Battery"
        capability "Refresh"
        capability "Polling"
        
        command "generateEvent"
        command "initialSetup"
        
        attribute "tagType","string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		valueTile("temperature", "device.temperature") {
			state("temperature", label:'${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}
        valueTile("humidity", "device.humidity", inactiveLabel: false) {
			state "humidity", label:'${currentValue}% humidity', unit:""
		}
		valueTile("rssi", "device.rssi", inactiveLabel: false, decoration: "flat") {
			state "rssi", label:'${currentValue}% signal', unit:""
		}
        standardTile("presence", "device.presence", canChangeBackground: true) {
			state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#53a7c0"
			state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
			state "battery", label:'${currentValue}% battery', unit:"V"
		}
		standardTile("refresh", "device.temperature", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("type", "device.tagType", decoration: "flat") {
			state "default", label:'${currentValue}'
		}
       	standardTile("water", "device.water") {
			state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#53a7c0"
		}
		main(["water", "temperature", "presence", "humidity"])
		details(["water", "temperature", "humidity", "presence", "refresh", "type", "rssi", "battery"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

void poll() {
	log.debug "poll"	
    parent.pollChild(this)
}

def refresh() {
	log.debug "refresh"
    parent.refreshChild(this)
}

def initialSetup() {

}

def updated() {
	log.trace "updated"
}

void generateEvent(Map results)
{
	log.debug "parsing data $results"
    
   	if(results)
	{
		results.each { name, value ->
            def isDisplayed = true
            
            if (name=="temperature") {
            	def tempValue = getTemperature(value)
            	def isChange = isTemperatureStateChange(device, name, tempValue.toString())
            	isDisplayed = isChange
                
				sendEvent(name: name, value: tempValue, unit: getTemperatureScale(), displayed: isDisplayed)                                  									 
            }
            else {
            	def isChange = isStateChange(device, name, value.toString())
                isDisplayed = isChange

             	sendEvent(name: name, value: value, isStateChange: isChange, displayed: isDisplayed)       
            }
		}
	}
}

def getTemperature(value) {
	def celsius = value
	if(getTemperatureScale() == "C"){
		return celsius
	} else {
		return celsiusToFahrenheit(celsius) as Integer
	}
}

