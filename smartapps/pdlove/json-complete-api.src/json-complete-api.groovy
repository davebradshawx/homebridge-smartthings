/**
 *  JSON Complete API
 *
 *  Copyright 2016 Paul Lovelace
 *
 */
definition(
    name: "JSON Complete API",
    namespace: "pdlove",
    author: "Paul Lovelace",
    description: "API for JSON with complete set of devices",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Title") {
        input "deviceList", "capability.refresh", title: "All Devices", multiple: true, required: false
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
	if(!state.accessToken) {
         createAccessToken()
    }
}


def authError() {
    [error: "Permission denied"]
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "JSON API",
        platforms: [
            [
                platform: "SmartThings",
                name: "SmartThings",
                app_url: apiServerUrl("/api/smartapps/installations/${app.id}/"),
                access_token:  state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def authorizedDevices() {
    [
        deviceList: deviceList
    ]
}

def deviceCommand() {
  def device  = deviceList.find { it.id == params.id }
  def command = params.command
  if (!device) {
      httpError(404, "Device not found")
  } else {
      def value = request.JSON?.value
      if (value) {
        device."$command"(value)
      } else {
        device."$command"()
      }
  }
}

def deviceAttribute() {
  def device    = deviceList.find { it.id == params.id }
  def attribute = params.attribute
  if (!device) {
      httpError(404, "Device not found")
  } else {
      def currentValue = device.currentValue(attribute)
      [currentValue: currentValue]
  }
}

def deviceQuery() {
  def device    = deviceList.find { it.id == params.id }
  if (!device) {
      httpError(404, "Device not found")
  } else {
 	def deviceData = 
                [
                    name: device.displayName,
            		deviceid: device.id,
            		capabilities: deviceCapabilityList(device),
            		commands: deviceCommandList(device),
            		attributes: deviceAttributeList(device)
                ]       
    
    def deviceJson    = new groovy.json.JsonOutput().toJson(deviceData)
    render contentType: "application/json", data: deviceJson  
    }
}

def deviceCapabilityList(device) {
  def i=0
  device.capabilities.collectEntries { capability->
    [
      (i++):(capability.name)
    ]
  }
}

def deviceCommandList(device) {
  def i=0
  device.supportedCommands.collectEntries { command->
    [
      (i++): (command.name)
    ]
  }
}

def deviceAttributeList(device) {
  device.supportedAttributes.collectEntries { attribute->
    [
      (attribute.name): device.currentValue(attribute.name)
    ]
  }
}
        	
def renderDevices() {
    def deviceData = authorizedDevices().collectEntries { devices->
        [
            (devices.key): devices.value.collect { device->
                [
                    name: device.displayName,
            		deviceid: device.id,
            		capabilities: deviceCapabilityList(device),
            		commands: deviceCommandList(device),
            		attributes: deviceAttributeList(device)
                ]
            }
        ]
    }
    def deviceJson    = new groovy.json.JsonOutput().toJson(deviceData)
    render contentType: "application/json", data: deviceJson
}
mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/devices")                        { action: [GET: "authError"] }
        path("/config")                         { action: [GET: "authError"] }
        path("/location")                       { action: [GET: "authError"] }
        path("/:id/command/:command")     		{ action: [PUT: "authError"] }
        path("/:id/attribute/:attribute") 		{ action: [GET: "authError"] }
    } else {
        path("/devices")                        { action: [GET: "renderDevices"] }
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/location")                       { action: [GET: "renderLocation"] }
        path("/:id/command/:command")     		{ action: [PUT: "deviceCommand"] }
        path("/:id/query")						{ action: [GET: "deviceQuery"] }	
        path("/:id/attribute/:attribute") 		{ action: [GET: "deviceAttribute"] }
    }
}

