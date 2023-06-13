var jormrpc;

var rollcalled = false;

var state = "rollcall";

var setstate = function(thestate){
   if(thestate === state)
      return;
      
   if(state === "freequiz"){
      document.activeElement.blur();
      $("#freequizinput").blur();
   }
   
   state = thestate;
   if(state === "rollcall"){
      karbase.activate("rollcall");
      
      $("#statusbar2").html("not roll called yet");
      
   } else if (state === "choicequiz"){
      karbase.activate("choicequiz");
   } else if (state == "freequiz"){
      karbase.activate("textquiz");
   } else if (state == "waiting"){
      karbase.activate("waiting");
   } else {
      console.log("Unknown state : " + state);
   }
}

var quizscore = 0;

var setscore = function(thescore, aslogin){

   if(!aslogin && (thescore > quizscore)){
      var thediff = thescore - quizscore;
      humane.log("You gained +" + thediff + " points");
   }

   $("#botstatusbar1").html("your score : <b> " + thescore + "</b>");
   quizscore = thescore;
}

var onGSVarChange = function(varname, varval){
   
   console.log("Gotshere : " + varname);
   
   if(varname === "questiontext"){
      if(varval === ""){
         $(".quizheader").text("Enter your response below");
      } else {
         $(".quizheader").text(varval);
      }
   } else if (varname == "choicecount"){
      window.quizbase.openMultiChoice(varval);
   } else if (varname.substring(0, 10) === "choicetext"){
      var theindex = parseInt(varname.substring(10));
      window.quizbase.markDesc(theindex, varval);
   }
};

var initiatejormrpc = function(){
    jormrpc = new JormRPC("ws://10.1.1.5:3232", {
    
        local:{
            message: function(title, note){
                humane.log([title, note]);
            },
            studentname : function(thename){
               $("#studentname").val(thename);
            },
            rollcollected: function(thename){
               $("#statusbar2").html("enrolled as <i>" + escapeHtml(thename )+ "</i>");
               // karbase.activate("choicequiz");
               rollcalled = true;
               
               document.activeElement.blur();
               $("#studentname").blur();
               
            },
            curfreeformanswer : function(theanswer){
               $("#freequizinput").val(theanswer);
            },
            curchoiceanswer : function(theindex){
               window.quizbase.syncclicked(theindex);
            },
            statechange : function(thestate){
               setstate(thestate);
            },
            scoreupdate : function(newscore, aslogin){
               setscore(newscore, aslogin);
            },
            gsvar : function(varname, varval){
               onGSVarChange(varname, varval);
            }
        },
        onopen: function(){
        },
        onbecomeconnected: function(){
          $("#statusbar").html("connected");
        },
        onbecomedisconnected: function(){
          $("#statusbar").html("not connected");
        }
    
    });
};


var textupdater = undefined;
var freechoiceupdater = undefined;

$(document).ready(function(){

   initiatejormrpc();

   karbase.activate("rollcall");
   
   $("#studentname").keyup(function(){
      
      // let's message it after all
      // let's see
      if(textupdater)
         canceldelay(textupdater);
      
      textupdater = delayfor(300,
         function(){
            $("#rollcallheader").addClass('loading');
            jormrpc.invoke("studentname", $("#studentname").val(), function(){
               $("#rollcallheader").removeClass('loading');
            });
         }
      );
      
   });
   
   $("#freequizinput").keyup(function(){
   
      if(freechoiceupdater)
         canceldelay(freechoiceupdater);
      
      freechoiceupdater = delayfor(300,function(){
         $("#textquizheader").addClass('loading');
         jormrpc.invoke("freequizinput", $("#freequizinput").val(), function(result, oldanswer){
            if(result === "okay"){
               // cool
            } else {
               humane.log("too late! answer set as the old answer : " + oldanswer);
               $("#freequizinput").val(oldanswer);
            }
            $("#textquizheader").removeClass('loading');
         });
      });
      
   });
   
   
});
