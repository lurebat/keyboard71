(function(){
 
  // Prevent multiple applications
  if(window.getTimeouts !== undefined) return;
  
  // Only apply settimeout workaround for iOS 6 - for all others, we map to native Timers
  if (!UA.isIOS() || !navigator.userAgent.match(/OS 6(_\d)+/i)){

     window.delayfor = function(wait, callback){
       return setTimeout(callback, wait);
     };
     
     window.canceldelay = function(uid){
       return clearTimeout(uid);
     };
     
     return;
  }
 
  var TIMERID = 'rafTimer',
  
      touchTimeouts   = {},
      touchIntervals  = {},
      
      /* Reference to original timers */
      _st = window.setTimeout, 
      _si = window.setInterval, 
      _ct = window.clearTimeout, 
      _ci = window.clearInterval,
      
      /* Request animation timers */
      _clearTouchTimer = function(uid, isInterval){
        var interval = isInterval || false,
            timer = interval ? touchIntervals :  touchTimeouts;
        if(timer[uid]) {
          timer[uid].callback = undefined;
          timer[uid].loop = false;
          return true;
        } else {
          return false;
        }
      },
      _touchTimer = function(callback, wait, isInterval){
        var uid,
            name = callback.name || TIMERID + Math.floor(Math.random() * 1000),
            delta = new Date().getTime()+ wait,
            interval = isInterval || false,
            timer = interval ? touchIntervals :  touchTimeouts;
    
        uid = name + "" + delta;
    
        timer[uid] = {};
        timer[uid].loop = true;
        timer[uid].callback = callback;
    
        function _loop() {
          var now = new Date().getTime();
          if (timer[uid].loop !== false) {
              timer[uid].requestededFrame = webkitRequestAnimationFrame(_loop);
              timer[uid].loop = now <= delta;
          } else {
            if(timer[uid].callback) timer[uid].callback();
            if(interval){
              delta = new Date().getTime() + wait;
              timer[uid].loop = now <= delta;
              timer[uid].requestedFrame = webkitRequestAnimationFrame(_loop);
            } else {
              delete timer[uid];
            }
          }
        };
        
        _loop();
        return uid;
      },
      _timer = function(callback, wait, touch, isInterval){
        if(touch){
          return _touchTimer(callback, wait, isInterval);
        } else {
          return isInterval ? _si(callback, wait) : _st(callback, wait);
        }
      },
      _clear = function(uid, isInterval){
        if(uid.indexOf && uid.indexOf(TIMERID) > -1){
          return _clearTouchTimer(uid, isInterval);
        } else {
          return isInterval ? _ci(uid) : _ct(uid);
        }
      };
  
  /* Returns raf-based timers; For debugging purposes */
  window.getTimeouts = function(){
    return { timeouts: touchTimeouts , intervals : touchIntervals }
  };
 
  /* Exposed globally */
  window.setTimeout = function(callback, wait, touch){
    return _timer(callback, wait, touch);
  };
  
  window.delayfor = function(wait, callback){
    return _timer(callback, wait, true);
  };
  
  window.setInterval = function(callback, wait, touch){
    return _timer(callback, wait, touch, true);
  };
  window.clearTimeout = function(uid){
    return _clear(uid);
  };
  window.canceldelay = function(uid){
    return _clear(uid);
  };
  window.clearInterval = function(uid){
    return _clear(uid, true);
  };
})();
