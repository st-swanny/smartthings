/**
 *  Diskstation (Connect)
 *
 *  Copyright 2014 David Swanson
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
    name: "Diskstation (Connect)",
    namespace: "swanny",
    author: "swanny",
    description: "Allows you to connect the cameras from the Synology Surveilence Station",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name:"diskstationDiscovery", title:"Connect with your Diskstation!", content:"diskstationDiscovery")
	page(name:"cameraDiscovery", title:"Camera Setup", content:"cameraDiscovery", refreshTimeout:5)
    page(name:"motionSetup", title:"Motion Detection Triggers", content:"motionSetup", refreshTimeout:3)
}

mappings {
  path("/DSNotify") {
    action: [
      GET: "webNotifyCallback"
    ]
  }
}

//PAGES
/////////////////////////////////////

def motionSetup()
{   
   	// check for timeout error
    state.refreshCountMotion = state.refreshCountMotion+1
    if (state.refreshCountMotion > 10) {} 
    def interval = (state.refreshCountMotion < 4) ? 10 : 5
    
    if (!state.accessToken) {
    	createAccessToken() 
    }

	def url = apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}/DSNotify?user=user&password=pass&to=num&msg=Hello+World")
    
    if (state.motionTested == false) {    
        return dynamicPage(name:"motionSetup", title:"Motion Detection Triggers", nextPage:"", refreshInterval:interval, install: true, uninstall: true){        
            section("Overview") {
                paragraph "Motion detected by the cameras can be used as triggers to other ST devices. This step is not required " + 
                		  "to use the rest of the features of this SmartApp. Click 'Done' if you don't want to set up motion detection " + 
                          "or continue below to set it up."
            } 
            section("Diskstation Setup") {
                paragraph "Follow these steps to set up motion notifications from Surveillance Station. "
                paragraph "1. Log into your Diskstation and go to Surveillance Station"
                paragraph "2. Choose Notifications from the Menu"
                paragraph "3. Go to the SMS tab. Enable SMS notifications. Note that this setup will not actually send " + 
                          "SMS messages but instead overrides the SMS system to call a web link for this SmartApp."
                paragraph "4. Click 'Add SMS Service Provider'"
                paragraph "5. Copy the text entry field below and past into the SMS URL field"                
                input "ignore", "text", title:"Web address to copy:", defaultValue:"${url}"
                paragraph "6. Name your service provider something like 'Smartthings'" 
                paragraph "7. Click next"
                paragraph "8. In the drop downs, choose 'Username', 'Password', 'Phone Number' and 'Message Content' in order"
                paragraph "9. Press Finish"
                paragraph "10. Type 'user' for the Username, 'password' in both Password fields"
                paragraph "11. Type 123-4567890 for the first phone number"
                paragraph "12. Press 'Send a test SMS message' to update this screen"
                paragraph "13. Now click on the Settings tab in the Nofications window"
                paragraph "14. Go to the Camera section of this pane and then check SMS for Motion Detected"
                paragraph "15. With the Motion Detected row highlighted, choose Edit and then Edit Notification from the top left of this pane"
                paragraph "16. Put the following text into the Subject line and choose Save"
                input "ignore2", "text", title:"", defaultValue:"Camera %CAMERA% on %SS_PKG_NAME% has detected motion"
                paragraph "If the page does not say 'success' within 10-15 seconds after sending the test message, " +
                		  "go to the previous page and come back to refresh the screen again. If you still don't have " +
                          "the success message, retrace these steps."
            }
            section("Optional Settings", hidden: true, hideable: true) {
            	input "motionOffDelay", "number", title:"Minutes with no message before motion is deactivated:", defaultValue:1
            } 
        }
    } else {
    	return dynamicPage(name:"motionSetup", title:"Motion Detection Triggers", nextPage:"", install: true, uninstall: true){        
            section("Success!") {
            	paragraph "The test message was received from the DiskStation. " + 
                "Motion detected by the cameras can now be used as triggers to other ST devices."
            } 
        }
    }
}


def diskstationDiscovery()
{
    log.trace "subscribe to location"
    subscribe(location, null, locationHandler, [filterEvents:false])
    state.subscribe = true

    state.commandList = new LinkedList()
    
    // clear the refresh count for the next page
    state.refreshCount = 0
    state.motionTested = false
    // just default to get new info even though we have some logic later to see if IP has changed, this is more robust    
    state.getDSinfo = true		
    
    return dynamicPage(name:"diskstationDiscovery", title:"Connect with your Diskstation!", nextPage:"cameraDiscovery", uninstall: true){
        section("Please enter your local network DiskStation information:") {
            input "userip", "text", title:"ip address", defaultValue:"192.168.1.99"
            input "userport", "text", title:"http port", defaultValue:"5000"
        } 
        section("Please enter your DiskStation login information:") {
            input "username", "text", title:"username", defaultValue:""
            input "password", "password", title:"password", defaultValue:""
        }
    }
}

def cameraDiscovery()
{   
    if(!state.subscribe) {
        log.trace "subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }
    
    // see if we need to reprocess the DS info
    if ((userip != state.previoususerip) || (userport != state.previoususerport)) {
    	log.trace "force refresh of DS info"
    	state.previoususerip = userip
        state.previoususerport = userport
        state.getDSinfo = true
    }
    
    if ((state.getDSinfo == true) || state.getDSinfo == null) {
		getDSInfo()
    }
    
    //if (state.getCameraCapabilities) {    	
	//	getCameraCapabilities()
    //}

	// check for timeout error
    state.refreshCount = state.refreshCount+1
    if (state.refreshCount > 20) {state.error = "Network Timeout. Check your ip address and port. You must access a local IP address and a non-https port."} 

	state.refreshCountMotion = 0

    def options = camerasDiscovered() ?: []
    def numFound = options.size() ?: 0
    
    if (state.error == "")
    {
    	if (!state.SSCameraList || (state.commandList.size() > 0)) {
        	// we're waiting for the list to be created
            return dynamicPage(name:"cameraDiscovery", title:"Diskstation", nextPage:"", refreshInterval:4, uninstall: true) {
                section("Connecting to ${userip}:${userport}") {
                	paragraph "This can take a minute. Please wait..."
                }
            }
        } else {
        	// we have the list now
            return dynamicPage(name:"cameraDiscovery", title:"Camera Information", nextPage:"motionSetup", uninstall: true) {
                section("See the available cameras:") {
                    input "selectedCameras", "enum", required:false, title:"Select Cameras (${numFound} found)", multiple:true, options:options
                }
                section("") {
                    paragraph "Select the cameras that you want created as ST devices. Cameras will be remembered by the camera name in Surveillance Station. Please do not rename them in Surveillance Station or you may lose access to them in ST. Advanced users may change the ST DNI to the new camera name if needed."
                }
            }
        }
    }
    else
    {	
    	def error = state.error
        
        // clear the error
        state.error = ""
        
        // force us to reget the DS info
        state.previoususerip = "forcereset"
        clearDiskstationCommandQueue()
        
        // show the message
        return dynamicPage(name:"cameraDiscovery", title:"Connection Error", nextPage:"", uninstall: true) {
        	section() {
            	paragraph error
            }
        }    
    }
}

def getDSInfo() {	
    // clear camera list for now
    state.motionTested = false
    state.getDSinfo = false
    state.SSCameraList = null
    state.error = ""
    state.api = ["SYNO.API.Info":[path:"query.cgi",minVersion:1,maxVersion:1]]
    state.lastEventTime = null
    
    clearDiskstationCommandQueue() 

    // get APIs    
    queueDiskstationCommand("SYNO.API.Info", "Query", "query=SYNO.API.Auth", 1)
    queueDiskstationCommand("SYNO.API.Info", "Query", "query=SYNO.SurveillanceStation.Camera", 1)
    queueDiskstationCommand("SYNO.API.Info", "Query", "query=SYNO.SurveillanceStation.PTZ", 1)
    queueDiskstationCommand("SYNO.API.Info", "Query", "query=SYNO.SurveillanceStation.ExternalRecording", 1)

    // login
    executeLoginCommand()

    // get cameras
    queueDiskstationCommand("SYNO.SurveillanceStation.Camera", "List", "additional=device", 1)
}

def executeLoginCommand() {
	queueDiskstationCommand("SYNO.API.Auth", "Login", "account=${URLEncoder.encode(username, "UTF-8")}&passwd=${URLEncoder.encode(password, "UTF-8")}&session=SurveillanceStation&format=sid", 2)
}

def getCameraCapabilities() {
    state.getCameraCapabilities = false;
    state.cameraCapabilities = [:]
    state.cameraPresets = []
    state.cameraPatrols = []

    state.SSCameraList.each {
        updateCameraInfo(it)     	
    }
}

// takes in object from state.SSCameraList
def updateCameraInfo(camera) {
    def vendor = camera.additional.device.vendor.replaceAll(" ", "%20")
    def model = camera.additional.device.model.replaceAll(" ", "%20")
    if ((model == "Define") && (vendor = "User")) {
    	// user defined camera
        def capabilities = [:]
        
        capabilities.ptzPan = false
    	capabilities.ptzTilt = false
    	capabilities.ptzZoom = false
    	capabilities.ptzHome = false
    	capabilities.ptzPresetNumber = 0
        
        state.cameraCapabilities.put(makeCameraModelKey(vendor, model), capabilities)
    } else {
    	// standard camera
        //queueDiskstationCommand("SYNO.SurveillanceStation.Camera", "GetCapability", "vendor=${vendor}&model=${model}", 1)
        queueDiskstationCommand("SYNO.SurveillanceStation.Camera", "GetCapabilityByCamId", "cameraId=${camera.id}", 4)

        queueDiskstationCommand("SYNO.SurveillanceStation.PTZ", "ListPreset", "cameraId=${camera.id}", 1)    
        queueDiskstationCommand("SYNO.SurveillanceStation.PTZ", "ListPatrol", "cameraId=${camera.id}", 1)
    }
}

Map camerasDiscovered() {
	def map = [:]
	state.SSCameraList.each { 
		map[it.id] = it.name
	}
	map
}

def getUniqueCommand(String api, String Command) {
	return api + Command
}

def getUniqueCommand(Map commandData) {
	return getUniqueCommand(commandData.api, commandData.command)
}

def makeCameraModelKey(cameraInfo) {
    def vendor = cameraInfo.additional.device.vendor.replaceAll(" ", "%20")
    def model = cameraInfo.additional.device.model.replaceAll(" ", "%20")
	
    return makeCameraModelKey(vendor, model);
}

def makeCameraModelKey(vendor, model) {
	return (vendor + "_" + model)
}

/////////////////////////////////////

def finalizeChildCommand(commandInfo) {
	state.lastEventTime = commandInfo.time
}

def getFirstChildCommand(commandType) {
	def commandInfo = null
    
	// get event type to search for
    def searchType = null
	switch (commandType) {
        case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot"):
        	searchType = "takeImage"
        	break   
    }
    
    if (searchType != null) {
        def children = getChildDevices()
        def bestTime = now()
        def startTime = now() - 40000
        
        if (state.lastEventTime != null) {
        	if (startTime <= state.lastEventTime) {
        		startTime = state.lastEventTime+1
            }
        }
        
        //log.trace "startTime = ${startTime}, now = ${now()}"

        children.each {
            // get the events from the child
            def events = it.eventsSince(new Date(startTime))        
            def typedEvents = events.findAll { it.name == searchType }

			if (typedEvents) {
             	typedEvents.each { event ->
                    def eventTime = event.date.getTime()
                    //log.trace "eventTime = ${eventTime}"
                    if (eventTime >= startTime && eventTime < bestTime) {
                        // is it the oldest
                        commandInfo = [:]
                        commandInfo.child = it        
                        commandInfo.time = eventTime
                        bestTime = eventTime
                        //log.trace "bestTime = ${bestTime}"
                    }
                }
			}
        }
    }
    return commandInfo
}

/////////////////////////////////////

// return a getUniqueCommand() equivalent value
def determineCommandFromResponse(parsedEvent, bodyString, body) {
	if (parsedEvent.bucket && parsedEvent.key) {
    	return getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot")	
    }
    
    if (body) {
    	if (body.data) {
        	// has data
        	if (body.data.sid != null) { return getUniqueCommand("SYNO.API.Auth", "Login") }            
            if (bodyString.contains("maxVersion")) { return getUniqueCommand("SYNO.API.Info", "Query") }
            if (body.data.cameras != null) { return getUniqueCommand("SYNO.SurveillanceStation.Camera", "List") }
            //if (body.data.ptzPan != null) { return getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapability")}
            if (body.data.ptzPan != null) { return getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapabilityByCamId")}
            if ((body.data.total != null) && (body.data.offset != null)) 
            { 	// this hack is annoying, they return the same thing if there are zero presets or patrols
            	if ((state.commandList.size() > 0) 
                	&& (getUniqueCommand(state.commandList.first()) == getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPreset"))) {
                	return getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPreset") 
                }
                else {
            		return getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPatrol") 
                }
            }
    	}
    }
    
    return ""
}

def doesCommandReturnData(uniqueCommand) {
	switch (uniqueCommand) {
    	case getUniqueCommand("SYNO.API.Auth", "Login"):
        case getUniqueCommand("SYNO.API.Info", "Query"): 
        case getUniqueCommand("SYNO.SurveillanceStation.Camera", "List"): 
        case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapability"): 
        case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapabilityByCamId"):
        case getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPreset"): 
        case getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPatrol"): 
        case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot"):
        	return true
    }
    
    return false
}

// this process is overly complex handling async events from one IP
// would be much better synchronous and not having to track / guess where things are coming from
def locationHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]   
        
    if ((parsedEvent.ip == convertIPtoHex(userip)) && (parsedEvent.port == convertPortToHex(userport)))
    {
    	def bodyString = ""
        def body = null
                
        if (hub) { state.hub = hub }

        if (parsedEvent.headers && parsedEvent.body)
        { // DS RESPONSES
            def headerString = new String(parsedEvent.headers.decodeBase64())
            bodyString = new String(parsedEvent.body.decodeBase64())

            def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*/)[0] : null
            log.trace "DISKSTATION REPONSE TYPE: $type"
            if (type?.contains("text/plain")) 
            {
            	log.trace bodyString     
            	body = new groovy.json.JsonSlurper().parseText(bodyString)
            } else if (type?.contains("application/json")) {
            	log.trace bodyString
                body = new groovy.json.JsonSlurper().parseText(bodyString)
            } else if (type?.contains("text/html")) {
                log.trace bodyString
                body = new groovy.json.JsonSlurper().parseText(bodyString.replaceAll("\\<.*?\\>", ""))                
            } else {
                // unexpected data type
                log.trace "unexpected data type"
                if (state.commandList.size() > 0) {
                	Map commandData = state.commandList.first()
                	handleErrors(commandData, null)
                }
                return
            }
            if (body.error) {   
                if (state.commandList.size() > 0) {
                    Map commandData = state.commandList?.first()
                    // should we generate an error for this type or ignore
                    if ((getUniqueCommand(commandData) == getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPreset"))
                        || (getUniqueCommand(commandData) == getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPatrol")))
                    {
                        // ignore
                        body.data = null
                    } else {
                        // don't ignore
                        handleErrors(commandData, body.error)
                        return
                    }
                } else {
                    // error on a command we don't care about
                    handleErrorsIgnore(null, body.error)
                    return
                }
            }
   		}
        
        // gathered our incoming command data, see what we have        
        def commandType = determineCommandFromResponse(parsedEvent, bodyString, body)
   		
   		// check if this is a command for the master   
        if ((state.commandList.size() > 0) && (body != null) && (commandType != ""))
        {
            Map commandData = state.commandList.first()
            
            //log.trace "Logging command " + bodyString
            
            //log.trace "master waiting on " + getUniqueCommand(commandData)
            if (getUniqueCommand(commandData) == commandType) 
            {
            	// types match between incoming and what we wanted, handle it
                def finalizeCommand = true

                //try {
                    if (body.success == true)
                    {
                        switch (getUniqueCommand(commandData)) {
                            case getUniqueCommand("SYNO.API.Info", "Query"):
                            	def api = commandData.params.split("=")[1];
                            	state.api.put((api), body.data[api]);
                            	break
                            case getUniqueCommand("SYNO.API.Auth", "Login"):
                            	state.sid = body.data.sid
                            	break
                            case getUniqueCommand("SYNO.SurveillanceStation.Camera", "List"):
                            	state.SSCameraList = body.data.cameras
                            	state.getCameraCapabilities = true;
                                getCameraCapabilities()
                            	break
                            case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapability"):
                            	// vendor=TRENDNet&model=TV-IP751WC
                                def info = (commandData.params =~ /vendor=(.*)&model=(.*)/)
                                if ((info[0][1] != null) && (info[0][2] != null)) {
                                    state.cameraCapabilities.put(makeCameraModelKey(info[0][1], info[0][2]), body.data)
                                }
                            	break                             
                            case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapabilityByCamId"):
                            	def cameraId = (commandData.params =~ /cameraId=([0-9]+)/) ? (commandData.params =~ /cameraId=([0-9]+)/)[0][1] : null
                                if (cameraId) { 
                                	def camera = state.SSCameraList.find { it.id.toString() == cameraId.toString() }
                                    if (camera) {
                                        def vendor = camera.additional.device.vendor.replaceAll(" ", "%20")
                                        def model = camera.additional.device.model.replaceAll(" ", "%20")
                                        state.cameraCapabilities.put(makeCameraModelKey(vendor, model), body.data)
                                    } else {
                                    	log.trace "invalid camera id"
                                    }
                                }
                            	break
                            case getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPreset"):
                            	def cameraId = (commandData.params =~ /cameraId=([0-9]+)/) ? (commandData.params =~ /cameraId=([0-9]+)/)[0][1] : null
                            	if (cameraId) { state.cameraPresets[cameraId.toInteger()] = body.data?.presets }
                            	break
                            case getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPatrol"):
                            	def cameraId = (commandData.params =~ /cameraId=([0-9]+)/) ? (commandData.params =~ /cameraId=([0-9]+)/)[0][1] : null
                            	if (cameraId) { state.cameraPatrols[cameraId.toInteger()] = body.data?.patrols }
                            	break
                            default:
                                log.debug "received invalid command: " + state.lastcommand
                            	finalizeCommand = false
                            	break
                    	}
                    } else {
                        // success = false
						log.debug "success = false but how did we know what command it was?"
                    }

                    // finalize and send next command
                    if (finalizeCommand == true) {
                        finalizeDiskstationCommand()
                    }
                //}
                //catch (Exception err) {
                //	log.trace "parse exception: ${err}"
                //    handleErrors(commandData)
                //}
                // exit out, we've handled the message we wanted
				return
            }            
      	}
        // no master command waiting or not the one we wanted 
        // is this a child message?
        
        if (commandType != "") {
        	log.trace "event = ${description}"
        
            // see who wants this type (commandType)        
            def commandInfo = getFirstChildCommand(commandType)

            if (commandInfo != null) {
                switch (commandType) {
                    case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot"):
                        if (parsedEvent.bucket && parsedEvent.key){
                        	log.trace "saving image to device"
                            commandInfo?.child?.putImageInS3(parsedEvent)
                        }
                        return finalizeChildCommand(commandInfo)
                }
            }
        }
        
        // no one wants this type or unknown type
        if ((state.commandList.size() > 0) && (body != null))
        {
        	// we have master commands, maybe this is an error
            Map commandData = state.commandList.first()
            
            if (body.success == false) {  
            	def finalizeCommand = true
                
                switch (getUniqueCommand(commandData)) {
                    case getUniqueCommand("SYNO.API.Info", "Query"):
                    case getUniqueCommand("SYNO.API.Auth", "Login"):
                    case getUniqueCommand("SYNO.SurveillanceStation.Camera", "List"):
                    case getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetCapability"):
                    	handleErrors(commandData, null)
                    	break
                    case getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPreset"):
                    	def cameraId = (commandData.params =~ /cameraId=([0-9]+)/) ? (commandData.params =~ /cameraId=([0-9]+)/)[0][1] : null
                    	if (cameraId) { state.cameraPresets[cameraId.toInteger()] = null }
                    	break
                    case getUniqueCommand("SYNO.SurveillanceStation.PTZ", "ListPatrol"):
                    	def cameraId = (commandData.params =~ /cameraId=([0-9]+)/) ? (commandData.params =~ /cameraId=([0-9]+)/)[0][1] : null
                    	if (cameraId) { state.cameraPatrols[cameraId.toInteger()] = null }
                    	break
                    default:
                        log.debug "don't know now to handle this command " + state.lastcommand
                    	finalizeCommand = false
                    	break
             	}
                if (finalizeCommand == true) {
                 	finalizeDiskstationCommand()
                }
                return
          	} else {
            	// if we get here, we likely just had a success for a message we don't care about
            }
        }
       
        // is this an empty GetSnapshot error?
        if (parsedEvent.requestId && !parsedEvent.headers) {
        	def commandInfo = getFirstChildCommand(getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot"))
            if (commandInfo) {
                log.trace "take image command returned an error"                
                if ((state.lastErrorResend == null) || ((now() - state.lastErrorResend) > 15000)) {
                	log.trace "resending to get real error message"
                	state.lastErrorResend = now()
                    state.doSnapshotResend = true
                    sendDiskstationCommand(createCommandData("SYNO.SurveillanceStation.Camera", "GetSnapshot", "cameraId=${getDSCameraIDbyChild(commandInfo.child)}", 1))                    
                } else {
                	log.trace "not trying to resend again for more error info until later"
                }
                return 
            }
        }
        
        // why are we here?
        log.trace "Did not use " + bodyString
   	}
}

def handleErrors(commandData, errorData) {
	if (errorData) { 
    	log.trace "trying to handle error ${errorData}"
    }
    
	if (!state.SSCameraList) {
    	// error while starting up
        switch (getUniqueCommand(commandData)) {
            case getUniqueCommand("SYNO.API.Info", "Query"):
            state.error = "Network Error. Check your ip address and port. You must access a local IP address and a non-https port."
            break
                case getUniqueCommand("SYNO.API.Auth", "Login"):
            state.error = "Login Error. Login failed. Check your login credentials."
            break
                default:
                state.error = "Error communicating with the Diskstation. Please check your settings and network connection. API = " + commandData.api + " command = " + commandData.command
            break
      	}
    } else {
    	// error later on
        checkForRedoLogin(commandData, errorData)
    }	
}

def checkForRedoLogin(commandData, errorData) {
	if (errorData != null) {
        log.trace errorData
        if (errorData?.code == 102 || errorData?.code == 105) {
            log.trace "relogging in"
			executeLoginCommand()
        } else {
        	if (commandData) {
            	state.error = "Error communicating with the Diskstation. Please check your settings and network connection. API = " + commandData.api + " command = " + commandData.command
        	} else {
            	state.error = "Error communicating with the Diskstation. Please check your settings and network connection."
            }
        }
    }
}

def handleErrorsIgnore(commandData, errorData) {
	if (errorData) { 
    	log.trace "trying to handle error ${errorData}"
    }
    checkForRedoLogin(commandData, errorData)
}

private def parseEventMessage(Map event) {
	//handles attribute events
    log.trace "map event recd = " + event
	return event
}

private def parseEventMessage(String description) {
	def event = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('bucket:')) {
			part -= "bucket:"
			def valueString = part.trim()
			if (valueString) {
				event.bucket = valueString
			}
		}
        else if (part.startsWith('key:')) {
			part -= "key:"
			def valueString = part.trim()
			if (valueString) {
				event.key = valueString
			}
		}
        else if (part.startsWith('ip:')) {
			part -= "ip:"
			def valueString = part.trim()
			if (valueString) {
				event.ip = valueString
			}
		}
        else if (part.startsWith('port:')) {
			part -= "port:"
			def valueString = part.trim()
			if (valueString) {
				event.port = valueString
			}
		}
        else if (part.startsWith('headers')) {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString) {
				event.headers = valueString
			}
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			def valueString = part.trim()
			if (valueString) {
				event.body = valueString
			}
		}
        else if (part.startsWith('requestId')) {
			part -= "requestId:"
			def valueString = part.trim()
			if (valueString) {
				event.requestId = valueString
			}
		}
	}

	event
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    hex = hex.toUpperCase()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    hexport = hexport.toUpperCase()
    return hexport
}

private String getDeviceId(ip, port) {
    def hosthex = convertIPtoHex(ip)
    def porthex = convertPortToHex(port)
	return "$hosthex:$porthex"
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	unsubscribe()
	state.subscribe = false
    state.getDSinfo = true
    
    state.lastMotion = [:]
    
    if (selectedCameras) {
    	addCameras()
    }
    
    if(!state.subscribe) {
        log.trace "subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def createCameraDNI(camera) {
	return (camera.name)
}

def addCameras() {
	selectedCameras.each { cameraIndex ->
        def newCamera = state.SSCameraList.find { it.id.toString() == cameraIndex.toString() }
        log.trace "newCamera = " + newCamera
        if (newCamera != null) {
            def newCameraDNI = createCameraDNI(newCamera)
            log.trace "newCameraDNI = " + newCameraDNI
            def d = getChildDevice(newCameraDNI)
            if(!d) {
                d = addChildDevice("swanny", "Diskstation Camera", newCameraDNI, state.hub, [label:"Diskstation ${newCamera?.name}"]) //, completedSetup: true
                log.trace "created ${d.displayName} with id $newCameraDNI"

                // set up device capabilities here ??? TODO ???
                //d.setModel(newPlayer?.value.model)
            } else {
                log.trace "found ${d.displayName} with id $newCameraDNI already exists"
            }

            // set up even if already installed in case setup has changed
            d.initChild(state.cameraCapabilities[makeCameraModelKey(newCamera)])
        }
	}

}

def createDiskstationURL(Map commandData) {
    String apipath = state.api.get(commandData.api)?.path
    if (apipath != null) {

        // add session id for most events (not api query or login)
        def session = ""
        if (!( (getUniqueCommand("SYNO.API.Info", "Query") == getUniqueCommand(commandData)) 
              || (getUniqueCommand("SYNO.API.Auth", "Login") == getUniqueCommand(commandData)) ) ) {
            session = "&_sid=" + state.sid
        }
        
        if ((state.api.get(commandData.api)?.minVersion <= commandData.version) && (state.api.get(commandData.api)?.maxVersion >= commandData.version)) {
        	def url = "/webapi/${apipath}?api=${commandData.api}&method=${commandData.command}&version=${commandData.version}${session}&${commandData.params}"
            return url
       	} else {
        	log.trace "need a higher DS api version"
        }

    } else {
        // error!!!???
        log.trace "Unable to send to api " + commandData.api
        log.trace "Available APIs are " + state.api
    }
    return null
}

def createHubAction(Map commandData) {
    
    String deviceNetworkId = getDeviceId(userip, userport)
    String ip = userip + ":" + userport

    try {
        def url = createDiskstationURL(commandData)
        if (url != null) {
            def acceptType = "application/json, text/plain, text/html, */*"
            if (commandData.acceptType) {
                acceptType = commandData.acceptType
            }

            def hubaction = new physicalgraph.device.HubAction(
                """GET ${url} HTTP/1.1\r\nHOST: ${ip}\r\nAccept: ${acceptType}\r\n\r\n""", 
                physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
            if (getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot") == getUniqueCommand(commandData)) {
            	if (state.doSnapshotResend) {
                	state.doSnapshotResend = false
                } else {
                	hubaction.options = [outputMsgToS3:true]
                }
            }
            return hubaction   
        } else {
        	return null
        }
    }
    catch (Exception err) {
        log.debug "error sending message: " + err
    }
    return null
}

def sendDiskstationCommand(Map commandData) {
	def hubaction = createHubAction(commandData)
    if (hubaction) {
		sendHubCommand(hubaction)
    }
}

def createCommandData(String api, String command, String params, int version) {
    def commandData = [:]
    commandData.put('api', api)
    commandData.put('command', command)
    commandData.put('params', params)
    commandData.put('version', version)
    commandData.put('time', now())
    
    if (getUniqueCommand("SYNO.SurveillanceStation.Camera", "GetSnapshot") == getUniqueCommand(commandData)) {
		commandData.put('acceptType', "image/jpeg");
    }
    
    return commandData
}

def queueDiskstationCommand(String api, String command, String params, int version) {
    
    def commandData = createCommandData(api, command, params, version)
    
    if (doesCommandReturnData(getUniqueCommand(commandData))) {
    	// queue since we get data
        state.commandList.add(commandData)

        // list was empty, send now
        if (state.commandList.size() == 1) {
            sendDiskstationCommand(state.commandList.first())
        } else {
        	// something else waiting
            if ((now() - state.commandList.first().time) > 15000) {
            	log.trace "waiting command being cancelled = " + state.commandList.first()
                finalizeDiskstationCommand()
            }
        }
    } else {
    	sendDiskstationCommand(commandData)
    }
}

def finalizeDiskstationCommand() {
	//log.trace "removing " + state.commandList.first().command
    state.commandList.remove(0)
    
    // may need to handle some child stuff based on this command
    pollChildren()
    
    // send next command if list was full
    if (state.commandList.size() > 0) {
    	sendDiskstationCommand(state.commandList.first())
    }    
}

private def clearDiskstationCommandQueue() {
	state.commandList.clear()
}

def webNotifyCallback() {
	log.trace "motion callback"
    
    if (params?.msg?.contains("Test message from Synology")) {
    	state.motionTested = true
        log.debug "Test message received"
    }
    
    // Camera Foscam1 on DiskStation has detected motion
    def motionMatch = (params?.msg =~ /Camera (.*) on (.*) has detected motion/)
    if (motionMatch) {
        def thisCamera = state.SSCameraList.find { it.name.toString() == motionMatch[0][1].toString() }
        if (thisCamera) {
        	def cameraDNI = createCameraDNI(thisCamera)
            if (cameraDNI) {
                if ((state.lastMotion[cameraDNI] == null) || ((now() - state.lastMotion[cameraDNI]) > 1000)) {
                    state.lastMotion[cameraDNI] = now()
                    
                    def d = getChildDevice(cameraDNI)
                    if (d && d.currentValue("motion") == "inactive") {
                        log.trace "motion on child device: " + d
                        d.motionActivated()
                        if (d.currentValue("autoTake") == "on") {
                            log.trace "taking motion image for child"
                            d.take()
                        }
                        handleMotion()
                    }
                }
            }
        }
    }
}

def checkMotionDeactivate(child) {
	def timeRemaining = null
    def cameraDNI = child.deviceNetworkId
    
    try {
        def delay = (motionOffDelay) ? motionOffDelay : 5
        delay = delay * 60
        if (state.lastMotion[cameraDNI] != null) { 
            timeRemaining = delay - ((now() - state.lastMotion[cameraDNI])/1000) 
        }
    }
    catch (Exception err) {
    	timeRemaining = 0
    }
    
    log.debug "checkMotionDeactivate ${cameraDNI} timeRemaining = ${timeRemaining}"
    
    // we can end motion early to avoid unresponsiveness later
    if ((timeRemaining != null) && (timeRemaining < 15)) {
		child.motionDeactivate()
        state.lastMotion[cameraDNI] = null
        timeRemaining = null        
    	log.debug "checkMotionDeactivate ${cameraDNI} deactivated"
    }
    return timeRemaining
}

def handleMotion() {
	def children = getChildDevices()
    def nextTime = 1000000;
    log.debug "handleMotion"
    
    children.each {
    	def newTime = checkMotionDeactivate(it)
        if ((newTime != null) && (newTime < nextTime)) {
        	nextTime = newTime
        }
    }

	log.debug "handleMotion nextTime = ${nextTime}"
	if ((nextTime != 1000000)){
    	log.trace "nextTime = " + nextTime
        nextTime = (nextTime >= 25) ? nextTime : 25
		runIn((nextTime+5).toInteger(), "handleMotion")
    }
}

/////////CHILD DEVICE METHODS

def getDSCameraIDbyChild(childDevice) {
	return getDSCameraIDbyName(childDevice.device?.deviceNetworkId)
}

def getDSCameraIDbyName(String name) {    
    if (name) {
    	def thisCamera = state.SSCameraList.find { createCameraDNI(it).toString() == name.toString() }
		return thisCamera?.id
    } else {
    	return null   
    }
}

def getNumPresets(childDevice) {
	def childId = getDSCameraIDbyChild(childDevice)
    if ((childId != null) && (childId <= state.cameraPresets.size()) && state.cameraPresets[childId]) {
    	return state.cameraPresets[childId].size()
    }
	return 0
}


def getPresetId(childDevice, index) {
	def childId = getDSCameraIDbyChild(childDevice)
    if ((childId != null) && (childId <= state.cameraPresets.size())) {
    	if (index <= state.cameraPresets[childId]?.size()) {
        	return state.cameraPresets[childId][index-1]?.id
        }
    } 
    return null
}

def getPresetString(childDevice, index) {
	def childId = getDSCameraIDbyChild(childDevice)
    if ((childId != null) && (childId <= state.cameraPresets.size())) {
    	if ((index > 0) && (index <= state.cameraPresets[childId]?.size())) {
        	return state.cameraPresets[childId][index-1]?.name
        }
    } 
    return "N/A"
}

def getPresetIdByString(childDevice, name) {
	def childId = getDSCameraIDbyChild(childDevice)
    if (state.cameraPresets[childId] != null) { 
    	def preset = state.cameraPresets[childId].find { it.name.toString().equalsIgnoreCase(name.toString()) }
        return preset?.id
    }
    return null
}

def getNumPatrols(childDevice) {
	def childId = getDSCameraIDbyChild(childDevice)
    if ((childId != null) && (childId <= state.cameraPatrols.size()) && state.cameraPatrols[childId]) {
    	return state.cameraPatrols[childId].size()
    }
	return 0
}

def getPatrolId(childDevice, index) {
	def childId = getDSCameraIDbyChild(childDevice)
    if ((childId != null) && (childId <= state.cameraPatrols.size())) {
    	if (index <= state.cameraPatrols[childId]?.size()) {
        	return state.cameraPatrols[childId][index-1]?.id
        }
    } 
    return null
}

def getPatrolString(childDevice, index) {
	def childId = getDSCameraIDbyChild(childDevice)
    if ((childId != null) && (childId <= state.cameraPatrols.size())) {
    	if ((index > 0) && (index <= state.cameraPatrols[childId]?.size())) {
        	return state.cameraPatrols[childId][index-1]?.name
        }
    }   
    return "N/A"
}

def getPatrolIdByString(childDevice, name) {
	def childId = getDSCameraIDbyChild(childDevice)
    if (state.cameraPatrols[childId] != null) { 
    	def patrol = state.cameraPatrols[childId].find { it.name.toString().equalsIgnoreCase(name.toString()) }
        return patrol?.id
    }
    return null
}

import groovy.time.TimeCategory

def refreshCamera(childDevice) {
	// this is called by the child, let's just come back here in a second from the SmartApp instead so we have the right context
    //runIn(8, "startPolling")
    def timer = new Date()
		use(TimeCategory) {
    	timer = timer + 3.second
	}
    runOnce(timer, "startPolling")
}

def startPolling() {
    state.refreshIterations = 0
    pollChildren()
}

def pollChildren(){    
   	def children = getChildDevices() 
    
    children.each {
        //log.trace "refreshState = " + getRefreshState(it)
    
        // step 2 - check if they are waiting to be told of their refresh
        if (waitingRefresh(it) == true) {
        	// waiting on refresh 
            if  (state.commandList.size() == 0) {
            	def childObj = it;
                def thisCamera = state.SSCameraList.find { createCameraDNI(it).toString() == childObj.deviceNetworkId.toString() }
                if (thisCamera) {
                    it.doRefreshUpdate(state.cameraCapabilities[makeCameraModelKey(thisCamera)])
                }
            }
        }
        
    	// step 1 - check if they are wanting to start a refresh
        if (wantRefresh(it) == true) {
			// do child refresh
            def childObj = it;
            def thisCamera = state.SSCameraList.find { createCameraDNI(it).toString() == childObj.deviceNetworkId.toString() }
            if (thisCamera) {
                updateCameraInfo(thisCamera)
            }
        }       
    }
}

// parent checks this to say if we want a refresh
def wantRefresh(child) {
	def want = (child.currentState("refreshState")?.value == "want")
    if (want) {
    	child.doRefreshWait()	
    }
    return (want)
}

def getRefreshState(child) {
	return (child.currentState("refreshState")?.value)
}

def waitingRefresh(child) {
	return (child.currentState("refreshState")?.value == "waiting")
}
