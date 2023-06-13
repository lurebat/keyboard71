function KarletBase(){
   
   // requires jquery, I suppose
   // and assumes document is loaded. yep.
   
   var _divs = {};
   var _activediv = null;
   var _activedivname = "";
   
   this.onactivate = null;
   this.ondeactivate = null;
   // I guess you can set these
   
   var _kb = this;
   
   this.reg = function(divname){
      var thearray = $("#" + divname).get();
      
      if(thearray.length != 1){
         console.log("no such thing as div : " + divname);
      } else {
      
         var theobject = $(thearray[0]);
         _divs[divname] = theobject;
         
         theobject.hide();
      }
   };
   
   
   this.activate = function(divname){
      
      if(divname in _divs){
      
         var toactivate = _divs[divname];
         
         if(toactivate != _activediv){

            if(_activediv){
               
               if(this.ondeactivate){
                  this.ondeactivate(_activedivname);
               }
               
               _activediv.hide();
            }
            
            _activediv = _divs[divname];
            _activedivname = divname;
            
            if(this.onactivate)
               this.onactivate(divname);
            
            _activediv.show();
            
         }
         
         
      } else {
         console.log("cannot activate karlet : no such div : " + divname);
      }
      
   };
   
   
};
   
$(document).ready(function(){
   
   window.karbase = new KarletBase();
   
   var allkarlets = $("div[karlet]").get();
   allkarlets.forEach(function(karelt){
      
      var theid = $(karelt).attr('id');
      if(typeof(theid) == "string"){
         // cool
         window.karbase.reg(theid);
      } else {
         console.log("Found a karlet div with no id!");
         console.log("offending element : " + karelt.innerHTML);
      }
      
   });
   
});
