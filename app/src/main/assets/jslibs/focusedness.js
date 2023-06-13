(function() {

    var callbacks = [];

    window.FOC = {
       onChange: function(lecallback){
          callbacks.push(lecallback);
       }
    }
   
    var hidden = "hidden";
    
    var onchange = function (evt) {
        var v = true, h = false,
            evtMap = { 
                focus:v, focusin:v, pageshow:v, blur:h, focusout:h, pagehide:h 
            };

        evt = evt || window.event;
        
        var finalvis = false;
        
        if (evt.type in evtMap)
            finalvis = evtMap[evt.type];
        else        
            finalvis = this[hidden] ? false : true;
        
        console.log("On change!" + finalvis);
        
        callbacks.forEach(function(item){
           item(finalvis);
        });
        
    }
    
    // Standards:
    if (hidden in document)
        document.addEventListener("visibilitychange", onchange);
    else if ((hidden = "mozHidden") in document)
        document.addEventListener("mozvisibilitychange", onchange);
    else if ((hidden = "webkitHidden") in document)
        document.addEventListener("webkitvisibilitychange", onchange);
    else if ((hidden = "msHidden") in document)
        document.addEventListener("msvisibilitychange", onchange);
    // IE 9 and lower:
    else if ('onfocusin' in document)
        document.onfocusin = document.onfocusout = onchange;
    // All others:
    else
        window.onpageshow = window.onpagehide 
            = window.onfocus = window.onblur = onchange;
    
})();
