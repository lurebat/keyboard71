
function convertDataToString(somedata){
   
   // u8 array
   var uarr = new Uint8Array(somedata);
    
   var CHUNK_SIZE = 0x8000; //arbitrary number
   var index = 0;
   var length = uarr.length;
   var result = '';
   var slice;
   while (index < length) {
      slice = uarr.subarray(index, Math.min(index + CHUNK_SIZE, length)); 
      result += String.fromCharCode.apply(null, slice);
      index += CHUNK_SIZE;
   }
   return result;
}

function JormRPC(uri, service_def) {
    if(!window.console){window.console={log:function(){}}}
    if(!window.WebSocket){throw "jormrpc error: WebSockets not available"}
    if(!window.JSON){throw "jormrpc error: JSON (de)serializer not available"}
    
    if(uri == "" || uri == null){
      uri = "ws://" + _server_address_and_port;
    }
    
    
    //make `this` available in closures
    var that = this;
    
    var existing = store.get("_jormrpcid");
    if(existing){
      this.generatedid = existing;
    } else {
      this.generatedid = randomHexes('xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
      store.set("_jormrpcid", this.generatedid);
    }
    
    //apply the service definition
    this.local = service_def['local'];
    this.onopen = service_def['onopen'];
    this.onclose = service_def['onclose'];
    
    this.onbecomeconnected = service_def['onbecomeconnected'];
    this.onbecomedisconnected = service_def['onbecomedisconnected'];
    
    this.currentlyconnected = false;
    this.connecting_or_connected = false;
    
    //set up instance variables
    this._CALLBACK_QUEUE_SIZE = 256;
    this._id = null;
    this._next_id = 1;
    this._callbacks = [];
    this.uri = uri;

    // Set up WebSocket and event handlers
    this.ws = new WebSocket(uri);
    this.ws.binaryType = 'arraybuffer';
    
    this.isConnected = function(){
      return that.currentlyconnected;
    }

    this.ws.onopen = function(){
        //console.log("WebSocket to "+uri+" - connection open.")
        if(that['onopen']){ that.onopen.apply(that); }
        that.msg("_jormrpcid", that.generatedid);
        that.markConnectedness(true);
    }
    
    this.markConnectingOrConnected = function(truth){
       that.connecting_or_connected = truth;
    }
    
    this.markConnectedness = function(truth){
      if(truth == that.currentlyconnected){
         return; // ignore
      }
      
      if(truth){
         that.currentlyconnected = true;
         if(that['onbecomeconnected'])
            that.onbecomeconnected.apply(that, []);
      } else {
         that.currentlyconnected = false;
         if(that['onbecomedisconnected'])
            that.onbecomedisconnected.apply(that, []);
      }
      
    }

    this.ws.onmessage = function(evt) {
        
        //var stringified = String.fromCharCode.apply(null, new Uint8Array(evt.data));
        var stringified = convertDataToString(evt.data);
        
        try {
           data = JSON.parse(stringified);
        } catch (whatever){
           console.log("syntax error while parsing message : " + stringified);
           return;
        }
        
        //console.log("WebSocket to "+uri+" - got data: ", evt.data);
        
        var protocolval = data[0];
        var callid = data[1];
        var callmsg = data[2];
        var callargs = data[3];
        
        if(protocolval == 2){
            // it's a method
            service_method = that.local[callmsg];
            
            if(service_method == undefined){
               that.respond(callid, "~nofunc:" + callmsg, ["no such func : " + callmsg]);
            } else {
               that._id = callid;
               
               var cb = function(themethod){
                  
                  var otherargs = Array.prototype.slice.call(arguments, 1);
                  if(typeof themethod == 'string'){
                  } else {
                     themethod = JSON.stringify(themethod);
                  }
               
                  that.respond(callid, themethod, otherargs);
                  
               };
               
               callargs.push(cb);
               ret = service_method.apply(that, callargs);
               that._id = null;
               
            }
            
        } else if (protocolval == 3){
            for(var i=0; i<that._callbacks.length; i++){
                if(that._callbacks[i][0] == callid){
                    var callback = that._callbacks.splice(i,1)[0][1]
                    
                    var params = [callmsg].concat(callargs);
                    
                    callback.apply(that, params); // no further callbacks for now. maybe later.
                    break;
                }
            }
        } else {
            console.log("WebSocket to "+uri+" - JORMRPC error.");
            that.close()
        }
    };

    this.ws.onerror = function(){
        console.log("WebSocket to "+uri+" - error.");
        if(that['onclose']){ 
           that.onclose.apply(that, error) 
        }
        
        that.markConnectedness(false);
        that.markConnectingOrConnected(false);
        
        setTimeout(function(){
           that.reconnect();
        }, 1000);
        
    }

    this.ws.onclose = function(){
        console.log("WebSocket to "+uri+" - connection closed.")
        if(that['onclose']){ 
          that.onclose.apply(that, undefined) 
        }
        
        that.markConnectedness(false);
        that.markConnectingOrConnected(false);
        
        setTimeout(function(){
           that.reconnect();
        }, 1000);
    }
    
    this.reconnect = function(){
      
       if(that.connecting_or_connected){
         // I guess you can ignore?
       } else {
      
          var newws = new WebSocket(that.uri);
          newws.binaryType = 'arraybuffer';
          that.markConnectingOrConnected(true);
          
          newws.onopen = that.ws.onopen;
          newws.onmessage = that.ws.onmessage;
          newws.onerror = that.ws.onerror;
          newws.onclose = that.ws.onclose;
          that.ws = newws;
          
          console.log("Reconnecting!");
       }
      
    }
    
    this.markConnectingOrConnected(true);
    
}


JormRPC.prototype = {
    invoke: function(lemethod){
        /// Send a invocation
        
        var params = Array.prototype.slice.call(arguments, 1);
        var callback = null;
        if(params.length > 0){
           var theback = params[params.length - 1];
           if(typeof(theback) == "function"){
              callback = theback;
              params.pop();
           }
        }
        
        if(typeof lemethod == 'string'){
           // cool
        } else {
           lemethod = JSON.stringify(lemethod);
        }
        
        //set id
        var id = null
        if(callback){ id = this._next_id; this._next_id += 1; }
        
        //save callback
        if(this._callbacks.length > this._CALLBACK_QUEUE_SIZE){
            this._callbacks.pop()
        }
        if(callback){
            this._callbacks = [[id, callback]].concat(this._callbacks)
        }

        //send message
        //message = JSON.stringify({id:id, method:method, params:params})
        message = JSON.stringify([2, id, lemethod, params])
        //console.log("WebSocket to "+this.uri+" - sending data: "+message)
        this.ws.send(message)
    },
    msg: function(){
      this.invoke.apply(this, arguments);
    },
    respond: function(id, result, params){
        /// Send a response
        //message = JSON.stringify({id:id, result:result, error:error})
        message = JSON.stringify([3, id, result, params])
        //console.log("WebSocket to "+this.uri+" - sending data: "+message)
        this.ws.send(message)
    },
    close: function(){
        /// Close the connection
        this.ws.close()
    },
    isConnected: function(){
      return this.currentlyconnected;
    }
}


    

