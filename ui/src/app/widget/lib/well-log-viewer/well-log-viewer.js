/*
 * Copyright © 2017-2018 Hashmap, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import loadLogViewer from './logViewer';
//import loadLogViewer from 'WellLogViewer/logViewer';
//import welllogviwer from 'WellLogViewer'
//import * as welllogviwer from 'WellLogViewer'

/* eslint-disable angular/angularelement */
/* eslint-disable */
export default class WellLogViewer {
     constructor(ctx) {
     	this.cntr = 0;
     	this.ctx = ctx;
          //  var settings = ctx.settings;
            this.init();
     }
     init() {
		//loadLogViewer(this.ctx, "init");
        //alert(test);
        //alert("test.test");
     }

     test(){
     	var datasources = this.ctx.datasources;
        console.log(settings);
        var dataKeys = [];
        datasources.forEach(function(dkeys){
        	dkeys.dataKeys.forEach(function(datakey){

        		dataKeys.push({"value": datakey.label, "label": datakey.label});
        	})
        })
     	return dataKeys;
     }

     update() {
     	this.cntr +=1;
     	if(this.cntr < 2){
     		loadLogViewer(this.ctx, "init");
     	}     	
     	else{
     		loadLogViewer(this.ctx, "update");
     	}
     	//loadLogViewer();
     }

     static settingsSchema(ctx){
		return {
			"schema":{
			    "type": "object",
			    "properties": {
			      "Track": {
			        "type": "array",
			        "items": {
			            "title": "Settings",
			            "type": "object",
			            "properties":{
			            	"width": {
			             		 "title": "Track Width",
                                 "type": "string",
                          		 "enum": ["1", "2", "3", "4", "5"]
			             	},	
			                "component":{
			                    "title":"Component",
			                    "type": "array",
			                    "items":{ 
		                            "title": "fields",
			                        "type": "object",
			                        "properties": {
			                         	"cType": {
			                  	            "title": "Component Type",
			                                "type": "string",
			                          		"enum": ["line","linearGrid"]
			                      		},
			                      		"dataSource": {
											"title": "Data Source",
			                      			"type": "string"
			                      		},
			                      		"color": {
			                      			"title": "Color",
			                      			"type": "string"
			                      		},
			                      		"lineWeight": {
			                      			"title": "Line Width",
			                      			"type": "string"
			                      		},
			                      		"hasHeader": {
			                      			"title": "Has Header",
			                      			"type": "boolean"
			                      		},
			                      		"headerMin": {
			                      			"title": "Header minimum",
			                      			"type": "string"
			                      		},
			                      		"headerMax": {
			                      			"title": "Header max",
			                      			"type": "string"
			                      		},
			                      		"headerName": {
			                      			"title": "Header Name",
			                      			"type": "string"
			                      		},
			                      		"areaFill": {
			                      			"title": "Area Fill",
			                      			"type": "object",
			                      			"properties":{
			                      				"enable" : {
			                      					"title": "Enable Area Fill",
			                      					"type": "boolean"
			                      				},
			                      				"fill": {
			                      					"title": "Fill",
			                      					"type": "string",
			                      					"enum": ["none", "left", "right"]
			                      				}
			                      				,
			                      				"color": {
			                      					"title": "Fill Color",
			                      					"type": "string"
			                      				}
			                      				,
			                      				"opacity": {
			                      					"title": "Fill Opacity",
			                      					"type": "string"
			                      				}
			                      			}
			                      		},
			                      		"minorLines": {
			                      			"title": "Grid minor lines",
			                      			"type": "object",
			                      			"properties":{
			                      				"lines" : {
			                      					"title": "Number of lines",
			                      					"type": "string"
			                      				},
			                      				"lineWeight": {
			                      					"title": "Line weight",
			                      					"type": "string"
			                      				}
			                      				,
			                      				"style": {
			                      					"title": "Style",
			                      					"type": "string",
			                      					"enum": ["dashed", "solid"]
			                      				}	                      				
			                      			}
			                      		},
			                      		"majorLines": {
			                      			"title": "Grid major lines",
			                      			"type": "object",
			                      			"properties":{
			                      				"lines" : {
			                      					"title": "Number of lines",
			                      					"type": "string"
			                      				},
			                      				"lineWeight": {
			                      					"title": "Line weight",
			                      					"type": "string"
			                      				}		                      				
			                      			}
			                      		}
			                        }
			                 	}
			             	}		                 			               
			            }       	          
			        }
			      }
			    }
			},
		   "form":[{
	       		"key": "Track",
	      		"items": [
	      			"Track[].width",
	            	{
	                "key": "Track[].component",
	                "items": [
	               		"Track[].component[].cType",
	               		{
	               			"key":"Track[].component[].dataSource",
	               			"type": "rc-select",
		                    "multiple": false,
		                    "items": [],
		                    "condition": "model.Track[form.key[1]].component[form.arrayIndex].cType === 'line'"
	               		},             		
	               		 {
	                 		 "key": "Track[].component[].color",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].cType === 'line'"
	               		 },
	               		 {
	                 		 "key": "Track[].component[].lineWeight",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].cType === 'line'"
	               		 },
	               		 "Track[].component[].hasHeader",
	               		 {
	                 		 "key": "Track[].component[].headerMin",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].hasHeader === true"
	               		 },
	               		 {
	                 		 "key": "Track[].component[].headerMax",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].hasHeader === true"
	               		 },
	               		 {
	                 		 "key": "Track[].component[].headerName",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].hasHeader === true"
	               		 },
	               		 {
	                 		 "key": "Track[].component[].areaFill",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].cType === 'line'"
	               		 },
	               		 {
	                 		 "key": "Track[].component[].minorLines",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].cType === 'linearGrid'"
	               		 },
	               		 {
	                 		 "key": "Track[].component[].majorLines",
	                	 	 "condition": "model.Track[form.key[1]].component[form.arrayIndex].cType === 'linearGrid'"
	               		 }
	               		
	                ]
	            }
	      ]
	}]


		};
	}
}

/* eslint-enable angular/angularelement */
