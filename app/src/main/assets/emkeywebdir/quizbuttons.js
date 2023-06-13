function QuizBase(){
   
   var _buttons = [];
   
   var _buttonslocked = false;
   
   this.reg = function(thething){
      _buttons.push(thething);
      
      var thebutton = thething;
      thebutton.buttonnumber = _buttons.length - 1;
      
      $(thething).click(function(){
         // well, it got called
         
         window.jormrpc.invoke("quizclick", thebutton.buttonnumber, function(result){
            if(result === "okay"){
               window.quizbase.gotclicked(thebutton, true);
            } else {
               humane.log("too late!");
            }
         });
         
         window.quizbase.gotclicked(thebutton, false);
         
      });
      
   };
   
   this.gotclicked = function(theclicked, forreal){
      
      if(_buttonslocked)
         return;
      
      _buttons.forEach(function(thebutton){
         if(theclicked == thebutton){
            $(thebutton).addClass("quizbuttonchosen");
            if(!forreal){
               $(thebutton).addClass('loading');
            } else {
               $(thebutton).removeClass('loading');
            }
         } else {
            $(thebutton).removeClass("quizbuttonchosen loading");
         }
      });
      
   };
   
   this.syncclicked = function(theindex){
      _buttons.forEach(function(thebutton){
         
         if(thebutton.buttonnumber === theindex){
            $(thebutton).addClass("quizbuttonchosen");
         } else {
            $(thebutton).removeClass("quizbuttonchosen");
         }
         
         $(thebutton).removeClass("loading");
         
      });
   };
   
   
   this.syncTexts = function(){
      var i;
      for(i = 0; i < _buttons.length; i++){
         var thebutton = _buttons[i];
         
         var letext = String.fromCharCode(65 + i);
         if('choicedesc' in thebutton && thebutton.choicedesc !== ""){
            letext = letext + " : " + thebutton.choicedesc;
         }
         
         $(thebutton).text(letext);
         
      }
   }
   
   this.markDesc = function(theindex, thetext){
      if(theindex < _buttons.length){
         var thebutton = _buttons[theindex];
         thebutton.choicedesc = thetext;
         this.syncTexts();
      }
   }

   
   this.openMultiChoice = function(choiceamount){
      
      _buttonslocked = false;
      this.syncTexts();
      
      var i;
      for(i = 0; i < _buttons.length; i++){
         var thebutton = _buttons[i];
         
         if(i >= choiceamount){
            $(thebutton).hide();
         } else {
            $(thebutton).show();
         }
         
         // prepare the answers
         
      }
      
      
   };
   
};

   
$(document).ready(function(){

   window.quizbase = new QuizBase();
   
   var allbuttons = $("button[quizbutton]").get();
   allbuttons.forEach(function(thebutton){
      window.quizbase.reg(thebutton);
   });
   
   quizbase.openMultiChoice(5);
   
});
